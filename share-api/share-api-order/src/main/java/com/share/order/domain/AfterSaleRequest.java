package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 售后申请表 after_sale_request
 *
 * 用户对已完成订单发起售后（仅退款），商家审核 → 客服兜底。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale_request")
@Schema(description = "售后申请")
public class AfterSaleRequest extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    @Schema(description = "订单号")
    private String orderNo;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 商家ID */
    @Schema(description = "商家ID")
    private Long merchantId;

    /** 退款金额 */
    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    /** 退款原因 */
    @Schema(description = "退款原因")
    private String refundReason;

    /** 审核状态：0-待审核 1-商家同意 2-商家拒绝 3-客服介入 4-客服同意退款 5-客服拒绝 */
    @Schema(description = "审核状态：0-待审核 1-商家同意 2-商家拒绝 3-客服介入 4-客服同意退款 5-客服拒绝")
    private String auditStatus;

    /** 审核备注 */
    @Schema(description = "审核备注")
    private String auditRemark;

    /** 审核时间 */
    @Schema(description = "审核时间")
    private Date auditTime;

    /** 审核人 */
    @Schema(description = "审核人")
    private String auditBy;

    /** 关闭时间 */
    @Schema(description = "关闭时间")
    private Date closeTime;

    /** 关闭类型 */
    @Schema(description = "关闭类型")
    private String closeType;

    /** 扩展字段JSON */
    @Schema(description = "扩展字段JSON")
    private String extJson;
}
