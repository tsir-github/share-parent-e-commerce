package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价晒图对象 share-order.review_image
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_image")
@Schema(description = "评价晒图")
public class ReviewImage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单项ID")
    private Long orderItemId;

    @Schema(description = "评价ID")
    private Long reviewId;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
