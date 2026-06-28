package com.share.merchant.factory;

import com.share.common.core.domain.R;
import com.share.merchant.api.RemoteMerchantUserService;
import com.share.merchant.domain.MerchantUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商家用户服务降级处理
 *
 * @author share
 */
@Component
public class RemoteMerchantUserFallbackFactory implements FallbackFactory<RemoteMerchantUserService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteMerchantUserFallbackFactory.class);

    @Override
    public RemoteMerchantUserService create(Throwable throwable) {
        log.error("商家用户服务调用失败:{}", throwable.getMessage());
        return new RemoteMerchantUserService() {
            @Override
            public R<MerchantUser> login(String username, String password, String source) {
                return R.fail("商家登录失败:" + throwable.getMessage());
            }

            @Override
            public R<MerchantUser> getByMerchantId(Long merchantId, String source) {
                return R.fail("获取商家用户信息失败:" + throwable.getMessage());
            }
        };
    }
}
