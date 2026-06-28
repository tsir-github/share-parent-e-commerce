package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 购物车对象 cart
 *
 * @author share
 */
@Data
@TableName("cart")
@Schema(description = "购物车")
public class Cart extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 购物车ID */
    @Schema(description = "购物车ID")
    private Long id;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long productId;

    /** SKU ID */
    @Schema(description = "SKU ID")
    private Long skuId;

    /** 数量 */
    @Schema(description = "数量")
    private Integer quantity;

    /** 是否选中 */
    @Schema(description = "是否选中")
    private Boolean checked;

    /** 扩展字段 */
    @Schema(description = "扩展字段JSON")
    private String extJson;

}
