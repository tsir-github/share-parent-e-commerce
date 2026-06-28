package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.ProductStatus;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.goods.domain.Product;
import com.share.goods.domain.ProductImage;
import com.share.goods.mapper.ProductImageMapper;
import com.share.goods.mapper.ProductMapper;
import com.share.goods.service.IMerchantProductService;
import com.share.goods.service.IProductService;
import com.share.system.api.RemoteFileService;
import com.share.system.api.domain.SysFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家端商品 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class MerchantProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IMerchantProductService {

    private final IProductService productService;
    private final RemoteFileService remoteFileService;
    private final ProductImageMapper productImageMapper;

    @Override
    public List<Product> selectMerchantProductList(Product product, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        product.setMerchantId(merchantId);
        return productService.selectProductList(product);
    }

    @Override
    public Product getMerchantProduct(Long id, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        Product product = productService.getById(id);
        if (product == null) {
            throw new ServiceException("商品不存在");
        }
        if (!merchantId.equals(product.getMerchantId())) {
            throw new ServiceException("无权访问");
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addMerchantProduct(Product product, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        if (StringUtils.isEmpty(product.getName())) {
            throw new ServiceException("商品名称不能为空");
        }
        product.setMerchantId(merchantId);
        product.setId(null);
        productService.save(product);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchantProduct(Long id, Product product, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        Product existing = productService.getById(id);
        if (existing == null) {
            throw new ServiceException("商品不存在");
        }
        if (!merchantId.equals(existing.getMerchantId())) {
            throw new ServiceException("无权操作");
        }
        product.setId(id);
        product.setMerchantId(null);
        productService.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMerchantProduct(Long id, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        Product existing = productService.getById(id);
        if (existing == null) {
            throw new ServiceException("商品不存在");
        }
        if (!merchantId.equals(existing.getMerchantId())) {
            throw new ServiceException("无权操作");
        }
        productService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> uploadProductImages(Long productId, MultipartFile[] files, String type, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        Product existing = productService.getById(productId);
        if (existing == null) throw new ServiceException("商品不存在");
        if (!merchantId.equals(existing.getMerchantId())) throw new ServiceException("无权操作");

        List<Map<String, Object>> result = new ArrayList<>();

        // 获取当前最大排序号
        ProductImage maxSortImg = productImageMapper.selectOne(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .orderByDesc(ProductImage::getSortOrder)
                        .last("LIMIT 1"));
        int baseSort = (maxSortImg != null && maxSortImg.getSortOrder() != null) ? maxSortImg.getSortOrder() : 0;

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            R<SysFile> uploadResult = remoteFileService.upload(file);
            if (uploadResult == null || uploadResult.getData() == null) {
                throw new ServiceException("图片上传失败: " + file.getOriginalFilename());
            }
            String url = uploadResult.getData().getUrl();
            int sortOrder = baseSort + i + 1;

            ProductImage pi = new ProductImage();
            pi.setProductId(productId);
            pi.setImageUrl(url);
            pi.setSortOrder(sortOrder);
            productImageMapper.insert(pi);

            Map<String, Object> item = new HashMap<>();
            item.put("url", url);
            item.put("sortOrder", sortOrder);
            result.add(item);

            // 第一张图且 type=main 时更新主图字段
            if (i == 0 && "main".equals(type)) {
                baseMapper.update(null, new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, productId)
                        .set(Product::getMainImage, url));
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> batchUpdateStatus(List<Long> ids, String status, Long merchantId) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        if (ids == null || ids.isEmpty()) throw new ServiceException("商品ID列表不能为空");
        if (!ProductStatus.LISTED.equals(status) && !ProductStatus.UNLISTED.equals(status)) {
            throw new ServiceException("无效的状态值");
        }

        int success = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Long id : ids) {
            try {
                Product existing = productService.getById(id);
                if (existing == null) throw new ServiceException("商品不存在");
                if (!merchantId.equals(existing.getMerchantId())) throw new ServiceException("无权操作");
                if (status.equals(existing.getStatus())) {
                    String msg = ProductStatus.LISTED.equals(status) ? "已经是上架状态" : "已经是下架状态";
                    throw new ServiceException(msg);
                }

                baseMapper.update(null, new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, id)
                        .set(Product::getStatus, status));
                success++;
            } catch (Exception e) {
                failed++;
                Map<String, Object> err = new HashMap<>();
                err.put("id", id);
                err.put("reason", e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }
}
