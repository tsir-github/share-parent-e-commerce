package com.share.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 待评价订单项 VO
 *
 * @author share
 */
@Data
@Schema(description = "待评价订单项")
public class PendingReviewVO {

    @Schema(description = "订单项ID")
    private Long orderItemId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "单价")
    private BigDecimal price;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "规格描述")
    private String skuSpecs;

    @Schema(description = "收货时间")
    private Date receiveTime;
}
