package com.share.payment.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 付款信息对象 payment_info
 *
 * @author share
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_info")
@Schema(description = "付款信息")
public class PaymentInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 编号 */
    @Schema(description = "编号")
    private Long id;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 订单号 */
    @Schema(description = "订单号")
    private String orderNo;

    /** 付款方式：1-微信 */
    @Schema(description = "付款方式：1-微信")
    private Integer payWay;

    /** 微信支付订单号 */
    @Schema(description = "微信支付订单号")
    private String transactionId;

    /** 支付金额 */
    @Schema(description = "支付金额")
    private BigDecimal amount;

    /** 已退款金额 */
    @Schema(description = "已退款金额")
    private BigDecimal refundAmount;

    /** 最近退款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近退款时间")
    private Date refundTime;

    /** 交易内容 */
    @Schema(description = "交易内容")
    private String content;

    /** 支付状态：0-未支付 1-已支付 */
    @Schema(description = "支付状态：0-未支付 1-已支付")
    private Integer paymentStatus;

    /** 回调时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "回调时间")
    private Date callbackTime;

    /** 回调信息 */
    @Schema(description = "回调信息")
    private String callbackContent;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
