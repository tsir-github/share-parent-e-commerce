package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀活动对象 share_goods.seckill_activity
 *
 * @author share
 */
@Data
@TableName("seckill_activity")
@Schema(description = "秒杀活动")
public class SeckillActivity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @Schema(description = "活动ID")
    private Long id;

    /** 活动名称 */
    @Schema(description = "活动名称")
    private String name;

    /** 商家ID */
    @Schema(description = "商家ID")
    private Long merchantId;

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long productId;

    /** SKU ID */
    @Schema(description = "SKU ID")
    private Long skuId;

    /** 秒杀价 */
    @Schema(description = "秒杀价")
    private BigDecimal seckillPrice;

    /** 秒杀库存 */
    @Schema(description = "秒杀库存")
    private Integer stock;

    /** 每人限购数量 */
    @Schema(description = "每人限购数量")
    private Integer limitPerUser;

    /** 排序权重 */
    @Schema(description = "排序权重")
    private Integer sort;

    /** 活动开始时间 */
    @Schema(description = "活动开始时间")
    private Date startTime;

    /** 活动结束时间 */
    @Schema(description = "活动结束时间")
    private Date endTime;

    /** 状态（0-未开始 1-进行中 2-已结束 3-已禁用） */
    @Schema(description = "状态")
    private String status;

    /** 乐观锁版本号 */
    @Schema(description = "乐观锁版本号")
    private Integer version;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    // ==================== 非数据库字段 ====================

    @TableField(exist = false)
    @Schema(description = "商品名称")
    private String productName;

    @TableField(exist = false)
    @Schema(description = "SKU规格JSON")
    private String skuSpecs;

    @TableField(exist = false)
    @Schema(description = "商品主图")
    private String mainImage;
}
