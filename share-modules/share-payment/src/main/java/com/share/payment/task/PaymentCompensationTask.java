package com.share.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.PaymentStatus;
import com.share.payment.domain.PaymentInfo;
import com.share.payment.mapper.PaymentInfoMapper;
import com.share.payment.mq.PaymentMQProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 支付成功消息 MQ 补偿定时任务
 *
 * 兜底方案：当支付状态已更新为 PAID 但 MQ 消息未送达（生产者 catch 了异常），
 * 此任务定期扫描并重新发送。
 *
 * 事务边界：只读扫描 + MQ 发送，不涉及本地事务。
 * 幂等性由消费方（share-order.PaymentSuccessConsumer）保证。
 * 分布式锁防止多实例重复执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompensationTask {

    private final PaymentInfoMapper paymentInfoMapper;
    private final PaymentMQProducer paymentMQProducer;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "payment:task:compensation";
    private static final int SCAN_HOURS = 2;   // 只扫描最近 2 小时的支付记录
    private static final int DELAY_SECONDS = 30; // 回调后至少等 30 秒再补偿（给 MQ 正常发送留时间）

    /**
     * 每 60 秒执行一次补偿扫描
     */
    @Scheduled(fixedRate = 60_000)
    public void compensate() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                log.debug("补偿任务已被其他实例执行");
                return;
            }
            doCompensate();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("补偿任务被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void doCompensate() {
        // 扫描时间窗口：当前时间 - SCAN_HOURS 到 当前时间 - DELAY_SECONDS
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.HOUR, -SCAN_HOURS);
        Date since = cal.getTime();
        cal.setTime(now);
        cal.add(Calendar.SECOND, -DELAY_SECONDS);
        Date before = cal.getTime();

        List<PaymentInfo> list = paymentInfoMapper.selectList(
                new LambdaQueryWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getPaymentStatus, PaymentStatus.PAID)
                        .ge(PaymentInfo::getCallbackTime, since)
                        .le(PaymentInfo::getCallbackTime, before)
                        .orderByAsc(PaymentInfo::getCallbackTime)
                        .last("LIMIT 50"));

        if (list.isEmpty()) {
            return;
        }

        log.info("支付补偿任务: 发现 {} 条待补偿记录", list.size());
        for (PaymentInfo payment : list) {
            try {
                boolean sent = paymentMQProducer.sendPaySuccessMessage(
                        payment.getOrderNo(), payment.getTransactionId());
                if (sent) {
                    log.info("补偿发送成功: orderNo={}", payment.getOrderNo());
                } else {
                    log.warn("补偿发送失败（下次重试）: orderNo={}", payment.getOrderNo());
                }
            } catch (Exception e) {
                log.error("补偿发送异常: orderNo={}", payment.getOrderNo(), e);
            }
        }
    }
}
