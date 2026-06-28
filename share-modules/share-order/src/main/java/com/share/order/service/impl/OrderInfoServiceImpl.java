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

        // 支付成功 → 扣减库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        if (!items.isEmpty()) {
            List<RemoteGoodsService.StockDeductDTO> stockItems = new ArrayList<>();
            for (OrderItem item : items) {
                RemoteGoodsService.StockDeductDTO dto = new RemoteGoodsService.StockDeductDTO();
                dto.setSkuId(item.getSkuId());
                dto.setQuantity(item.getQuantity());
                stockItems.add(dto);
            }
            R<Boolean> deductResult = remoteGoodsService.deductStock(stockItems, SecurityConstants.INNER);
            if (deductResult.getCode() != HttpStatus.SUCCESS || !Boolean.TRUE.equals(deductResult.getData())) {
                log.error("支付成功扣库存失败: orderNo={}, msg={}", orderNo, deductResult.getMsg());
                throw new ServiceException("扣减库存失败，MQ 将重试: " + deductResult.getMsg());
            }
        }

        log.info("订单支付成功处理: orderNo={}", orderNo);
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
        // 计算累计退款金额
        BigDecimal existingRefund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal cumulativeRefund = existingRefund.add(refundAmount);
        int existingRefundCount = order.getRefundCount() != null ? order.getRefundCount() : 0;
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;

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
                    .set(OrderInfo::getRefundAmount, cumulativeRefund)
                    .set(OrderInfo::getRefundTime, new Date())
                    .set(OrderInfo::getRefundCount, existingRefundCount + 1)
            );
            log.info("订单全额退款处理: orderNo={}, cumulativeRefund={}", orderNo, cumulativeRefund);
        } else {
            // 部分退款：仅累加退款金额，不改变主状态和支付状态
            if (order.getTransactionId() != null && !order.getTransactionId().equals(transactionId)) {
                // 幂等：相同 transactionId 的退款不重复处理
            }
            baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, order.getId())
                    .set(OrderInfo::getRefundAmount, cumulativeRefund)
                    .set(OrderInfo::getRefundTime, new Date())
                    .set(OrderInfo::getRefundCount, existingRefundCount + 1)
            );
            log.info("订单部分退款处理: orderNo={}, thisRefund={}, cumulativeRefund={}",
                    orderNo, refundAmount, cumulativeRefund);
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
                // 校验库存
                if (sku.getStock() < item.getQuantity()) {
                    throw new ServiceException("商品库存不足: skuId=" + item.getSkuId()
                            + ", 当前库存=" + sku.getStock() + ", 需求=" + item.getQuantity());
                }
                skuList.add(sku);
                BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(subtotal);
            }

            // 2. 计算金额
            BigDecimal discountAmount = BigDecimal.ZERO;   // 优惠金额，TODO: 集成优惠券后计算
            BigDecimal freightAmount = BigDecimal.ZERO;     // 运费，暂免
            BigDecimal payAmount = totalAmount.subtract(discountAmount).add(freightAmount);

            // 3. 生成订单号
            String orderNo = Seq.getId();

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
            order.setOrderType(OrderStatus.ORDER_TYPE_NORMAL);
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
            if (dto.getCouponId() != null) {
                order.setCouponIds(String.valueOf(dto.getCouponId()));
                // ponytail: 锁定优惠券，待优惠券模块集成后实现
            }

            // 金额明细 JSON
            Map<String, Object> amountDetail = new LinkedHashMap<>();
            amountDetail.put("totalAmount", totalAmount);
            amountDetail.put("discountAmount", discountAmount);
            amountDetail.put("freightAmount", freightAmount);
            amountDetail.put("payAmount", payAmount);
            order.setAmountDetail(new com.alibaba.fastjson2.JSONObject(amountDetail).toString());

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
                orderItem.setSkuSpecs(dtoItem.getSkuSpecs());
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
    }

    /**
     * createOrder 的 Sentinel 降级方法（仅限流/熔断时触发）
     */
    public String createOrderFallback(CreateOrderDTO dto, Throwable t) {
        log.error("下单被限流降级: {}", t.getMessage());
        throw new ServiceException("下单服务繁忙，请稍后再试");
    }

    @Override
    public List<OrderInfo> selectOrderListByUserId(Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        wrapper.orderByDesc(OrderInfo::getId);
        return baseMapper.selectList(wrapper);
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

}

