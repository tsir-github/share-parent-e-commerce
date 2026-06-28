package com.share.coupon.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户领取记录对象 coupon_user
 *
 * @author share
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_user")
@Schema(description = "用户领取记录")
public class CouponUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Schema(description = "记录ID")
    private Long id;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 模板ID */
    @Schema(description = "模板ID")
    private Long templateId;

    /** 订单号 */
    @Schema(description = "订单号")
    private String orderNo;

    /** 状态：0-未使用 1-已使用 2-已过期 */
    @Schema(description = "状态：0-未使用 1-已使用 2-已过期")
    private String status;

    /** 使用时间 */
    @Schema(description = "使用时间")
    private Date usedTime;
}
