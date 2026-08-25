package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 秒杀订单记录对象 share-goods.seckill_order
 *
 * <p>记录用户秒杀购买记录，用于限购校验和历史查询。
 * 订单详情存在 share-order.order_info（order_type='1'），本表只存秒杀快照。</p>
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_order")
@Schema(description = "秒杀订单记录")
public class SeckillOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    //@Schema(description = "ID")
    //private Long id;

    @Schema(description = "秒杀活动ID")
    private Long activityId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "秒杀价")
    private BigDecimal seckillPrice;
}
