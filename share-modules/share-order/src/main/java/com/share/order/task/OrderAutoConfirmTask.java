package com.share.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.share.common.core.constant.OrderStatus;
import com.share.order.domain.OrderInfo;
import com.share.order.enums.OrderOperateType;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.service.impl.OrderStatusServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 自动确认收货定时任务
 *
 * <p>每小时扫描发货超 7 天仍未确认的订单，自动变更为已完成。
 * RocketMQ 延迟消息最大 2h，7 天延迟用定时任务兜底。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmTask {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderStatusServiceImpl orderStatusService;

    @Scheduled(cron = "0 0 * * * ?")
    public void execute() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date sevenDaysAgo = cal.getTime();

        List<OrderInfo> orders = orderInfoMapper.selectList(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getStatus, OrderStatus.DELIVERING)
                        .eq(OrderInfo::getDelFlag, "0")
                        .le(OrderInfo::getDeliveryTime, sevenDaysAgo));
        if (orders.isEmpty()) return;

        log.info("自动确认收货扫描: 待处理 {} 笔", orders.size());
        for (OrderInfo o : orders) {
            try {
                orderStatusService.transition(
                        o.getId(), o.getOrderNo(),
                        OrderOperateType.CONFIRM.getCode(),
                        OrderStatus.COMPLETED,
                        "系统", "7 天自动确认收货"
                );
                orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getId, o.getId())
                        .set(OrderInfo::getReceiveTime, new Date()));
                log.info("自动确认收货: orderNo={}", o.getOrderNo());
            } catch (Exception e) {
                log.warn("自动确认收货失败: orderNo={}, msg={}", o.getOrderNo(), e.getMessage());
            }
        }
    }
}
