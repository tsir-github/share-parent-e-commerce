package com.share.coupon.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 可领取优惠券列表 VO
 *
 * @author share
 */
@Data
@Builder
@Schema(description = "可领取优惠券")
public class AvailableCouponVO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "类型：0-满减 1-折扣 2-无门槛")
    private String type;

    @Schema(description = "满减条件金额")
    private BigDecimal conditionAmt;

    @Schema(description = "减免金额（满减/无门槛）")
    private BigDecimal discountAmt;

    @Schema(description = "折扣率（折扣券）")
    private BigDecimal discountRate;

    @Schema(description = "有效期开始")
    private Date startTime;

    @Schema(description = "有效期结束")
    private Date endTime;

    @Schema(description = "剩余数量（-1不限量）")
    private Integer remainCount;

    @Schema(description = "发行总量")
    private Integer totalCount;

    @Schema(description = "每人限领")
    private Integer limitPerUser;

    @Schema(description = "当前用户已领取数量")
    private Integer claimedCount;
}
