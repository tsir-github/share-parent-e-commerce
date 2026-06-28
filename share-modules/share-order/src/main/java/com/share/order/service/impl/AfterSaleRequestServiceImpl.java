package com.share.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.OrderStatus;
import com.share.common.core.constant.SecurityConstants;
import com.share.order.constant.AfterSaleStatus;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.AfterSaleRequest;
import com.share.order.domain.OrderInfo;
import com.share.order.enums.OrderOperateType;
import com.share.order.mapper.AfterSaleRequestMapper;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.service.IAfterSaleRequestService;
import com.share.payment.api.RemotePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import static java.math.BigDecimal.ZERO;

/**
 * 售后申请Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleRequestServiceImpl extends ServiceImpl<AfterSaleRequestMapper, AfterSaleRequest>
        implements IAfterSaleRequestService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderStatusServiceImpl orderStatusService;
    private final RemotePaymentService remotePaymentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String auditStatus, String auditRemark) {
        AfterSaleRequest request = baseMapper.selectById(id);
        if (request == null) {
            throw new ServiceException("售后申请不存在: id=" + id);
        }
        if (!AfterSaleStatus.PENDING.equals(request.getAuditStatus())) {
            throw new ServiceException("售后申请已处理，不可重复审核");
        }
        if (!Set.of(AfterSaleStatus.APPROVED, AfterSaleStatus.REJECTED, AfterSaleStatus.SERVICE_INTERVENTION).contains(auditStatus)) {
            throw new ServiceException("无效的审核状态");
        }

        baseMapper.update(null, new LambdaUpdateWrapper<AfterSaleRequest>()
                .eq(AfterSaleRequest::getId, request.getId())
                .set(AfterSaleRequest::getAuditStatus, auditStatus)
                .set(AfterSaleRequest::getAuditRemark, auditRemark)
                .set(AfterSaleRequest::getAuditTime, new Date())
                .set(AfterSaleRequest::getAuditBy, SecurityUtils.getUsername()));

        if (AfterSaleStatus.REJECTED.equals(auditStatus)) {
            // 商家拒绝 → 订单状态从 AFTER_SALE 恢复为 COMPLETED
            OrderInfo order = orderInfoMapper.selectOne(
                    new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, request.getOrderNo()));
            if (order != null) {
                orderStatusService.transition(
                        order.getId(), order.getOrderNo(),
                        OrderOperateType.AFTER_SALE.getCode(),
                        OrderStatus.COMPLETED,
                        SecurityUtils.getUsername(), "商家拒绝退款: " + auditRemark
                );
            }
            return;
        }

        if (AfterSaleStatus.SERVICE_INTERVENTION.equals(auditStatus)) {
            // 客服介入 — 无需操作订单状态，等待客服处理
            log.info("售后申请转客服处理: id={}, orderNo={}", id, request.getOrderNo());
            return;
        }

        // 商家同意 → 调用支付侧退款
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, request.getOrderNo()));
        if (order == null) {
            throw new ServiceException("订单不存在: " + request.getOrderNo());
        }
        doRefund(request, order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAudit(Long id, String auditStatus, String auditRemark) {
        AfterSaleRequest request = baseMapper.selectById(id);
        if (request == null) {
            throw new ServiceException("售后申请不存在: id=" + id);
        }
        if (!AfterSaleStatus.SERVICE_INTERVENTION.equals(request.getAuditStatus()) && !AfterSaleStatus.PENDING.equals(request.getAuditStatus())) {
            throw new ServiceException("当前状态不可客服审核");
        }
        if (!Set.of(AfterSaleStatus.SERVICE_APPROVED, AfterSaleStatus.SERVICE_REJECTED).contains(auditStatus)) {
            throw new ServiceException("无效的客服审核状态");
        }

        baseMapper.update(null, new LambdaUpdateWrapper<AfterSaleRequest>()
                .eq(AfterSaleRequest::getId, request.getId())
                .set(AfterSaleRequest::getAuditStatus, auditStatus)
                .set(AfterSaleRequest::getAuditRemark, auditRemark)
                .set(AfterSaleRequest::getAuditTime, new Date())
                .set(AfterSaleRequest::getAuditBy, SecurityUtils.getUsername()));

        if (AfterSaleStatus.SERVICE_REJECTED.equals(auditStatus)) {
            // 客服拒绝 → 订单状态恢复（如果之前改为 AFTER_SALE）
            OrderInfo order = orderInfoMapper.selectOne(
                    new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, request.getOrderNo()));
            if (order != null && OrderStatus.AFTER_SALE.equals(order.getStatus())) {
                orderStatusService.transition(
                        order.getId(), order.getOrderNo(),
                        OrderOperateType.AFTER_SALE.getCode(),
                        OrderStatus.COMPLETED,
                        SecurityUtils.getUsername(), "客服拒绝退款: " + auditRemark
                );
            }
            return;
        }

        // 客服同意退款 → 调用支付侧退款
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, request.getOrderNo()));
        if (order == null) {
            throw new ServiceException("订单不存在: " + request.getOrderNo());
        }
        doRefund(request, order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(String orderNo, BigDecimal refundAmount, String refundReason) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }

        // 校验订单归属和状态
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new ServiceException("订单不存在: " + orderNo);
        }
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException("无权操作此订单");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new ServiceException("仅已完成订单可申请售后");
        }

        // 幂等：已有待审核申请则拒绝
        AfterSaleRequest exists = baseMapper.selectOne(
                new LambdaQueryWrapper<AfterSaleRequest>()
                        .eq(AfterSaleRequest::getOrderNo, orderNo)
                        .in(AfterSaleRequest::getAuditStatus, AfterSaleStatus.PENDING, AfterSaleStatus.APPROVED, AfterSaleStatus.SERVICE_INTERVENTION, AfterSaleStatus.SERVICE_APPROVED));
        if (exists != null) {
            throw new ServiceException("该订单已有售后申请在处理中");
        }

        // 部分退款校验：累计退款不超过实付金额
        BigDecimal existingRefund = order.getRefundAmount() != null ? order.getRefundAmount() : ZERO;
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : ZERO;
        BigDecimal applyAmount = refundAmount != null ? refundAmount : payAmount;
        if (applyAmount.add(existingRefund).compareTo(payAmount) > 0) {
            throw new ServiceException(
                    String.format("申请退款金额(%.2f)超过可退金额(%.2f)", applyAmount, payAmount.subtract(existingRefund)));
        }

        // 订单状态改为售后中
        orderStatusService.transition(
                order.getId(), order.getOrderNo(),
                OrderOperateType.AFTER_SALE.getCode(),
                OrderStatus.AFTER_SALE,
                SecurityUtils.getUsername(), "用户发起售后申请: " + refundReason
        );

        // 创建售后申请
        AfterSaleRequest request = new AfterSaleRequest();
        request.setOrderNo(orderNo);
        request.setUserId(userId);
        request.setMerchantId(order.getSupplierId()); // supplierId 即商家ID
        request.setRefundAmount(applyAmount);
        request.setRefundReason(refundReason);
        request.setAuditStatus(AfterSaleStatus.PENDING); // 待审核
        baseMapper.insert(request);

        log.info("用户发起售后申请: orderNo={}, userId={}, amount={}", orderNo, userId, request.getRefundAmount());
    }

    @Override
    public List<AfterSaleRequest> selectMyList(Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<AfterSaleRequest>()
                        .eq(AfterSaleRequest::getUserId, userId)
                        .orderByDesc(AfterSaleRequest::getCreateTime));
    }

    @Override
    public List<AfterSaleRequest> selectList(String auditStatus) {
        LambdaQueryWrapper<AfterSaleRequest> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(AfterSaleRequest::getAuditStatus, auditStatus);
        }
        return baseMapper.selectList(wrapper.orderByDesc(AfterSaleRequest::getCreateTime));
    }

    /**
     * 调用支付侧退款
     */
    private void doRefund(AfterSaleRequest request, OrderInfo order) {
        BigDecimal refundAmount = request.getRefundAmount() != null
                ? request.getRefundAmount()
                : order.getPayAmount();

        // ponytail: 部分退款校验 — 累计退款不超过实付金额
        BigDecimal existingRefund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
        if (refundAmount.add(existingRefund).compareTo(payAmount) > 0) {
            throw new ServiceException(
                    String.format("累计退款金额(%.2f+%.2f)超过实付金额(%.2f)",
                            existingRefund, refundAmount, payAmount));
        }

        // 通过 Feign 调用支付侧退款
        R<Void> result = remotePaymentService.refund(
                new com.share.payment.api.RefundRequest(request.getOrderNo(), refundAmount, "售后退款"),
                SecurityConstants.INNER
        );
        if (result.getCode() != 200) {
            throw new ServiceException("调用退款失败: " + result.getMsg());
        }
        log.info("售后审核通过，已发起退款: id={}, orderNo={}, amount={}", request.getId(), request.getOrderNo(), refundAmount);
    }

    @Override
    public List<AfterSaleRequest> selectByMerchantId(Long merchantId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<AfterSaleRequest>()
                        .eq(AfterSaleRequest::getMerchantId, merchantId)
                        .orderByDesc(AfterSaleRequest::getCreateTime));
    }

    @Override
    public AfterSaleRequest getByOrderNo(String orderNo) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<AfterSaleRequest>().eq(AfterSaleRequest::getOrderNo, orderNo));
    }

    @Override
    public AfterSaleRequest getById(Long id) {
        return baseMapper.selectById(id);
    }
}
