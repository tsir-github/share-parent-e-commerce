package com.share.order.service;

import com.share.order.domain.vo.ReviewStatsDTO;

/**
 * 评价统计缓存服务接口
 *
 * @author share
 */
public interface IOrderReviewCacheService {

    ReviewStatsDTO getStats(Long productId);

    void setStats(Long productId, ReviewStatsDTO stats);

    void evictStats(Long productId);
}
