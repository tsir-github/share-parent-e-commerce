package com.share.merchant.service;

import com.share.merchant.domain.MerchantInfo;

/**
 * 商家信息缓存服务接口
 *
 * @author share
 */
public interface IMerchantCacheService {

    MerchantInfo getMerchant(Long merchantId);

    void setMerchant(Long merchantId, MerchantInfo merchant);

    void evictMerchant(Long merchantId);
}
