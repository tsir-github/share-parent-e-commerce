package com.share.goods.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.domain.R;
import com.share.goods.domain.Category;
import com.share.goods.domain.Product;
import com.share.goods.domain.ProductSku;
import com.share.goods.factory.RemoteGoodsFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品服务远程调用
 *
 * @author share
 */
@FeignClient(contextId = "remoteGoodsService",
        value = ServiceNameConstants.GOODS_SERVICE,
        fallbackFactory = RemoteGoodsFallbackFactory.class)
public interface RemoteGoodsService {

    /**
     * 根据 SKU ID 获取 SKU 信息
     */
    @GetMapping("/inner/sku/{skuId}")
    R<ProductSku> getSkuById(@PathVariable("skuId") Long skuId,
                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 批量扣减库存
     */
    @PutMapping("/inner/sku/deductStock")
    R<Boolean> deductStock(@RequestBody List<StockDeductDTO> items,
                           @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 批量归还库存（取消订单回滚）
     */
    @PutMapping("/inner/sku/releaseStock")
    R<Boolean> releaseStock(@RequestBody List<StockDeductDTO> items,
                            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    public static class StockDeductDTO {
        private Long skuId;
        private Integer quantity;

        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
