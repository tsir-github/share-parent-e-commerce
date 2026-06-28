package com.share.coupon.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板创建/修改 DTO
 *
 * @author share
 */
@Data
@Schema(description = "优惠券模板参数")
public class CouponTemplateDTO {

    @Schema(description = "模板ID（修改时传）")
    private Long id;

    @NotBlank(message = "优惠券名称不能为空")
    @Schema(description = "优惠券名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "优惠券类型不能为空")
    @Pattern(regexp = "[012]", message = "类型必须为0(满减)/1(折扣)/2(无门槛)")
    @Schema(description = "类型：0-满减 1-折扣 2-无门槛", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "满减条件金额（满减/折扣券必填）")
    private BigDecimal conditionAmt;

    @Schema(description = "减免金额（满减/无门槛券必填）")
    private BigDecimal discountAmt;

    @DecimalMin(value = "0.01", message = "折扣率不能小于0.01")
    @DecimalMax(value = "1.00", message = "折扣率不能大于1.00")
    @Schema(description = "折扣率（折扣券必填，如0.85=85折）")
    private BigDecimal discountRate;

    @NotNull(message = "发行总量不能为空")
    @Min(value = 1, message = "发行总量至少为1")
    @Schema(description = "发行总量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalCount;

    @Min(value = 1, message = "每人限领至少为1")
    @Schema(description = "每人限领数量（默认1）")
    private Integer limitPerUser;

    @NotNull(message = "有效期开始不能为空")
    @Schema(description = "有效期开始", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date startTime;

    @NotNull(message = "有效期结束不能为空")
    @Schema(description = "有效期结束", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date endTime;

    @Size(max = 255, message = "备注长度不能超过255")
    @Schema(description = "备注")
    private String remark;
}
