package com.share.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品评价统计 DTO
 *
 * @author share
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品评价统计")
public class ReviewStatsDTO {

    @Schema(description = "平均评分")
    private Double avgRating;

    @Schema(description = "评价总数")
    private Integer reviewCount;
}
