package com.share.goods.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 参与秒杀结果 VO
 *
 * @author share
 */
@Data
@AllArgsConstructor
@Schema(description = "秒杀结果")
public class SeckillOrderResultVO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "秒杀价")
    private BigDecimal seckillPrice;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;
}
