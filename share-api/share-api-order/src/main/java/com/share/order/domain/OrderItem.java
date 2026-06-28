package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细对象 order_item
 *
 * @author share
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_item")
@Schema(description = "订单明细")
public class OrderItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @Schema(description = "订单ID")
    private Long orderId;

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long productId;

    /** 商家ID */
    @Schema(description = "商家ID")
    private Long merchantId;

    /** SKU ID */
    @Schema(description = "SKU ID")
    private Long skuId;

    /** 商品名称 */
    @Schema(description = "商品名称")
    private String productName;

    /** SKU规格（JSON） */
    @Schema(description = "SKU规格")
    private String skuSpecs;

    /** 商品图片 */
    @Schema(description = "商品图片")
    private String productImage;

    /** 单价 */
    @Schema(description = "单价")
    private BigDecimal price;

    /** 数量 */
    @Schema(description = "数量")
    private Integer quantity;

    /** 总金额 */
    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    /** 扩展字段 */
    @Schema(description = "扩展字段JSON")
    private String extJson;
}
