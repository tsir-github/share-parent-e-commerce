package com.share.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.merchant.domain.MerchantFavorite;

/**
 * 商家收藏 Service 接口
 *
 * @author share
 */
public interface IMerchantFavoriteService extends IService<MerchantFavorite> {

    /** 切换收藏状态 */
    boolean toggle(Long userId, Long merchantId);

    /** 是否已收藏 */
    boolean isFavorited(Long userId, Long merchantId);

    /** 商家收藏数 */
    long countByMerchantId(Long merchantId);

    /** 用户收藏列表（含商家信息） */
    java.util.List<java.util.Map<String, Object>> selectUserFavorites(Long userId);
}
