package com.share.coupon.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 我的优惠券 VO
 *
 * @author share
 */
@Data
@Builder
@Schema(description = "我的优惠券")
public class MyCouponVO {

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

    @Schema(description = "状态：0-未使用 1-已使用 2-已过期")
    private String status;

    @Schema(description = "有效期开始")
    private Date startTime;

    @Schema(description = "有效期结束")
    private Date endTime;

    @Schema(description = "使用时间")
    private Date usedTime;

    @Schema(description = "领取时间")
    private Date createTime;
}
