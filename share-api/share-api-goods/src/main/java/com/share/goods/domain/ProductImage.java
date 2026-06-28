package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品图片对象 share-goods.product_image
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_image")
@Schema(description = "商品图片")
public class ProductImage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
