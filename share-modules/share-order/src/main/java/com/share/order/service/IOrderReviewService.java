package com.share.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.OrderReview;
import com.share.order.domain.vo.OrderReviewVO;
import com.share.order.domain.vo.PendingReviewVO;
import com.share.order.domain.vo.ReviewStatsDTO;

import java.util.List;
import java.util.Map;

/**
 * 订单评价 Service 接口
 *
 * @author share
 */
public interface IOrderReviewService extends IService<OrderReview> {

    /**
     * 提交评价
     *
     * @param orderItemId 订单项ID
     * @param userId      用户ID
     * @param rating      评分（1-5）
     * @param content     评价内容
     * @return 评价ID
     */
    Long submitReview(Long orderItemId, Long userId, Integer rating, String content);

    /**
     * 查询订单项评价
     */
    OrderReviewVO getReviewByOrderItemId(Long orderItemId);

    /**
     * 查询用户待评价的订单项列表
     */
    List<PendingReviewVO> getPendingReviews(Long userId);

    /**
     * 查询商品评价列表
     */
    List<OrderReviewVO> getProductReviews(Long productId);

    /**
     * 查询单个商品评价统计
     */
    ReviewStatsDTO getReviewStats(Long productId);

    /**
     * 批量查询商品评价统计
     */
    Map<Long, ReviewStatsDTO> getReviewStatsBatch(List<Long> productIds);
}
