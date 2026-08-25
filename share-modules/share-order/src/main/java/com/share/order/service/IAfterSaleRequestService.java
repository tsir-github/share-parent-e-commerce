package com.share.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.AfterSaleRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * 售后申请Service接口
 */
public interface IAfterSaleRequestService extends IService<AfterSaleRequest> {

    /**
     * 用户发起售后申请
     *
     * @param orderNo      订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     */
    void apply(String orderNo, BigDecimal refundAmount, String refundReason);

    /**
     * 商家审核售后申请
     *
     * @param id          售后申请ID
     * @param auditStatus 审核状态: 1-商家同意 2-商家拒绝 3-客服介入
     * @param auditRemark 审核备注
     */
    void audit(Long id, String auditStatus, String auditRemark);

    /**
     * 客服审核售后申请
     *
     * @param id          售后申请ID
     * @param auditStatus 审核状态: 4-客服同意退款 5-客服拒绝
     * @param auditRemark 审核备注
     */
    void adminAudit(Long id, String auditStatus, String auditRemark);

    /**
     * 查询用户自己的售后申请列表
     */
    List<AfterSaleRequest> selectMyList(Long userId);

    /**
     * 查询所有售后申请列表（管理端）
     *
     * @param auditStatus 审核状态筛选（可选）
     */
    List<AfterSaleRequest> selectList(String auditStatus);

    /**
     * 根据商家ID查询售后申请列表
     */
    List<AfterSaleRequest> selectByMerchantId(Long merchantId);

    /**
     * 根据订单号查询售后申请
     */
    AfterSaleRequest getByOrderNo(String orderNo);

    /**
     * 根据ID查询售后申请
     */
    AfterSaleRequest getById(Long id);

    /**
     * 统计商家待审核售后申请数
     *
     * @param merchantId 商家ID
     * @return 待审核售后申请数
     */
    long countPendingByMerchant(Long merchantId);
}
