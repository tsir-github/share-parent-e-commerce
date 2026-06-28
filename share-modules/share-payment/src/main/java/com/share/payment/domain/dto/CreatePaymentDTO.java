package com.share.payment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付入参 DTO
 */
@Data
@Schema(description = "创建支付请求参数")
public class CreatePaymentDTO {

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额不能小于0.01")
    @Schema(description = "支付金额")
    private BigDecimal amount;

    @NotBlank(message = "交易描述不能为空")
    @Schema(description = "交易描述")
    private String description;

    @NotBlank(message = "微信 openid 不能为空")
    @Schema(description = "微信 openid（JSAPI 支付需要）")
    private String openid;

    @Schema(description = "用户ID（Feign 调用传入）")
    private Long userId;
}