package com.share.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交评价 DTO
 *
 * @author share
 */
@Data
@Schema(description = "提交评价")
public class SubmitReviewDTO {

    @NotNull(message = "订单项ID不能为空")
    @Schema(description = "订单项ID")
    private Long orderItemId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    @Schema(description = "评分（1-5）")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;
}
