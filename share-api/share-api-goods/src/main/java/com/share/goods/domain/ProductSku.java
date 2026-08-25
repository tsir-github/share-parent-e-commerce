package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU 对象 share_goods.product_sku
 *
 * @author share
 */
@Data
@TableName("product_sku")
@Schema(description = "商品SKU")
public class ProductSku extends BaseEntity {

    private static final long serialVersionUID = 1L;

    ///** SKU ID */
    //@Schema(description = "ID")
    //private Long id;

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long productId;

    /** 商家ID */
    @Schema(description = "商家ID")
    private Long merchantId;

    /** 规格值JSON */
    @Schema(description = "规格值JSON")
    private String specs;

    /** 售价 */
    @Schema(description = "售价")
    private BigDecimal price;

    /** 原价 */
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    /** 库存 */
    @Schema(description = "库存")
    private Integer stock;

    /** SKU图片 */
    @Schema(description = "SKU图片")
    private String image;

    /** 销量 */
    @Schema(description = "销量")
    private Integer sales;

    /** 状态（0启用 1禁用） */
    @Schema(description = "状态")
    private String status;

    /** 扩展字段 */
    @Schema(description = "扩展字段JSON")
    private String extJson;

    /** 乐观锁版本号 */
    @Schema(description = "乐观锁版本号")
    private Integer version;

}
