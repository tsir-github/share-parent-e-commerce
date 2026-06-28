package com.share.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.goods.domain.SeckillActivity;

import java.util.List;

/**
 * 秒杀活动Mapper接口
 *
 * @author share
 */
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    /**
     * 查询秒杀活动列表（含商品名称）
     */
    List<SeckillActivity> selectSeckillActivityList(SeckillActivity activity);

    /**
     * 查询秒杀活动详情
     */
    SeckillActivity selectSeckillActivityById(Long id);
}
