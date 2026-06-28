package com.share.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.goods.domain.ProductSku;
import com.share.goods.mapper.ProductSkuMapper;
import com.share.goods.service.IMerchantSkuService;
import com.share.goods.service.IProductSkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商家端 SKU Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class MerchantSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku> implements IMerchantSkuService {

    private final IProductSkuService skuService;

    @Override
    public List<ProductSku> selectMerchantSkuList(ProductSku sku, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        sku.setMerchantId(merchantId);
        return skuService.selectSkuList(sku);
    }

    @Override
    public List<ProductSku> selectSkuByProductId(Long productId, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        return skuService.lambdaQuery()
                .eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getMerchantId, merchantId)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMerchantSku(ProductSku sku, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        sku.setMerchantId(merchantId);
        sku.setId(null);
        skuService.save(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchantSku(Long id, ProductSku sku, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        ProductSku existing = skuService.getById(id);
        if (existing == null) {
            throw new ServiceException("SKU不存在");
        }
        if (!merchantId.equals(existing.getMerchantId())) {
            throw new ServiceException("无权操作");
        }
        sku.setId(id);
        sku.setMerchantId(null);
        skuService.updateById(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMerchantSku(Long id, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        ProductSku existing = skuService.getById(id);
        if (existing == null) {
            throw new ServiceException("SKU不存在");
        }
        if (!merchantId.equals(existing.getMerchantId())) {
            throw new ServiceException("无权操作");
        }
        skuService.removeById(id);
    }
}
