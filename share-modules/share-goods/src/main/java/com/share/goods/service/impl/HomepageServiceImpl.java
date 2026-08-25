package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.CacheConstants;
import com.share.common.redis.service.RedisService;
import com.share.goods.domain.Product;
import com.share.goods.domain.vo.BannerVO;
import com.share.goods.domain.vo.HomepageVO;
import com.share.goods.service.IHomepageService;
import com.share.goods.service.IBannerService;
import com.share.goods.service.ICategoryCacheService;
import com.share.goods.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 首页数据 Service 实现 — Cache-Aside
 *
 * <p>缓存首页聚合数据（Banner + 推荐商品 + 分类树），TTL 5min ± 20% 随机偏移防雪崩。</p>
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomepageServiceImpl implements IHomepageService {

    private static final long TTL_MINUTES = 5;
    private static final double TTL_JITTER = 0.2;

    private final IBannerService bannerService;
    private final IProductService productService;
    private final ICategoryCacheService categoryCacheService;
    private final RedisService redisService;

    @Override
    public HomepageVO getHomepage() {
        // 尝试从缓存取
        try {
            HomepageVO cached = redisService.getCacheObject(CacheConstants.HOMEPAGE_DATA_KEY);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("首页缓存读取异常，回源聚合", e);
        }

        // 聚合数据
        HomepageVO vo = new HomepageVO();

        // Banner（启用中）
        List<BannerVO> bannerList = bannerService.selectEnabledBanners().stream()
                .map(BannerVO::from).collect(Collectors.toList());
        vo.setBannerList(bannerList);

        // 推荐商品
        List<Product> recommended = productService.list(new LambdaQueryWrapper<Product>()
                .eq(Product::getIsRecommended, "1")
                .eq(Product::getStatus, "1")
                .eq(Product::getDelFlag, "0")
                .last("LIMIT 20"));
        vo.setRecommendedProducts(recommended);

        // 分类树
        vo.setCategoryTree(categoryCacheService.getCategoryTreeWithCache());

        // 写入缓存（随机 TTL 防雪崩）
        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(CacheConstants.HOMEPAGE_DATA_KEY, vo, ttl, TimeUnit.MINUTES);
            log.debug("首页缓存写入");
        } catch (Exception e) {
            log.warn("首页缓存写入失败", e);
        }
        return vo;
    }

    private long ttlWithJitter(long baseMinutes) {
        return (long) (baseMinutes * (0.8 + TTL_JITTER * 2 * Math.random()));
    }
}
