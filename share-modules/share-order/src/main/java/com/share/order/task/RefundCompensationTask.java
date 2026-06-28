package com.share.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.OrderStatus;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.order.domain.OrderInfo;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.service.impl.OrderInfoServiceImpl;
import com.share.payment.api.RemotePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.math.BigDecimal.ZERO;

/**
 * 退款补偿定时任务
 *
 * <pre>
 * 兜底：每分钟检查已退款但订单状态未同步的记录。
 * 通过 Feign 查询支付侧状态，已退款则重新触发 processRefundSuccess()。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundCompensationTask {

    private final RedissonClient redissonClient;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderInfoServiceImpl orderInfoService;
    private final RemotePaymentService remotePaymentService;

    /**
     * 每分钟执行一次补偿扫描
     */
    @Scheduled(cron = "0 * * * * ?")
    public void compensateRefundStatus() {
        RLock lock = redissonClient.getLock("compensate:refund:lock");
        try {
            if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                log.debug("退款补偿任务被其他实例持有，跳过");
                return;
            }
            doCompensate();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("退款补偿任务中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void doCompensate() {
        // 扫描已支付但未全额退款的已完成/售后中订单（最多100条/次）
        List<OrderInfo> orders = orderInfoMapper.selectList(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getPayStatus, OrderStatus.PAY_PAID)
                        .in(OrderInfo::getStatus, OrderStatus.COMPLETED, OrderStatus.AFTER_SALE)
                        .last("LIMIT 100")
        );
        if (orders.isEmpty()) {
            return;
        }
        log.info("退款补偿扫描: 发现 {} 条待检查订单", orders.size());

        for (OrderInfo order : orders) {
            try {
                R<Integer> result = remotePaymentService.getPaymentStatus(
                        order.getOrderNo(), SecurityConstants.INNER);
                if (result.getCode() != 200 || result.getData() == null) {
                    log.warn("查询支付状态失败: orderNo={}, msg={}", order.getOrderNo(), result.getMsg());
                    continue;
                }
                // payment_status: 0-未支付 1-已支付 2-已退款 3-退款中
                int paymentStatus = result.getData();
                if (paymentStatus != 2 && paymentStatus != 3) {
                    continue;
                }
                // ponytail: 重新触发processRefundSuccess，传剩余应付金额确保全额退款同步
                BigDecimal existingRefund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
                BigDecimal remaining = order.getPayAmount().subtract(existingRefund);
                orderInfoService.processRefundSuccess(order.getOrderNo(), "compensate", remaining);
                log.info("补偿退款成功: orderNo={}, paymentStatus={}", order.getOrderNo(), paymentStatus);
            } catch (Exception e) {
                log.error("补偿退款异常: orderNo={}", order.getOrderNo(), e);
            }
        }
    }
}
