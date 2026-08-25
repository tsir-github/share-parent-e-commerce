package com.share.goods.domain.vo;

import com.share.goods.domain.Banner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Banner VO（C端公开）
 *
 * @author share
 */
@Data
@Schema(description = "Banner VO")
public class BannerVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "跳转链接")
    private String linkUrl;

    public static BannerVO from(Banner b) {
        BannerVO vo = new BannerVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setImageUrl(b.getImageUrl());
        vo.setLinkUrl(b.getLinkUrl());
        return vo;
    }
}
