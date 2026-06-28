package com.share.coupon.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单可用券查询 DTO
 *
 * @author share
 */
@Data
@Schema(description = "下单可用券查询参数")
public class UsableCouponDTO {

    @NotNull(message = "订单金额不能为空")
    @Schema(description = "订单金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
