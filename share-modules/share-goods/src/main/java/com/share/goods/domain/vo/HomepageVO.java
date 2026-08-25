package com.share.goods.domain.vo;

import com.share.goods.domain.Category;
import com.share.goods.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 首页聚合数据 VO
 *
 * @author share
 */
@Data
@Schema(description = "首页聚合数据")
public class HomepageVO {

    @Schema(description = "Banner轮播图列表")
    private List<BannerVO> bannerList;

    @Schema(description = "推荐商品列表")
    private List<Product> recommendedProducts;

    @Schema(description = "分类导航树")
    private List<Category> categoryTree;
}
