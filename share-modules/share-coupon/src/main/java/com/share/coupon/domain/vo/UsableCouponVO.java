package com.share.coupon.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 下单可用优惠券 VO
 *
 * @author share
 */
@Data
@Builder
@Schema(description = "下单可用优惠券")
public class UsableCouponVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "模板ID")
    private Long templateId;

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

    @Schema(description = "实际可减免金额")
    private BigDecimal discount;

    @Schema(description = "有效期结束")
    private Date endTime;
}
