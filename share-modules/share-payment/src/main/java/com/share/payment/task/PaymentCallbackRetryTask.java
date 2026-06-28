package com.share.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.PaymentStatus;
import com.share.payment.domain.PaymentInfo;
import com.share.payment.mapper.PaymentInfoMapper;
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
 * 支付回调丢失主动补偿任务
 *
 * <p>微信支付回调通知最多重试 8 次。如果全部丢失（网络故障、服务重启），
 * 支付记录会一直停留在 UNPAID 状态，用户已扣款但订单未更新。</p>
 *
 * <p>此任务定期扫描超时未支付的记录，记录日志供人工排查，
 * 生产环境可扩展为主动调用微信查单 API（{@code WxPayService.queryOrder()}）。</p>
 *
 * <p>与 {@link PaymentCompensationTask} 互补：
 *   - PaymentCompensationTask 处理"已支付但 MQ 未送达"
 *   - PaymentCallbackRetryTask 处理"回调丢失导致未标记已支付"</p>
 *
 * 事务边界：只读扫描 + 日志，不涉及本地事务。
 * 分布式锁防止多实例重复执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackRetryTask {

    private final PaymentInfoMapper paymentInfoMapper;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "payment:task:callback-retry";
    /** 回调超时阈值（分钟）：创建超过此时间的 UNPAID 记录视为回调丢失 */
    private static final int TIMEOUT_MINUTES = 5;
    /** 单次扫描上限 */
    private static final int SCAN_LIMIT = 100;

    /**
     * 每 120 秒执行一次
     */
    @Scheduled(fixedRate = 120_000)
    public void retryLostCallbacks() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                log.debug("回调补偿任务已被其他实例执行");
                return;
            }
            doRetry();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("回调补偿任务被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void doRetry() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -TIMEOUT_MINUTES);
        Date threshold = cal.getTime();

        List<PaymentInfo> list = paymentInfoMapper.selectList(
                new LambdaQueryWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getPaymentStatus, PaymentStatus.UNPAID)
                        .le(PaymentInfo::getCreateTime, threshold)
                        .orderByAsc(PaymentInfo::getCreateTime)
                        .last("LIMIT " + SCAN_LIMIT));

        if (list.isEmpty()) {
            return;
        }

        log.warn("回调补偿任务: 发现 {} 条疑似回调丢失记录", list.size());
        for (PaymentInfo payment : list) {
            // ponytail: 生产环境此处调用 WxPayService.queryOrderByOutTradeNo(orderNo)
            //           若微信返回 trade_state=SUCCESS，则调用 updateToPaid()
            log.warn("疑似支付回调丢失: orderNo={}, createTime={}, 已等待 {} 分钟",
                    payment.getOrderNo(), payment.getCreateTime(), TIMEOUT_MINUTES);
        }
    }
}
