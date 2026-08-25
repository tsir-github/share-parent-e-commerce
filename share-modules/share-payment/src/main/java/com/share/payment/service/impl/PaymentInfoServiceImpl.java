package com.share.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.uuid.IdUtils;
import com.share.payment.config.WxPayConfig;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.PaymentStatus;
import com.share.payment.domain.PaymentInfo;
import com.share.payment.mapper.PaymentInfoMapper;
import com.share.payment.mq.PaymentMQProducer;
import com.share.payment.service.IPaymentInfoService;
import com.share.payment.utils.WxPayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 付款信息Service实现
 *
 * 核心逻辑：
 * 1. createPayment — 创建支付记录（状态0），调微信统一下单或 mock，返回前端调起支付参数
 * 2. handlePayCallback — 微信异步回调：验签+解密 → 状态机校验（只能0→1）
 * 3. mockPaySuccess — 模拟支付成功（开发阶段替代微信回调）
 * 4. refund — 状态机校验（已支付才能退）
 *
 * 事务边界说明：
 * - Redis 分布式锁在 @Transactional 之前获取，防止锁内包含远程 RPC（微信 API）
 * - 支付状态更新（updateToPaid）和 MQ 发送之间不是强事务关系：
 *   支付状态先持久化，MQ 发送失败由补偿定时任务兜底（最终一致性）
 *
 * mock-mode 说明：
 * - WxPayConfig.mockMode=true 时不调微信 API，直接返回模拟参数
 * - 通过 POST /api/v1/payment/mock/callback 手动触发支付成功
 * - 上线前关闭 mock-mode，改为真实微信支付
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements IPaymentInfoService {

    private final WxPayUtil wxPayUtil;
    private final WxPayConfig wxPayConfig;
    private final PaymentMQProducer paymentMQProducer;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;



    @Override
    public Integer getPaymentStatusByOrderNo(String orderNo) {
        PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo));
        return paymentInfo != null ? paymentInfo.getPaymentStatus() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> createPayment(String orderNo, Long userId, BigDecimal amount,
                                             String description, String openid) {
        RLock lock = redissonClient.getLock(CacheConstants.PAYMENT_LOCK_KEY + orderNo);
        boolean locked = false;
        try {
            // 分布式锁在事务前获取，控制并发创建
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException("支付处理中，请稍后重试");
            }

            // 幂等检查：是否已有支付记录
            LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<PaymentInfo>()
                    .eq(PaymentInfo::getOrderNo, orderNo);
            PaymentInfo exist = this.getOne(queryWrapper);
            if (exist != null) {
                if (PaymentStatus.PAID.equals(exist.getPaymentStatus())) {
                    throw new ServiceException("该订单已支付");
                }
                // 未支付，复用已有记录
                return doCreatePayment(exist, openid);
            }

            // 创建支付记录
            PaymentInfo paymentInfo = PaymentInfo.builder()
                    .userId(userId)
                    .orderNo(orderNo)
                    .payWay(1) // 微信支付
                    .amount(amount)
                    .content(description)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .build();
            this.save(paymentInfo);

            return doCreatePayment(paymentInfo, openid);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("支付处理被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 调微信统一下单或 mock，返回前端调起支付参数
     */
    private Map<String, String> doCreatePayment(PaymentInfo paymentInfo, String openid) {
        if (wxPayConfig.isMockMode()) {
            return mockPayParams(paymentInfo);
        }
        String description = paymentInfo.getContent() != null ? paymentInfo.getContent() : "小区电商-商品购买";
        int totalFee = paymentInfo.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
        String prepayId = wxPayUtil.createOrder(
                paymentInfo.getOrderNo(), totalFee, description, openid);
        return wxPayUtil.buildPayParams(prepayId);
    }

    /**
     * mock 模式：生成模拟调起支付参数
     */
    private Map<String, String> mockPayParams(PaymentInfo paymentInfo) {
        String mockPrepayId = "mock_prepay_id_" + IdUtils.fastSimpleUUID();
        log.info("[MOCK] 模拟统一下单: orderNo={}, prepayId={}", paymentInfo.getOrderNo(), mockPrepayId);
        // ponytail: mock 模式返回硬编码参数，不调签名（避免加载商户私钥失败）
        return Map.of(
            "appId", wxPayConfig.getAppId(),
            "timeStamp", String.valueOf(System.currentTimeMillis() / 1000),
            "nonceStr", IdUtils.fastSimpleUUID(),
            "package", "prepay_id=" + mockPrepayId,
            "signType", "RSA",
            "paySign", "MOCK_SIGN"
        );
    }

    @Override
    @SentinelResource(value = "paymentCallback", fallback = "handlePayCallbackFallback")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> handlePayCallback(HttpServletRequest request) {
        try {
            if (wxPayConfig.isMockMode()) {
                log.warn("[MOCK] 收到微信回调请求，mock 模式忽略。请使用 /api/v1/payment/mock/callback 触发");
                return buildWechatSuccessResponse();
            }

            String serial = request.getHeader("Wechatpay-Serial");
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 验签 + 解密
            String plainText = wxPayUtil.verifyAndDecryptCallback(serial, signature, timestamp, nonce, body);

            // 使用 Jackson 解析回调数据
            JsonNode root = objectMapper.readTree(plainText);
            String orderNo = root.get("out_trade_no").asText();
            String transactionId = root.get("transaction_id").asText();
            String tradeState = root.has("trade_state") ? root.get("trade_state").asText() : "SUCCESS";

            if (!"SUCCESS".equals(tradeState)) {
                log.warn("微信回调 trade_state 非 SUCCESS: {}", tradeState);
                return buildWechatSuccessResponse();
            }

            updateToPaid(orderNo, transactionId, body);
            return buildWechatSuccessResponse();

        } catch (SecurityException e) {
            log.error("微信回调验签失败", e);
            return buildWechatFailResponse("签名验证失败");
        } catch (Exception e) {
            log.error("处理微信回调异常", e);
            return buildWechatFailResponse("处理异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPaySuccess(String orderNo) {
        if (!wxPayConfig.isMockMode()) {
            throw new ServiceException("非 mock 模式不可使用模拟回调");
        }

        RLock lock = redissonClient.getLock(CacheConstants.PAYMENT_LOCK_KEY + orderNo);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException("支付处理中，请稍后重试");
            }

            PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>()
                    .eq(PaymentInfo::getOrderNo, orderNo));
            if (paymentInfo == null) {
                // ponytail: mock 模式自动补建支付记录，避免要求前端先调 createPayment
                paymentInfo = PaymentInfo.builder()
                        .orderNo(orderNo)
                        .userId(0L)
                        .amount(java.math.BigDecimal.ZERO)
                        .content("mock 自动创建")
                        .paymentStatus(PaymentStatus.UNPAID)
                        .build();
                paymentInfo.setCreateTime(new Date());
                this.save(paymentInfo);
                log.info("[MOCK] 自动创建支付记录: orderNo={}", orderNo);
            }

            String mockTransactionId = "mock_txn_" + IdUtils.fastSimpleUUID().substring(0, 16);
            String mockBody = "{\"mock\":true,\"out_trade_no\":\"" + orderNo + "\"}";
            updateToPaid(orderNo, mockTransactionId, mockBody);
            log.info("[MOCK] 模拟支付成功: orderNo={}, transactionId={}", orderNo, mockTransactionId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("支付处理被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 支付成功公共处理：更新支付记录状态 0→1
     * 幂等：已支付（status=1）则跳过
     *
     * 事务边界：该方法在 @Transactional 内被调用。
     * MQ 发送失败捕获异常，不阻止事务提交 — 支付状态持久化优先级高于 MQ，
     * 补偿定时任务会处理未送达的 MQ 消息。
     */
    private void updateToPaid(String orderNo, String transactionId, String callbackBody) {
        PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo));
        if (paymentInfo == null) {
            log.error("支付记录不存在: {}", orderNo);
            return;
        }
        if (PaymentStatus.PAID.equals(paymentInfo.getPaymentStatus())) {
            log.info("支付已处理（幂等跳过）: orderNo={}", orderNo);
            return;
        }

        boolean updated = this.update(null, new LambdaUpdateWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo)
                .eq(PaymentInfo::getPaymentStatus, PaymentStatus.UNPAID)
                .set(PaymentInfo::getTransactionId, transactionId)
                .set(PaymentInfo::getPaymentStatus, PaymentStatus.PAID)
                .set(PaymentInfo::getCallbackTime, new Date())
                .set(PaymentInfo::getCallbackContent, callbackBody));

        if (!updated) {
            log.warn("支付状态更新失败（可能已被其他请求处理）: orderNo={}", orderNo);
            return;
        }

        // 发送支付成功 MQ 消息 — 异步通知订单模块更新状态
        // 失败不抛异常（不阻塞事务提交），补偿定时任务会兜底
        boolean sent = paymentMQProducer.sendPaySuccessMessage(orderNo, transactionId);
        if (!sent) {
            log.warn("支付成功 MQ 发送失败（已记录支付状态，补偿任务将处理）: orderNo={}", orderNo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, BigDecimal amount, String reason) {
        RLock lock = redissonClient.getLock(CacheConstants.PAYMENT_LOCK_KEY + orderNo);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException("退款处理中，请稍后重试");
            }

            PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>()
                    .eq(PaymentInfo::getOrderNo, orderNo));
            if (paymentInfo == null) {
                throw new ServiceException("支付记录不存在");
            }
            if (!PaymentStatus.PAID.equals(paymentInfo.getPaymentStatus())) {
                throw new ServiceException("该订单未支付，无法退款");
            }
            // 退款金额校验：不允许超过实付金额
            if (amount.compareTo(paymentInfo.getAmount()) > 0) {
                throw new ServiceException("退款金额不能超过实付金额");
            }
            if (reason == null || reason.isBlank()) {
                throw new ServiceException("退款原因不能为空");
            }

            String refundId = null;
            if (!wxPayConfig.isMockMode()) {
                // 调微信退款 API（异步，微信会回调 notify_url）
                int totalFee = paymentInfo.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
                String response = wxPayUtil.createRefund(orderNo, totalFee, reason);
                // 解析微信退款响应
                JsonNode root = objectMapper.readTree(response);
                String refundStatus = root.has("refund_status") ? root.get("refund_status").asText() : "";
                refundId = root.has("refund_id") ? root.get("refund_id").asText() : "";

                Integer targetStatus = "SUCCESS".equals(refundStatus) ? PaymentStatus.REFUNDED : PaymentStatus.REFUNDING;

                this.update(null, new LambdaUpdateWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
                        .eq(PaymentInfo::getPaymentStatus, PaymentStatus.PAID)
                        .set(PaymentInfo::getPaymentStatus, targetStatus)
                        .set(PaymentInfo::getTransactionId, refundId));
                log.info("微信退款 API 调用完成: orderNo={}, refundStatus={}, refundId={}", orderNo, refundStatus, refundId);

                // 如果微信即时返回退款成功，发 MQ 通知订单侧
                if (PaymentStatus.REFUNDED.equals(targetStatus)) {
                    sendRefundMq(orderNo, refundId, amount);
                }
            } else {
                this.update(null, new LambdaUpdateWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
                        .eq(PaymentInfo::getPaymentStatus, PaymentStatus.PAID)
                        .set(PaymentInfo::getPaymentStatus, PaymentStatus.REFUNDED));
                log.info("[MOCK] 退款处理完成: orderNo={}, amount={}", orderNo, amount);
                sendRefundMq(orderNo, "mock_refund_" + IdUtils.fastSimpleUUID().substring(0, 16), amount);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("退款处理被中断");
        } catch (Exception e) {
            log.error("退款处理异常: orderNo={}", orderNo, e);
            throw new ServiceException("退款处理异常: " + e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * handlePayCallback 的 Sentinel 降级方法（仅限流/熔断时触发）
     */
    public Map<String, String> handlePayCallbackFallback(HttpServletRequest request, Throwable t) {
        log.error("支付回调被限流降级: {}", t.getMessage());
        return buildWechatFailResponse("服务繁忙");
    }

    @Override
    public List<PaymentInfo> selectPaymentList(PaymentInfo query) {
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        if (query.getOrderNo() != null && !query.getOrderNo().isEmpty()) {
            wrapper.like(PaymentInfo::getOrderNo, query.getOrderNo());
        }
        if (query.getPaymentStatus() != null) {
            wrapper.eq(PaymentInfo::getPaymentStatus, query.getPaymentStatus());
        }
        if (query.getUserId() != null) {
            wrapper.eq(PaymentInfo::getUserId, query.getUserId());
        }
        wrapper.orderByDesc(PaymentInfo::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public Map<String, Object> getPaymentMapByOrderNo(String orderNo) {
        PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo));
        if (paymentInfo == null) {
            return Map.of();
        }
        return Map.of(
                "id", paymentInfo.getId(),
                "orderNo", paymentInfo.getOrderNo(),
                "amount", paymentInfo.getAmount(),
                "paymentStatus", paymentInfo.getPaymentStatus(),
                "transactionId", paymentInfo.getTransactionId(),
                "payWay", paymentInfo.getPayWay(),
                "userId", paymentInfo.getUserId()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> handleRefundCallback(HttpServletRequest request) {
        try {
            if (wxPayConfig.isMockMode()) {
                log.warn("[MOCK] 收到微信退款回调，mock 模式忽略。");
                return buildWechatSuccessResponse();
            }
            String serial = request.getHeader("Wechatpay-Serial");
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String plainText = wxPayUtil.verifyAndDecryptCallback(serial, signature, timestamp, nonce, body);

            JsonNode root = objectMapper.readTree(plainText);
            String orderNo = root.get("out_trade_no").asText();
            String refundStatus = root.has("refund_status") ? root.get("refund_status").asText() : "";

            if ("SUCCESS".equals(refundStatus)) {
                String transactionId = root.has("transaction_id") ? root.get("transaction_id").asText() : "";
                BigDecimal refundAmount = root.has("refund_amount") ?
                        new BigDecimal(root.get("refund_amount").asText()).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

                this.update(null, new LambdaUpdateWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
                        .eq(PaymentInfo::getPaymentStatus, PaymentStatus.REFUNDING)
                        .set(PaymentInfo::getPaymentStatus, PaymentStatus.REFUNDED));
                log.info("退款回调成功: orderNo={}", orderNo);

                sendRefundMq(orderNo, transactionId, refundAmount);
            } else if ("CLOSED".equals(refundStatus) || "ABNORMAL".equals(refundStatus)) {
                log.warn("退款回调异常: orderNo={}, refundStatus={}", orderNo, refundStatus);
            }
            return buildWechatSuccessResponse();
        } catch (SecurityException e) {
            log.error("微信退款回调验签失败", e);
            return buildWechatFailResponse("签名验证失败");
        } catch (Exception e) {
            log.error("处理微信退款回调异常", e);
            return buildWechatFailResponse("处理异常");
        }
    }

    // ==================== 私有工具方法 ====================

    /**
     * 发送退款成功 MQ，通知订单侧更新状态
     * 失败不抛异常，补偿定时任务兜底
     */
    private void sendRefundMq(String orderNo, String transactionId, BigDecimal refundAmount) {
        boolean sent = paymentMQProducer.sendRefundSuccessMessage(orderNo, transactionId, refundAmount);
        if (!sent) {
            log.warn("退款成功 MQ 发送失败（已记录退款状态，补偿任务将处理）: orderNo={}", orderNo);
        }
    }

    private Map<String, String> buildWechatSuccessResponse() {
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    private Map<String, String> buildWechatFailResponse(String message) {
        return Map.of("code", "FAIL", "message", message);
    }
}
