package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商品分类对象 share_goods.category
 *
 * @author share
 */
@Data
@TableName("category")
@Schema(description = "商品分类")
public class Category extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @Schema(description = "分类ID")
    private Long id;

    /** 父分类ID */
    @Schema(description = "父分类ID")
    private Long parentId;

    /** 分类名称 */
    @Schema(description = "分类名称")
    private String name;

    /** 分类图标 */
    @Schema(description = "分类图标")
    private String icon;

    /** 分类图片 */
    @Schema(description = "分类图片")
    private String image;

    /** 排序 */
    @Schema(description = "排序")
    private Integer sort;

    /** 状态（0正常 1禁用） */
    @Schema(description = "状态")
    private String status;

    /** 扩展字段 */
    @Schema(description = "扩展字段JSON")
    private String extJson;

    /** 子分类列表（树形结构） */
    @Schema(description = "子分类列表")
    private List<Category> children;

}
