package com.share.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.goods.domain.SeckillOrder;
import org.apache.ibatis.annotations.Param;

/**
 * 秒杀订单记录 Mapper
 *
 * @author share
 */
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    /**
     * 查询用户在某秒杀活动中的已购数量
     */
    Integer countByUserAndActivity(@Param("activityId") Long activityId,
                                   @Param("userId") Long userId);
}
