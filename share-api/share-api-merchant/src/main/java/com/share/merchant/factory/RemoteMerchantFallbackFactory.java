package com.share.merchant.factory;

import com.share.common.core.domain.R;
import com.share.merchant.api.RemoteMerchantService;
import com.share.merchant.domain.MerchantInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商家信息服务降级处理
 *
 * @author share
 */
@Component
public class RemoteMerchantFallbackFactory implements FallbackFactory<RemoteMerchantService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteMerchantFallbackFactory.class);

    @Override
    public RemoteMerchantService create(Throwable throwable) {
        log.error("商家信息服务调用失败:{}", throwable.getMessage());
        return new RemoteMerchantService() {
            @Override
            public R<MerchantInfo> get(Long merchantId, String source) {
                return R.fail("获取商家信息失败:" + throwable.getMessage());
            }
        };
    }
}
