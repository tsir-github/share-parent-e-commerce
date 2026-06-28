package com.share.order.service.impl;

import com.share.common.core.exception.ServiceException;
import com.share.common.core.constant.OrderStatus;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.OrderLog;
import com.share.order.enums.OrderOperateType;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.mapper.OrderLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单状态引擎实现
 *
 * <pre>
 * 状态机转换规则：
 *   待支付(0) ──支付──→ 待发货(1)
 *   待支付(0) ──取消──→ 已取消(4)
 *   待发货(1) ──发货──→ 配送中(2)
 *   待发货(1) ──取消──→ 已取消(4)
 *   配送中(2) ──送达──→ 已完成(3)
 *   配送中(2) ──确认──→ 已完成(3)
 *   已完成(3) ──售后──→ 售后中(5)
 *   售后中(5) ──退款──→ 已取消(4)
 *   已取消(4) 终态，不可操作
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusServiceImpl {


    private final OrderInfoMapper orderInfoMapper;
    private final OrderLogMapper orderLogMapper;

    // ==================== 状态机规则表 ====================
    private static final Map<String, String> OPERATE_TO_TARGET = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> STATUS_TO_OPERATES = new ConcurrentHashMap<>();

    static {
        OPERATE_TO_TARGET.put(OrderOperateType.CREATE.getCode(), OrderStatus.PENDING_PAY);
        OPERATE_TO_TARGET.put(OrderOperateType.PAY.getCode(), OrderStatus.PENDING_DELIVERY);
        OPERATE_TO_TARGET.put(OrderOperateType.DELIVER.getCode(), OrderStatus.DELIVERING);
        OPERATE_TO_TARGET.put(OrderOperateType.ARRIVE.getCode(), OrderStatus.COMPLETED);
        OPERATE_TO_TARGET.put(OrderOperateType.CONFIRM.getCode(), OrderStatus.COMPLETED);
        OPERATE_TO_TARGET.put(OrderOperateType.CANCEL.getCode(), OrderStatus.CANCELLED);
        OPERATE_TO_TARGET.put(OrderOperateType.AFTER_SALE.getCode(), OrderStatus.AFTER_SALE);
        OPERATE_TO_TARGET.put(OrderOperateType.REFUND.getCode(), OrderStatus.CANCELLED);

        STATUS_TO_OPERATES.put(OrderStatus.PENDING_PAY, Set.of(OrderOperateType.PAY.getCode(), OrderOperateType.CANCEL.getCode()));
        STATUS_TO_OPERATES.put(OrderStatus.PENDING_DELIVERY, Set.of(OrderOperateType.DELIVER.getCode(), OrderOperateType.CANCEL.getCode()));
        STATUS_TO_OPERATES.put(OrderStatus.DELIVERING, Set.of(OrderOperateType.ARRIVE.getCode(), OrderOperateType.CONFIRM.getCode()));
        STATUS_TO_OPERATES.put(OrderStatus.COMPLETED, Set.of(OrderOperateType.AFTER_SALE.getCode(), OrderOperateType.REFUND.getCode(), OrderOperateType.CANCEL.getCode()));
        STATUS_TO_OPERATES.put(OrderStatus.AFTER_SALE, Set.of(OrderOperateType.REFUND.getCode()));
        // CANCELLED 终态，无允许操作
    }

    public void validateTransition(OrderInfo order, String targetStatus) {
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        String currentStatus = order.getStatus();
        if (currentStatus != null && currentStatus.equals(targetStatus)) {
            return; // 幂等
        }
        if (OrderStatus.CANCELLED.equals(currentStatus)) {
            throw new ServiceException("订单已取消，不可操作");
        }
        Set<String> allowedOperates = STATUS_TO_OPERATES.get(currentStatus);
        if (allowedOperates == null) {
            throw new ServiceException("当前状态不允许任何操作: status=" + currentStatus);
        }
        boolean canTransition = allowedOperates.stream()
                .anyMatch(op -> targetStatus.equals(OPERATE_TO_TARGET.get(op)));
        if (!canTransition) {
            throw new ServiceException(
                    String.format("非法状态转换: %s → %s", currentStatus, targetStatus));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void transition(Long orderId, String orderNo, String operateType,
                           String targetStatus, String operateUser, String note) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在: id=" + orderId);
        }
        if (order.getStatus() != null && order.getStatus().equals(targetStatus)) {
            log.info("订单状态幂等跳过: orderNo={}, status={}", orderNo, targetStatus);
            return;
        }
        validateTransition(order, targetStatus);

        String beforeStatus = order.getStatus();

        // 乐观锁更新：同时校验当前状态和版本号，防止并发覆盖
        int updated = orderInfoMapper.updateOrderStatus(orderId, targetStatus, order.getVersion(), beforeStatus);
        if (updated == 0) {
            throw new ServiceException("订单状态更新失败，数据已被修改: orderNo=" + orderNo);
        }

        // 记操作流水
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(orderId);
        logEntry.setOrderNo(orderNo);
        logEntry.setOperateType(operateType);
        logEntry.setBeforeStatus(beforeStatus);
        logEntry.setAfterStatus(targetStatus);
        logEntry.setOperateUser(operateUser != null ? operateUser : "系统");
        logEntry.setNote(note);
        orderLogMapper.insert(logEntry);

        log.info("订单状态变更: orderNo={}, {}→{} (操作={}, 操作人={})",
                orderNo, beforeStatus, targetStatus, operateType, operateUser);
    }

    public String getTargetStatusByOperate(String operateType) {
        return OPERATE_TO_TARGET.get(operateType);
    }
}
