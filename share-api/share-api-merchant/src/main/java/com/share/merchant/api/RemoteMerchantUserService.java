package com.share.merchant.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.domain.R;
import com.share.merchant.domain.MerchantUser;
import com.share.merchant.factory.RemoteMerchantUserFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商家用户远程服务
 *
 * @author share
 */
@FeignClient(contextId = "remoteMerchantUserService",
        value = ServiceNameConstants.MERCHANT_SERVICE,
        fallbackFactory = RemoteMerchantUserFallbackFactory.class)
public interface RemoteMerchantUserService {

    /**
     * 商家登录校验
     */
    @PostMapping("/inner/merchant/user/login")
    R<MerchantUser> login(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据商家ID获取用户信息
     */
    @GetMapping("/inner/merchant/user/getByMerchantId/{merchantId}")
    R<MerchantUser> getByMerchantId(@PathVariable("merchantId") Long merchantId,
                                    @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
