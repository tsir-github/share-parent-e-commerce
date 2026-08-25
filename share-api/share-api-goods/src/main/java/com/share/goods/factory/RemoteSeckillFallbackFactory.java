package com.share.goods.factory;

import com.share.common.core.domain.R;
import com.share.goods.api.RemoteSeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 秒杀服务降级处理
 *
 * @author share
 */
@Component
public class RemoteSeckillFallbackFactory implements FallbackFactory<RemoteSeckillService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteSeckillFallbackFactory.class);

    @Override
    public RemoteSeckillService create(Throwable throwable) {
        log.error("秒杀服务调用失败:{}", throwable.getMessage());
        return new RemoteSeckillService() {
            @Override
            public R<Void> releaseStock(Long activityId, int quantity, String source) {
                return R.fail("归还秒杀库存失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> releaseStockByOrderNo(String orderNo, String source) {
                return R.fail("归还秒杀库存失败:" + throwable.getMessage());
            }
        };
    }
}
