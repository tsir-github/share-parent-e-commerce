package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单评价对象 share-order.order_review
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_review")
@Schema(description = "订单评价")
public class OrderReview extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单项ID")
    private Long orderItemId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称（评价时快照）")
    private String nickname;

    @Schema(description = "用户头像（评价时快照）")
    private String avatar;

    @Schema(description = "评分（1-5）")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "状态（0正常 1隐藏）")
    private String status;
}
