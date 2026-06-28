package com.share.goods.factory;

import com.share.common.core.domain.R;
import com.share.goods.api.RemoteGoodsService;
import com.share.goods.domain.ProductSku;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品服务降级处理
 *
 * @author share
 */
@Component
public class RemoteGoodsFallbackFactory implements FallbackFactory<RemoteGoodsService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteGoodsFallbackFactory.class);

    @Override
    public RemoteGoodsService create(Throwable throwable) {
        log.error("商品服务调用失败:{}", throwable.getMessage());
        return new RemoteGoodsService() {
            @Override
            public R<ProductSku> getSkuById(Long skuId, String source) {
                return R.fail("获取SKU信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deductStock(List<RemoteGoodsService.StockDeductDTO> items, String source) {
                return R.fail("扣减库存失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> releaseStock(List<RemoteGoodsService.StockDeductDTO> items, String source) {
                return R.fail("归还库存失败:" + throwable.getMessage());
            }
        };
    }
}
