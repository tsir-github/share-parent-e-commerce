package com.share.goods.service;

import com.share.goods.domain.dto.SeckillOrderRequestDTO;
import com.share.goods.domain.vo.SeckillActivityVO;
import com.share.goods.domain.vo.SeckillDetailVO;

import java.util.List;

/**
 * C端秒杀 Service 接口
 *
 * <p>非实体 CRUD 工具类，不继承 {@code IService}。
 * 职责是 C 端秒杀业务（列表/详情/参与秒杀），不是单一实体的增删改查。</p>
 *
 * @author share
 */
public interface ISeckillApiService {

    /** 秒杀活动列表 */
    List<SeckillActivityVO> getSeckillList();

    /** 秒杀活动详情 */
    SeckillDetailVO getSeckillDetail(Long activityId, Long userId);

    /** 参与秒杀 */
    String createSeckillOrder(Long activityId, Long userId, SeckillOrderRequestDTO dto);

    /** 归还秒杀库存（Feign 内部调用） */
    void releaseStock(Long activityId, int quantity);

    /** 根据订单号归还秒杀库存（超时取消时调用） */
    void releaseStockByOrderNo(String orderNo);
}
