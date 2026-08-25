package com.share.merchant.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.domain.R;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.factory.RemoteMerchantFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 商家信息远程服务
 */
@FeignClient(contextId = "remoteMerchantService",
        value = ServiceNameConstants.MERCHANT_SERVICE,
        fallbackFactory = RemoteMerchantFallbackFactory.class)
public interface RemoteMerchantService {

    @GetMapping("/inner/merchant/get/{merchantId}")
    R<MerchantInfo> get(@PathVariable("merchantId") Long merchantId,
                        @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
