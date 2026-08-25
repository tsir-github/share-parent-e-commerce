package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 首页轮播图对象 banner
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
@Schema(description = "首页轮播图")
public class Banner extends BaseEntity {

    private static final long serialVersionUID = 1L;

    //@Schema(description = "ID")
    //private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "跳转链接")
    private String linkUrl;

    @Schema(description = "排序权重")
    private Integer sort;

    @Schema(description = "状态(0-禁用 1-启用)")
    private String status;
}
