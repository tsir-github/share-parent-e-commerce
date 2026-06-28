package com.share.payment.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 付款信息展示 VO（管理端）
 *
 * 用于管理员查看支付记录列表/详情，金额转字符串展示，状态码转中文说明。
 */
@Data
@Schema(description = "付款信息展示")
public class PaymentInfoVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "付款方式：1-微信")
    private Integer payWay;

    @Schema(description = "付款方式说明")
    public String getPayWayStr() {
        return payWay != null && payWay == 1 ? "微信支付" : "未知";
    }

    @Schema(description = "微信支付交易号")
    private String transactionId;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付金额(元)")
    public String getAmountStr() {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP).toString() : "0.00";
    }

    @Schema(description = "交易内容")
    private String content;

    @Schema(description = "支付状态：0-未支付 1-已支付 2-已退款 3-退款中")
    private Integer paymentStatus;

    @Schema(description = "支付状态说明")
    public String getPaymentStatusStr() {
        if (paymentStatus == null) return "未知";
        return switch (paymentStatus) {
            case 0 -> "未支付";
            case 1 -> "已支付";
            case 2 -> "已退款";
            case 3 -> "退款中";
            default -> "未知";
        };
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "回调时间")
    private Date callbackTime;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;
}