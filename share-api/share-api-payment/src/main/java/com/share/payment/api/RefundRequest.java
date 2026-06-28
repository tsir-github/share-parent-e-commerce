package com.share.payment.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 退款请求 DTO（Feign 调用用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 退款金额 */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;
}
