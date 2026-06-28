package com.share.payment.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 支付成功消息（MQ）
 *
 * share-payment 发送 → share-order 消费
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 微信支付交易号（mock 模式为模拟交易号） */
    private String transactionId;
}
