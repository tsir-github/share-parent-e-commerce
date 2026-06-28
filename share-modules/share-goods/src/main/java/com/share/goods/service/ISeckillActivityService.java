package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.domain.SeckillActivity;

import java.util.List;

/**
 * 秒杀活动Service接口
 *
 * @author share
 */
public interface ISeckillActivityService extends IService<SeckillActivity> {

    /**
     * 查询秒杀活动列表
     */
    List<SeckillActivity> selectSeckillActivityList(SeckillActivity activity);

    /**
     * 查询秒杀活动详情
     */
    SeckillActivity selectSeckillActivityById(Long id);

    /**
     * 新增秒杀活动
     */
    int insertSeckillActivity(SeckillActivity activity);

    /**
     * 修改秒杀活动
     */
    int updateSeckillActivity(SeckillActivity activity);

    /**
     * 修改秒杀活动状态
     */
    int updateStatus(Long id, String status);

    /**
     * 批量删除秒杀活动（逻辑删除）
     */
    int deleteSeckillActivityByIds(Long[] ids);

    /**
     * 校验时间冲突
     */
    void checkTimeConflict(SeckillActivity activity);
}
