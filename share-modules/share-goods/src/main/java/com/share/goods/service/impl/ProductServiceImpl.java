package com.share.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.ProductStatus;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.goods.domain.Product;
import com.share.goods.mapper.ProductMapper;
import com.share.goods.service.IProductService;
import com.share.order.api.RemoteOrderReviewService;
import com.share.order.domain.vo.ReviewStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    public List<Product> selectListedProducts(String name, Long categoryId) {
        Product query = new Product();
        query.setName(name);
        query.setCategoryId(categoryId);
        query.setStatus(ProductStatus.LISTED);
        return productMapper.selectProductList(query);
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
}
