package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.ProductStatus;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.goods.domain.Product;
import com.share.goods.mapper.ProductMapper;
import com.share.goods.service.IProductService;
import com.share.order.api.RemoteOrderReviewService;
import com.share.order.domain.vo.ReviewStatsDTO;
import com.share.order.domain.vo.OrderReviewVO;
import com.share.merchant.api.RemoteMerchantService;
import com.share.common.core.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品Service业务层处理
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    private final ProductMapper productMapper;
    private final RemoteOrderReviewService remoteOrderReviewService;

    /** 默认平台自营商家ID */
    private static final Long DEFAULT_MERCHANT_ID = 1L;

    public List<Product> selectProductList(Product product) {
        return productMapper.selectProductList(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Product entity) {
        if (entity.getMerchantId() == null) {
            entity.setMerchantId(DEFAULT_MERCHANT_ID);
        }
        return super.save(entity);
    }

    @Override
    public List<Product> selectListedProducts(String name, Long categoryId, String tag) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatus.LISTED)
                .eq(Product::getDelFlag, "0");
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (name != null && !name.isEmpty()) {
            wrapper.like(Product::getName, name);
        }
        if ("recommend".equals(tag)) {
            wrapper.eq(Product::getIsRecommended, "1");
        } else if ("hot".equals(tag)) {
            wrapper.eq(Product::getIsHot, "1");
        } else if ("new".equals(tag)) {
            wrapper.eq(Product::getIsNew, "1");
        }
        // 加权排序：hot优先 → 销量 → recommended → 最新
        wrapper.orderByDesc(Product::getIsHot)
               .orderByDesc(Product::getSales)
               .orderByDesc(Product::getIsRecommended)
               .orderByDesc(Product::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Product getListedProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new ServiceException("商品不存在或已下架");
        }
        return product;
    }

    @Override
    public List<Product> searchProducts(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        return productMapper.searchProducts(keyword, categoryId, minPrice, maxPrice, sortBy);
    }

    @Override
    public void populateReviewStats(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> ids = products.stream().map(Product::getId).collect(Collectors.toList());
        try {
            R<Map<Long, ReviewStatsDTO>> statsR = remoteOrderReviewService.getReviewStatsBatch(ids);
            if (statsR != null && statsR.getData() != null) {
                Map<Long, ReviewStatsDTO> statsMap = statsR.getData();
                for (Product p : products) {
                    ReviewStatsDTO stats = statsMap.get(p.getId());
                    p.setAvgRating(stats != null ? stats.getAvgRating() : 0.0);
                    p.setReviewCount(stats != null ? stats.getReviewCount() : 0);
                }
            }
        } catch (Exception e) {
            log.warn("批量获取评价统计失败", e);
        }
    }

    @Override
    public void fillProductReviewStats(Product product) {
        if (product == null) {
            return;
        }
        try {
            R<ReviewStatsDTO> statsR = remoteOrderReviewService.getReviewStats(product.getId());
            if (statsR != null && statsR.getData() != null) {
                product.setAvgRating(statsR.getData().getAvgRating());
                product.setReviewCount(statsR.getData().getReviewCount());
            } else {
                product.setAvgRating(0.0);
                product.setReviewCount(0);
            }
        } catch (Exception e) {
            log.warn("获取商品评价统计失败: productId={}", product.getId(), e);
            product.setAvgRating(0.0);
            product.setReviewCount(0);
        }
    }

    private final RemoteMerchantService remoteMerchantService;

    @Override
    public Object getMerchantInfo(Long merchantId) {
        if (merchantId == null) return null;
        try {
            R<?> result = remoteMerchantService.get(merchantId, SecurityConstants.INNER);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.warn("获取商家信息失败: merchantId={}", merchantId, e);
            return null;
        }
    }

    @Override
    public List<OrderReviewVO> getProductReviews(Long productId) {
        try {
            R<List<OrderReviewVO>> result = remoteOrderReviewService.getProductReviews(productId);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.warn("获取商品评价失败: productId={}", productId, e);
            return null;
        }
    }

    @Override
    public List<Product> selectByMerchantId(Long merchantId, int limit) {
        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                        .eq(Product::getMerchantId, merchantId)
                        .eq(Product::getStatus, ProductStatus.LISTED)
                        .eq(Product::getDelFlag, "0")
                        .orderByDesc(Product::getCreateTime)
                        .last("LIMIT " + limit));
    }

    @Override
    public Map<String, Object> getMerchantStats(Long merchantId) {
        List<Product> products = baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                        .eq(Product::getMerchantId, merchantId)
                        .eq(Product::getStatus, ProductStatus.LISTED)
                        .eq(Product::getDelFlag, "0"));
        int productCount = products.size();
        int totalSales = products.stream().mapToInt(p -> p.getSales() != null ? p.getSales() : 0).sum();
        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productCount);
        stats.put("totalSales", totalSales);
        return stats;
    }
}
