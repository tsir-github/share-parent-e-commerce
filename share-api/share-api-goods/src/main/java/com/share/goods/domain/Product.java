package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SPU 对象 share_goods.product
 *
 * @author share
 */
@Data
@TableName("product")
@Schema(description = "商品SPU")
public class Product extends BaseEntity {

    private static final long serialVersionUID = 1L;

    ///** 商品ID */
    //@Schema(description = "ID")
    //private Long id;

    /** 分类ID */
    @Schema(description = "分类ID")
    private Long categoryId;

    /** 商家ID */
    @Schema(description = "商家ID")
    private Long merchantId;

    /** 商品名称 */
    @Schema(description = "商品名称")
    private String name;

    /** 副标题 */
    @Schema(description = "副标题/卖点")
    private String subtitle;

    /** 商品详情 */
    @Schema(description = "商品详情")
    private String description;

    /** 主图 */
    @Schema(description = "主图")
    private String mainImage;

    /** 单位 */
    @Schema(description = "单位")
    private String unit;

    /** 最低售价 */
    @Schema(description = "最低售价")
    private BigDecimal minPrice;

    /** 最高售价 */
    @Schema(description = "最高售价")
    private BigDecimal maxPrice;

    /** 总库存 */
    @Schema(description = "总库存")
    private Integer totalStock;

    /** 销量 */
    @Schema(description = "销量")
    private Integer sales;

    /** 状态（0上架 1下架） */
    @Schema(description = "状态")
    private String status;

    /** 是否新品 */
    @Schema(description = "是否新品")
    private String isNew;

    /** 是否热销 */
    @Schema(description = "是否热销")
    private String isHot;

    /** 是否推荐 */
    @Schema(description = "是否推荐(0-否 1-是)")
    private String isRecommended;

    /** 扩展字段 */
    @Schema(description = "扩展字段JSON")
    private String extJson;

    // ==================== 非数据库字段 ====================

    @TableField(exist = false)
    @Schema(description = "平均评分")
    private Double avgRating;

    @TableField(exist = false)
    @Schema(description = "评价数")
    private Integer reviewCount;
}
