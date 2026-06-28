package com.share.payment.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 退款成功消息（MQ）
 *
 * share-payment 发送 → share-order 消费
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundSuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 微信退款单号 */
    private String transactionId;

    /** 退款金额 */
    private BigDecimal refundAmount;
}
