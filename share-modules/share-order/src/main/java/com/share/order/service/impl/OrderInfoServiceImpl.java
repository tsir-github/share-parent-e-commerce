package com.share.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.HttpStatus;
import com.share.common.core.constant.MqConstants;
import com.share.common.core.constant.OrderStatus;
import com.share.common.core.constant.PaymentStatus;
import com.share.common.core.constant.ProductSkuStatus;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.uuid.IdUtils;
import com.share.common.core.utils.uuid.Seq;
import com.share.common.security.utils.SecurityUtils;
import com.share.coupon.api.RemoteCouponService;
import com.share.goods.api.RemoteGoodsService;
import com.share.goods.api.RemoteSeckillService;
import com.share.goods.domain.ProductSku;
import com.share.order.domain.OrderBill;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.OrderItem;
import com.share.order.domain.OrderLog;
import com.share.order.domain.dto.CreateOrderDTO;
import com.share.order.domain.vo.EndOrderVo;
import com.share.order.enums.OrderOperateType;
import com.share.order.mapper.OrderBillMapper;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.mapper.OrderItemMapper;
import com.share.order.mapper.OrderLogMapper;
import com.share.order.service.IOrderInfoService;
import com.share.order.service.impl.OrderStatusServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单Service业务层处理
 *
 * 所有订单状态变更都通过 OrderStatusServiceImpl 完成，自动记录 OrderLog 操作流水。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService {

    private final OrderBillMapper orderBillMapper;
    private final OrderLogMapper orderLogMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusServiceImpl orderStatusService;
    private final RemoteGoodsService remoteGoodsService;
    private final RemoteSeckillService remoteSeckillService;
    private final RemoteCouponService remoteCouponService;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    /** 订单支付超时时间（分钟），从 Nacos 读取，默认 30 分钟 */
    @Value("${order.pay.timeout-minutes:30}")
    private Integer payTimeoutMinutes;

    @Override
    public OrderInfo getByOrderNo(String orderNo) {
        return baseMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processPaySuccess(String orderNo, String transactionId) {
        OrderInfo order = baseMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            log.error("支付成功回调订单不存在: {}", orderNo);
            return;
        }

        // 快速路径：状态已变更 → 幂等跳过
        if (OrderStatus.PENDING_DELIVERY.equals(order.getStatus())) {
            log.info("支付已处理（状态幂等跳过）: orderNo={}", orderNo);
            return;
        }

        // 抢锁式幂等：SETNX 原子抢锁，避免并发重复处理
        String dedupKey = "pay:dedup:" + orderNo;
        boolean first = redissonClient.getBucket(dedupKey).trySet("1", 7, TimeUnit.DAYS);
        if (!first) {
            log.info("支付已处理（幂等跳过）: orderNo={}", orderNo);
            return;
        }

        try {
            // 使用状态引擎：待支付(0) → 待发货(1)
            orderStatusService.transition(
                    order.getId(), order.getOrderNo(),
                    OrderOperateType.PAY.getCode(),
                    OrderStatus.PENDING_DELIVERY,
                    "系统(支付回调)", "支付成功 transactionId=" + transactionId
            );
            // 仅更新支付相关字段，防止覆盖 transition() 已更新的 status/version
            baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, order.getId())
                    .set(OrderInfo::getPayStatus, OrderStatus.PAY_PAID)
                    .set(OrderInfo::getPayTime, new Date())
                    .set(OrderInfo::getTransactionId, transactionId)
            );

            // 支付成功 → 扣减库存（goods 侧也做 Redis SETNX 幂等）
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            if (!items.isEmpty()) {
                List<RemoteGoodsService.StockDeductDTO> stockItems = new ArrayList<>();
                for (OrderItem item : items) {
                    RemoteGoodsService.StockDeductDTO dto = new RemoteGoodsService.StockDeductDTO();
                    dto.setSkuId(item.getSkuId());
                    dto.setQuantity(item.getQuantity());
                    dto.setOrderNo(orderNo);
                    stockItems.add(dto);
                }
                R<Boolean> deductResult = remoteGoodsService.deductStock(stockItems, SecurityConstants.INNER);
                if (deductResult.getCode() != HttpStatus.SUCCESS || !Boolean.TRUE.equals(deductResult.getData())) {
                    log.error("支付成功扣库存失败: orderNo={}, msg={}", orderNo, deductResult.getMsg());
                    throw new ServiceException("扣减库存失败，MQ 将重试: " + deductResult.getMsg());
                }
            }

            log.info("订单支付成功处理: orderNo={}", orderNo);
        } catch (Exception e) {
            // 任何一步失败 → 清理幂等标记，允许 MQ 重试
            // goods 侧已有 Redis SETNX 防重复扣，重试不会超卖
            redissonClient.getBucket(dedupKey).delete();
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, String closeType, String reason) {
        OrderInfo order = baseMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new ServiceException("订单不存在: " + orderNo);
        }
        orderStatusService.transition(
                order.getId(), order.getOrderNo(),
                OrderOperateType.CANCEL.getCode(),
                OrderStatus.CANCELLED,
                SecurityUtils.getUsername(), reason
        );
        // 仅更新取消相关字段，防止覆盖 transition() 已更新的 status/version
        baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, order.getId())
                .set(OrderInfo::getCloseType, closeType)
                .set(OrderInfo::getCloseReason, reason)
                .set(OrderInfo::getCloseTime, new Date())
        );

        // 库存回滚：仅已支付订单需要归还库存（未支付订单下单时未扣库存）
        if (OrderStatus.PAY_PAID.equals(order.getPayStatus())) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            if (!items.isEmpty()) {
                List<RemoteGoodsService.StockDeductDTO> stockItems = new ArrayList<>();
                for (OrderItem item : items) {
                    RemoteGoodsService.StockDeductDTO dto = new RemoteGoodsService.StockDeductDTO();
                    dto.setSkuId(item.getSkuId());
                    dto.setQuantity(item.getQuantity());
                    dto.setOrderNo(orderNo);
                    stockItems.add(dto);
                }
                R<Boolean> result = remoteGoodsService.releaseStock(stockItems, SecurityConstants.INNER);
                if (result.getCode() != HttpStatus.SUCCESS) {
                    log.error("库存回滚失败: orderNo={}, msg={}", orderNo, result.getMsg());
                }
            }
        }

        // 优惠券释放：如果订单使用了优惠券，调用优惠券服务回退
        if (order.getCouponIds() != null && !order.getCouponIds().isEmpty()) {
            Map<String, Object> couponParams = new HashMap<>();
            couponParams.put("orderNo", orderNo);
            R<Void> couponResult = remoteCouponService.releaseByOrderNo(couponParams, SecurityConstants.INNER);
            if (couponResult.getCode() != HttpStatus.SUCCESS) {
                log.warn("优惠券释放失败: orderNo={}, msg={}", orderNo, couponResult.getMsg());
            }
        }

        log.info("订单已取消: orderNo={}, type={}", orderNo, closeType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRefundSuccess(String orderNo, String transactionId, BigDecimal refundAmount) {
        OrderInfo order = baseMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            log.error("退款成功回调订单不存在: {}", orderNo);
            return;
        }

        // 幂等：同一笔退款单号只处理一次
        String dedupKey = "refund:dedup:" + orderNo + ":" + transactionId;
        boolean first = redissonClient.getBucket(dedupKey).trySet("1", 7, TimeUnit.DAYS);
        if (!first) {
            log.info("退款已处理（幂等跳过）: orderNo={}, refundTransactionId={}", orderNo, transactionId);
            return;
        }

        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
        BigDecimal cumulativeRefund = (order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO).add(refundAmount);

        if (cumulativeRefund.compareTo(payAmount) >= 0) {
            // 全额退款（含超额部分）：走状态机变更主状态
            if (OrderStatus.PAY_REFUNDED.equals(order.getPayStatus())) {
                log.info("订单已全额退款（幂等跳过）: orderNo={}", orderNo);
                return;
            }
            orderStatusService.transition(
                    order.getId(), order.getOrderNo(),
                    OrderOperateType.REFUND.getCode(),
                    OrderStatus.CANCELLED,
                    "系统(退款回调)", "全额退款成功 transactionId=" + transactionId
            );
            baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, order.getId())
                    .set(OrderInfo::getPayStatus, OrderStatus.PAY_REFUNDED)
                    .setSql("refund_amount = COALESCE(refund_amount,0) + " + refundAmount)
                    .set(OrderInfo::getRefundTime, new Date())
                    .setSql("refund_count = COALESCE(refund_count,0) + 1")
            );
            log.info("订单全额退款处理: orderNo={}, refundAmount={}", orderNo, refundAmount);
        } else {
            // 部分退款：仅累加退款金额，不改变主状态和支付状态
            baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, order.getId())
                    .setSql("refund_amount = COALESCE(refund_amount,0) + " + refundAmount)
                    .set(OrderInfo::getRefundTime, new Date())
                    .setSql("refund_count = COALESCE(refund_count,0) + 1")
            );
            log.info("订单部分退款处理: orderNo={}, refundAmount={}", orderNo, refundAmount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(String orderNo, Long deliveryBy, String deliveryName, String deliveryPhone) {
        OrderInfo order = baseMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new ServiceException("订单不存在: " + orderNo);
        }
        orderStatusService.transition(
                order.getId(), order.getOrderNo(),
                OrderOperateType.DELIVER.getCode(),
                OrderStatus.DELIVERING,
                SecurityUtils.getUsername(), "分配配送员: " + deliveryName
        );
        // 仅更新配送相关字段
        baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, order.getId())
                .set(OrderInfo::getDeliveryBy, deliveryBy)
                .set(OrderInfo::getDeliveryName, deliveryName)
                .set(OrderInfo::getDeliveryPhone, deliveryPhone)
                .set(OrderInfo::getDeliveryTime, new Date())
        );
        log.info("订单发货: orderNo={}, deliveryBy={}", orderNo, deliveryBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo) {
        OrderInfo order = baseMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new ServiceException("订单不存在: " + orderNo);
        }
        orderStatusService.transition(
                order.getId(), order.getOrderNo(),
                OrderOperateType.CONFIRM.getCode(),
                OrderStatus.COMPLETED,
                SecurityUtils.getUsername(), "用户确认收货"
        );
        // 仅更新收货时间
        baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, order.getId())
                .set(OrderInfo::getReceiveTime, new Date())
        );
        log.info("订单确认收货: orderNo={}", orderNo);
    }

    // ==================== 创建订单 ====================

    @Override
    @SentinelResource(value = "createOrder", fallback = "createOrderFallback")
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(CreateOrderDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }

        // 用户级别分布式锁：防止同一用户重复提交订单
        RLock lock = redissonClient.getLock("order:create:" + userId);
        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                throw new ServiceException("下单操作太频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("下单被中断，请重试");
        }

        try {
            // 秒杀订单标识（用于后续分支）
            boolean isSeckill = dto.getSeckillActivityId() != null;

            // 秒杀 + 优惠券互斥
            if (isSeckill && dto.getCouponId() != null) {
                throw new ServiceException("秒杀商品不支持使用优惠券");
            }

            // 1. 收集 SKU 信息并校验
            List<CreateOrderDTO.OrderItemDTO> items = dto.getItems();
            List<ProductSku> skuList = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CreateOrderDTO.OrderItemDTO item : items) {
                R<ProductSku> skuResult = remoteGoodsService.getSkuById(item.getSkuId(), SecurityConstants.INNER);
                if (skuResult.getCode() != HttpStatus.SUCCESS
                        || skuResult.getData() == null) {
                    throw new ServiceException("商品SKU不存在: id=" + item.getSkuId());
                }
                ProductSku sku = skuResult.getData();
                // 校验 SKU 状态
                if (!ProductSkuStatus.ENABLED.equals(sku.getStatus())) {
                    throw new ServiceException("商品已下架: skuId=" + item.getSkuId());
                }
                // 秒杀订单的库存已在 share-goods 预扣，此处跳过校验
                if (!isSeckill) {
                    if (sku.getStock() < item.getQuantity()) {
                        throw new ServiceException("商品库存不足: skuId=" + item.getSkuId()
                                + ", 当前库存=" + sku.getStock() + ", 需求=" + item.getQuantity());
                    }
                }
                skuList.add(sku);
                BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(subtotal);
            }

            // 2. 生成订单号（秒杀订单复用预生成的 orderNo，MQ 重投时 DB 唯一约束防重复）
            String orderNo = dto.getOrderNo() != null ? dto.getOrderNo() : Seq.nextOrderNo();

            // 3. 处理优惠券（查询 + 校验 + 锁定）
            BigDecimal discountAmount = BigDecimal.ZERO;
            Long lockedCouponUserId = null;

            if (dto.getCouponId() != null) {
                // 查询券详情（含模板折扣信息）
                Map<String, Object> couponParams = new HashMap<>();
                couponParams.put("couponUserId", dto.getCouponId());
                R<Map<String, Object>> couponDetailR = remoteCouponService.getCouponDetail(couponParams, SecurityConstants.INNER);
                if (couponDetailR.getCode() != HttpStatus.SUCCESS || couponDetailR.getData() == null) {
                    throw new ServiceException("优惠券不存在");
                }
                Map<String, Object> cd = couponDetailR.getData();

                // 校验有效期
                Date now = new Date();
                Date startTime = (Date) cd.get("startTime");
                Date endTime = (Date) cd.get("endTime");
                if (now.before(startTime) || now.after(endTime)) {
                    throw new ServiceException("优惠券不在有效期内");
                }

                // 计算折扣
                String type = (String) cd.get("type");
                if ("0".equals(type) || "2".equals(type)) {
                    // 满减券 / 无门槛券
                    BigDecimal conditionAmt = new BigDecimal(cd.get("conditionAmt").toString());
                    if (conditionAmt.compareTo(BigDecimal.ZERO) > 0 && totalAmount.compareTo(conditionAmt) < 0) {
                        throw new ServiceException("未达到优惠券使用门槛");
                    }
                    discountAmount = new BigDecimal(cd.get("discountAmt").toString());
                } else {
                    // 折扣券 (type=1)
                    BigDecimal rate = new BigDecimal(cd.get("discountRate").toString());
                    discountAmount = totalAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                if (discountAmount.compareTo(totalAmount) > 0) {
                    discountAmount = totalAmount; // 兜底：优惠不超订单总额
                }

                // 锁定优惠券（status 0→1）
                Map<String, Object> lockParams = new HashMap<>();
                lockParams.put("userId", userId);
                lockParams.put("couponUserId", dto.getCouponId());
                lockParams.put("orderNo", orderNo);
                R<Void> lockResult = remoteCouponService.lockForOrder(lockParams, SecurityConstants.INNER);
                if (lockResult.getCode() != HttpStatus.SUCCESS) {
                    throw new ServiceException("优惠券锁定失败：" + lockResult.getMsg());
                }
                lockedCouponUserId = dto.getCouponId();
            }

            BigDecimal freightAmount = BigDecimal.ZERO;     // 运费，暂免
            // 秒杀订单使用传递的秒杀价，不走商品原价计算
            BigDecimal payAmount = isSeckill ? dto.getSeckillPrice().multiply(
                    BigDecimal.valueOf(dto.getItems().stream().mapToInt(CreateOrderDTO.OrderItemDTO::getQuantity).sum()))
                    : totalAmount.subtract(discountAmount).add(freightAmount);

            // 4. 构建订单主体
            OrderInfo order = new OrderInfo();
            order.setUserId(userId);
            order.setOrderNo(orderNo);
            order.setStartTime(new Date());
            order.setTotalAmount(totalAmount);
            order.setDiscountAmount(discountAmount);
            order.setFreightAmount(freightAmount);
            order.setPayAmount(payAmount);
            order.setPayStatus(OrderStatus.PAY_UNPAID);
            order.setOrderType(isSeckill ? OrderStatus.ORDER_TYPE_FLASH : OrderStatus.ORDER_TYPE_NORMAL);
            order.setStatus(OrderStatus.PENDING_PAY);
            order.setDeliveryStatus(OrderStatus.DELIVERY_UNDELIVERED);
            order.setReceiverName(dto.getReceiverName());
            order.setReceiverPhone(dto.getReceiverPhone());
            order.setReceiverAddress(dto.getReceiverAddress());
            order.setVersion(0);
            order.setCreateTime(new Date());
            order.setCreateBy(String.valueOf(userId));
            order.setRemark(dto.getRemark());

            // 优惠券关联（如有）
            if (lockedCouponUserId != null) {
                order.setCouponIds(String.valueOf(lockedCouponUserId));
            }

            // 金额明细 JSON
            Map<String, Object> amountDetail = new LinkedHashMap<>();
            amountDetail.put("totalAmount", totalAmount);
            amountDetail.put("discountAmount", discountAmount);
            amountDetail.put("freightAmount", freightAmount);
            amountDetail.put("payAmount", payAmount);
            order.setAmountDetail(new com.alibaba.fastjson2.JSONObject(amountDetail).toString());

            // 秒杀订单幂等：如 orderNo 已存在（MQ 重投场景），直接返回（防 Consumer 误判失败多还库存）
            if (isSeckill && dto.getOrderNo() != null) {
                OrderInfo existing = baseMapper.selectOne(
                        new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, dto.getOrderNo()));
                if (existing != null) {
                    log.info("秒杀订单已存在（幂等跳过）: orderNo={}", dto.getOrderNo());
                    return dto.getOrderNo();
                }
            }

            baseMapper.insert(order);
            Long orderId = order.getId();

            // 5. 构建订单明细
            for (int i = 0; i < items.size(); i++) {
                CreateOrderDTO.OrderItemDTO dtoItem = items.get(i);
                ProductSku sku = skuList.get(i);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(orderId);
                orderItem.setProductId(sku.getProductId());
                orderItem.setSkuId(sku.getId());
                orderItem.setMerchantId(sku.getMerchantId()); // 记录商品所属商家
                orderItem.setProductName(dtoItem.getProductName());
                orderItem.setSkuSpecs(toJsonSafe(dtoItem.getSkuSpecs()));
                orderItem.setProductImage(sku.getImage());
                orderItem.setPrice(sku.getPrice());
                orderItem.setQuantity(dtoItem.getQuantity());
                orderItem.setTotalAmount(sku.getPrice().multiply(BigDecimal.valueOf(dtoItem.getQuantity())));
                orderItem.setCreateTime(new Date());
                orderItemMapper.insert(orderItem);
            }

            // 6. 记录下单流水
            OrderLog logEntry = new OrderLog();
            logEntry.setOrderId(orderId);
            logEntry.setOrderNo(orderNo);
            logEntry.setOperateType(OrderOperateType.CREATE.getCode());
            logEntry.setBeforeStatus(null);
            logEntry.setAfterStatus(OrderStatus.PENDING_PAY);
            logEntry.setOperateUser(String.valueOf(userId));
            logEntry.setNote("用户下单");
            orderLogMapper.insert(logEntry);

            // 下单不扣库存——库存将在支付成功时扣减，超时取消无需回滚
            // ponytail: 防止恶意占库，把扣库存时机从下单推迟到支付

            // 发送 RocketMQ 延迟消息 — 超时未支付自动取消
            int delayLevel = computeDelayLevel(payTimeoutMinutes);
            rocketMQTemplate.syncSend(
                    MqConstants.ORDER_TIMEOUT_CANCEL_TOPIC,
                    MessageBuilder.withPayload(orderNo).build(),
                    3000,
                    delayLevel
            );

            // 发送商家新订单通知
            try {
                rocketMQTemplate.convertAndSend(MqConstants.ORDER_MERCHANT_NOTIFY_TOPIC, orderNo);
            } catch (Exception e) {
                log.warn("商家通知发送失败: orderNo={}", orderNo, e);
            }

            log.info("订单创建成功: orderNo={}, userId={}, payAmount={}", orderNo, userId, payAmount);
            return orderNo;
        } finally {
            // 确保锁释放（Redisson 看门狗自动续期，此处只处理本线程持有情况）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 将配置的分钟数映射为 RcoketMQ MessageDelayLevel
     *
     * RocketMQ 延迟等级（18级）：
     *   1s/5s/10s/30s/1m/2m/3m/4m/5m/6m/7m/8m/9m/10m/20m/30m/1h/2h
     *
     * @param minutes 配置的分钟数
     * @return 对应的延迟等级
     */
    private int computeDelayLevel(int minutes) {
        if (minutes <= 1) return 5;   // 1m
        if (minutes <= 2) return 6;   // 2m
        if (minutes <= 3) return 7;   // 3m
        if (minutes <= 4) return 8;   // 4m
        if (minutes <= 5) return 9;   // 5m
        if (minutes <= 6) return 10;  // 6m
        if (minutes <= 7) return 11;  // 7m
        if (minutes <= 8) return 12;  // 8m
        if (minutes <= 9) return 13;  // 9m
        if (minutes <= 10) return 14; // 10m
        if (minutes <= 20) return 15; // 20m
        if (minutes <= 30) return 16; // 30m (默认)
        if (minutes <= 60) return 17; // 1h
        return 18;                    // 2h
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endOrder(EndOrderVo endOrderVo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo, endOrderVo.getOrderNo());
        wrapper.eq(OrderInfo::getStatus, OrderStatus.PENDING_PAY);
        OrderInfo orderInfo = baseMapper.selectOne(wrapper);
        if (orderInfo == null) {
            log.warn("超时取消订单不存在或状态不符: orderNo={}", endOrderVo.getOrderNo());
            return;
        }
        // 使用状态引擎取消（transition 内部已处理 version 乐观锁）
        orderStatusService.transition(
                orderInfo.getId(), orderInfo.getOrderNo(),
                OrderOperateType.CANCEL.getCode(),
                OrderStatus.CANCELLED,
                "系统", endOrderVo.getEndTime() != null ? "超时取消" : "手动取消"
        );

        // 秒杀订单超时取消 → 归还 Redis 秒杀库存
        if (OrderStatus.ORDER_TYPE_FLASH.equals(orderInfo.getOrderType())) {
            try {
                remoteSeckillService.releaseStockByOrderNo(endOrderVo.getOrderNo(), SecurityConstants.INNER);
                log.info("秒杀订单超时取消，已归还库存: orderNo={}", endOrderVo.getOrderNo());
            } catch (Exception e) {
                log.error("秒杀订单超时取消归还库存失败: orderNo={}", endOrderVo.getOrderNo(), e);
            }
        }
    }

    /**
     * createOrder 的 Sentinel 降级方法（仅限流/熔断时触发）
     */
    public String createOrderFallback(CreateOrderDTO dto, Throwable t) {
        log.error("下单被限流降级: {}", t.getMessage(), t);
        throw new ServiceException("下单服务繁忙，请稍后再试");
    }

    @Override
    public List<OrderInfo> selectOrderListByUserId(Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        wrapper.orderByDesc(OrderInfo::getId);
        List<OrderInfo> list = baseMapper.selectList(wrapper);
        // 批量填充订单明细
        if (!list.isEmpty()) {
            List<Long> orderIds = list.stream().map(OrderInfo::getId).collect(Collectors.toList());
            List<OrderItem> allItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
            Map<Long, List<OrderItem>> itemMap = allItems.stream()
                    .collect(Collectors.groupingBy(OrderItem::getOrderId));
            for (OrderInfo o : list) {
                o.getParams().put("_items", itemMap.getOrDefault(o.getId(), Collections.emptyList()));
            }
        }
        return list;
    }

    @Override
    public OrderInfo selectNoFinishOrder(Long userId) {
        List<OrderInfo> list = selectOrderListByUserId(userId);
        // 取最近一条未完成订单：status 非 已完成/已取消/售后中
        return list.stream()
                .filter(o -> !List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.AFTER_SALE).contains(o.getStatus()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public OrderInfo selectOrderInfoById(Long id) {
        OrderInfo orderInfo = baseMapper.selectById(id);
        if (orderInfo != null) {
            List<OrderBill> orderBillList = orderBillMapper
                    .selectList(new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, id));
            orderInfo.setOrderBillList(orderBillList);
            List<OrderItem> items = orderItemMapper
                    .selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
            orderInfo.getParams().put("_items", items);
        }
        return orderInfo;
    }

    @Override
    public Map<String, Object> getOrderCount(String sql) {
        // ponytail: 旧接口走安全查询，忽略传入的 SQL 参数
        List<Map<String, Object>> list = baseMapper.getOrderCountByDate(null, null);
        Map<String, Object> dataMap = new HashMap<>();
        List<Object> dateList = new ArrayList<>();
        List<Object> countList = new ArrayList<>();
        for (Map<String, Object> map : list) {
            dateList.add(map.get("order_date"));
            countList.add(map.get("order_count"));
        }
        dataMap.put("dateList", dateList);
        dataMap.put("countList", countList);
        return dataMap;
    }

    @Override
    public List<OrderInfo> selectOrderListForAdmin(OrderInfo orderInfo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (orderInfo != null) {
            if (com.share.common.core.utils.StringUtils.isNotBlank(orderInfo.getOrderNo())) {
                wrapper.like(OrderInfo::getOrderNo, orderInfo.getOrderNo());
            }
            if (com.share.common.core.utils.StringUtils.isNotBlank(orderInfo.getStatus())) {
                wrapper.eq(OrderInfo::getStatus, orderInfo.getStatus());
            }
            if (com.share.common.core.utils.StringUtils.isNotBlank(orderInfo.getPayStatus())) {
                wrapper.eq(OrderInfo::getPayStatus, orderInfo.getPayStatus());
            }
        }
        wrapper.orderByDesc(OrderInfo::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 订单总数
        stats.put("totalOrders", baseMapper.selectCount(null));

        // 今日订单数
        LambdaQueryWrapper<OrderInfo> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.apply("DATE(create_time) = CURDATE()");
        stats.put("todayOrders", baseMapper.selectCount(todayWrapper));

        // 待支付订单数（status=0）
        LambdaQueryWrapper<OrderInfo> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(OrderInfo::getStatus, OrderStatus.PENDING_PAY);
        stats.put("pendingOrders", baseMapper.selectCount(pendingWrapper));

        // 已支付总金额（payStatus=1，SUM pay_amount）
        QueryWrapper<OrderInfo> paidWrapper = new QueryWrapper<>();
        paidWrapper.eq("pay_status", OrderStatus.PAY_PAID);
        paidWrapper.select("COALESCE(SUM(pay_amount), 0) AS totalRevenue");
        List<Map<String, Object>> paidResult = baseMapper.selectMaps(paidWrapper);
        stats.put("totalRevenue", paidResult.get(0).get("totalRevenue"));

        // 今日收入
        QueryWrapper<OrderInfo> todayPaidWrapper = new QueryWrapper<>();
        todayPaidWrapper.eq("pay_status", OrderStatus.PAY_PAID);
        todayPaidWrapper.apply("DATE(create_time) = CURDATE()");
        todayPaidWrapper.select("COALESCE(SUM(pay_amount), 0) AS todayRevenue");
        List<Map<String, Object>> todayPaidResult = baseMapper.selectMaps(todayPaidWrapper);
        stats.put("todayRevenue", todayPaidResult.get(0).get("todayRevenue"));

        return stats;
    }

    @Override
    public long countTodayOrders(Long merchantId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getSupplierId, merchantId)
                .eq(OrderInfo::getStatus, OrderStatus.PENDING_DELIVERY)
                .apply("DATE(create_time) = CURDATE()"));
    }

    @Override
    public BigDecimal sumTodaySales(Long merchantId) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("supplier_id", merchantId)
                .eq("status", OrderStatus.PENDING_DELIVERY)
                .apply("DATE(create_time) = CURDATE()")
                .select("COALESCE(SUM(total_amount), 0) AS amt");
        List<Map<String, Object>> result = baseMapper.selectMaps(wrapper);
        return new BigDecimal(result.get(0).get("amt").toString());
    }

    @Override
    public long countPendingDelivery(Long merchantId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getSupplierId, merchantId)
                .eq(OrderInfo::getStatus, OrderStatus.PENDING_DELIVERY));
    }

    /**
     * 将 skuSpecs 转为 MySQL JSON 列可接受的值。已为 JSON 格式的字符串原样返回，
     * 否则用 fastjson2 序列化为 JSON 字符串字面量（如 "300g" → \"300g\"）。
     */
    private static String toJsonSafe(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return value; // 已是 JSON 结构，不做转换
        }
        // ponytail: 纯字符串 → JSON 字符串字面量，MySQL JSON 列可接受
        return com.alibaba.fastjson2.JSON.toJSONString(value);
    }

}

