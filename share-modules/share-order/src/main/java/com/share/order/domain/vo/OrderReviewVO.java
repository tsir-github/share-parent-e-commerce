package com.share.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 评价返回 VO
 *
 * @author share
 */
@Data
@Schema(description = "评价信息")
public class OrderReviewVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "订单项ID")
    private Long orderItemId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "评分")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "晒图列表")
    private List<String> images;

    @Schema(description = "评价时间")
    private Date createTime;
}
