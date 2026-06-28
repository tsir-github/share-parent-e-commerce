package com.share.coupon.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板对象 coupon_template
 *
 * @author share
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_template")
@Schema(description = "优惠券模板")
public class CouponTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 模板ID */
    @Schema(description = "模板ID")
    private Long id;

    /** 优惠券名称 */
    @Schema(description = "优惠券名称")
    private String name;

    /** 商家ID */
    @Schema(description = "商家ID")
    private Long merchantId;

    /** 优惠券类型：0-现金券 1-折扣券 */
    @Schema(description = "优惠券类型：0-现金券 1-折扣券")
    private String type;

    /** 满减条件金额 */
    @Schema(description = "满减条件金额")
    private BigDecimal conditionAmt;

    /** 优惠金额（现金券） */
    @Schema(description = "优惠金额（现金券）")
    private BigDecimal discountAmt;

    /** 折扣率（折扣券） */
    @Schema(description = "折扣率（折扣券）")
    private BigDecimal discountRate;

    /** 发行总量 */
    @Schema(description = "发行总量")
    private Integer totalCount;

    /** 剩余数量（-1表示不限） */
    @Schema(description = "剩余数量")
    private Integer remainCount;

    /** 每人限领数量 */
    @Schema(description = "每人限领数量")
    private Integer limitPerUser;

    /** 有效期开始时间 */
    @Schema(description = "有效期开始时间")
    private Date startTime;

    /** 有效期结束时间 */
    @Schema(description = "有效期结束时间")
    private Date endTime;

    /** 状态：0-未启用 1-已启用 2-已过期 */
    @Schema(description = "状态：0-未启用 1-已启用 2-已过期")
    private String status;

    /** 乐观锁版本号 */
    @Schema(description = "乐观锁版本号")
    private Integer version;
}
