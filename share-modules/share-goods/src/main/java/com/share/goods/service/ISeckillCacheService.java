package com.share.goods.service;

import com.share.goods.domain.SeckillActivity;
import com.share.goods.domain.vo.SeckillActivityVO;

import java.util.List;

/**
 * 秒杀活动缓存服务接口
 *
 * @author share
 */
public interface ISeckillCacheService {

    List<SeckillActivityVO> getSeckillList();

    void setSeckillList(List<SeckillActivityVO> list);

    SeckillActivity getSeckillDetail(Long activityId);

    void setSeckillDetail(Long activityId, SeckillActivity activity);

    void evictList();

    void evictDetail(Long activityId);
}
