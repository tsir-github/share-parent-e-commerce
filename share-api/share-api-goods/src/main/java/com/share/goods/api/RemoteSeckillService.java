package com.share.goods.api;

import com.share.common.core.domain.R;
import com.share.common.core.constant.SecurityConstants;
import com.share.goods.factory.RemoteSeckillFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 秒杀服务 Feign 接口
 *
 * @author share
 */
@FeignClient(contextId = "remoteSeckillService",
        value = "share-goods",
        fallbackFactory = RemoteSeckillFallbackFactory.class)
public interface RemoteSeckillService {

    @PostMapping("/inner/seckill/releaseStock/{activityId}")
    R<Void> releaseStock(@PathVariable("activityId") Long activityId,
                         @RequestParam("quantity") int quantity,
                         @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PostMapping("/inner/seckill/releaseStockByOrderNo/{orderNo}")
    R<Void> releaseStockByOrderNo(@PathVariable("orderNo") String orderNo,
                                   @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
