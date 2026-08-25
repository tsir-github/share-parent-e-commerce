package com.share.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.OrderStatus;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.OrderItem;
import com.share.order.domain.OrderReview;
import com.share.order.domain.ReviewImage;
import com.share.order.domain.vo.OrderReviewVO;
import com.share.order.domain.vo.PendingReviewVO;
import com.share.order.domain.vo.ReviewStatsDTO;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.mapper.OrderItemMapper;
import com.share.order.mapper.OrderReviewMapper;
import com.share.order.mapper.ReviewImageMapper;
import com.share.order.service.IOrderReviewCacheService;
import com.share.order.service.IOrderReviewService;
import com.share.user.api.RemoteUserService;
import com.share.user.domain.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单评价 Service 实现
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReviewServiceImpl extends ServiceImpl<OrderReviewMapper, OrderReview> implements IOrderReviewService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final RemoteUserService remoteUserService;
    private final IOrderReviewCacheService reviewCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitReview(Long orderItemId, Long userId, Integer rating, String content) {
        // 校验评分
        if (rating == null || rating < 1 || rating > 5) {
            throw new ServiceException("评分必须在 1-5 之间");
        }

        // 校验订单项存在
        OrderItem orderItem = orderItemMapper.selectById(orderItemId);
        if (orderItem == null) {
            throw new ServiceException("订单项不存在");
        }

        // 校验订单已完成+已确认
        OrderInfo order = orderInfoMapper.selectById(orderItem.getOrderId());
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new ServiceException("订单未完成，不可评价");
        }

        // 校验是否已评价（幂等）
        Long existingId = baseMapper.selectCount(
                new LambdaQueryWrapper<OrderReview>()
                        .eq(OrderReview::getOrderItemId, orderItemId)
                        .eq(OrderReview::getDelFlag, "0"));
        if (existingId > 0) {
            throw new ServiceException("该订单项已评价");
        }

        // 获取用户信息（快照）
        String nickname = "";
        String avatar = "";
        try {
            R<UserInfo> userInfoR = remoteUserService.getInfo(userId);
            if (userInfoR != null && userInfoR.getData() != null) {
                nickname = userInfoR.getData().getNickname();
                avatar = userInfoR.getData().getAvatarUrl();
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败，使用默认值: userId={}", userId, e);
        }

        // 创建评价
        OrderReview review = new OrderReview();
        review.setOrderId(orderItem.getOrderId());
        review.setOrderItemId(orderItemId);
        review.setUserId(userId);
        review.setNickname(nickname);
        review.setAvatar(avatar);
        review.setRating(rating);
        review.setContent(content != null ? content : "");
        review.setStatus("0");
        review.setCreateBy(String.valueOf(userId));
        baseMapper.insert(review);

        // 关联已有晒图（同一 order_item_id 的未关联图片）
        reviewImageMapper.update(null,
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaUpdate(ReviewImage.class)
                        .set(ReviewImage::getReviewId, review.getId())
                        .eq(ReviewImage::getOrderItemId, orderItemId)
                        .isNull(ReviewImage::getReviewId));

        // 失效评价统计缓存
        reviewCacheService.evictStats(orderItem.getProductId());

        return review.getId();
    }

    @Override
    public OrderReviewVO getReviewByOrderItemId(Long orderItemId) {
        OrderReview review = baseMapper.selectOne(
                new LambdaQueryWrapper<OrderReview>()
                        .eq(OrderReview::getOrderItemId, orderItemId)
                        .eq(OrderReview::getDelFlag, "0"));
        if (review == null) {
            return null;
        }
        return toReviewVO(review);
    }

    @Override
    public List<PendingReviewVO> getPendingReviews(Long userId) {
        // 查询当前用户所有已完成+已确认的订单
        List<OrderInfo> completedOrders = orderInfoMapper.selectList(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getUserId, userId)
                        .eq(OrderInfo::getStatus, OrderStatus.COMPLETED)
                        .eq(OrderInfo::getDelFlag, "0")
                        .orderByDesc(OrderInfo::getCreateTime));

        if (completedOrders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> orderIds = completedOrders.stream().map(OrderInfo::getId).collect(Collectors.toSet());
        Map<Long, OrderInfo> orderMap = completedOrders.stream().collect(Collectors.toMap(OrderInfo::getId, o -> o));

        // 查询这些订单的所有订单项
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
                        .eq(OrderItem::getDelFlag, "0"));

        if (allItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询已评价的订单项ID
        Set<Long> reviewedItemIds = baseMapper.selectList(
                        new LambdaQueryWrapper<OrderReview>()
                                .in(OrderReview::getOrderItemId,
                                        allItems.stream().map(OrderItem::getId).collect(Collectors.toList()))
                                .eq(OrderReview::getDelFlag, "0"))
                .stream().map(OrderReview::getOrderItemId).collect(Collectors.toSet());

        // 过滤出未评价的订单项
        List<PendingReviewVO> result = new ArrayList<>();
        for (OrderItem item : allItems) {
            if (reviewedItemIds.contains(item.getId())) {
                continue;
            }
            OrderInfo order = orderMap.get(item.getOrderId());
            if (order == null) continue;

            PendingReviewVO vo = new PendingReviewVO();
            vo.setOrderItemId(item.getId());
            vo.setOrderId(item.getOrderId());
            vo.setOrderNo(order.getOrderNo());
            vo.setProductId(item.getProductId());
            vo.setProductName(item.getProductName());
            vo.setProductImage(item.getProductImage());
            vo.setPrice(item.getPrice());
            vo.setQuantity(item.getQuantity());
            vo.setSkuSpecs(item.getSkuSpecs());
            vo.setReceiveTime(order.getReceiveTime());
            result.add(vo);
        }

        return result;
    }

    @Override
    public List<OrderReviewVO> getProductReviews(Long productId) {
        // 查出该商品所有订单项的评价
        List<OrderReview> reviews = baseMapper.selectList(
                new LambdaQueryWrapper<OrderReview>()
                        .eq(OrderReview::getDelFlag, "0")
                        .eq(OrderReview::getStatus, "0")
                        .inSql(OrderReview::getOrderItemId,
                                "SELECT id FROM order_item WHERE product_id = " + productId)
                        .orderByDesc(OrderReview::getCreateTime));

        return reviews.stream().map(this::toReviewVO).collect(Collectors.toList());
    }

    @Override
    public ReviewStatsDTO getReviewStats(Long productId) {
        // 先查缓存
        ReviewStatsDTO cached = reviewCacheService.getStats(productId);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> stats = baseMapper.selectReviewStatsByProductId(productId);
        ReviewStatsDTO dto;
        if (stats == null || stats.isEmpty()) {
            dto = new ReviewStatsDTO(0.0, 0);
        } else {
            Double avgRating = stats.get("avgRating") != null ? ((Number) stats.get("avgRating")).doubleValue() : 0.0;
            Integer reviewCount = stats.get("reviewCount") != null ? ((Number) stats.get("reviewCount")).intValue() : 0;
            dto = new ReviewStatsDTO(avgRating, reviewCount);
        }
        reviewCacheService.setStats(productId, dto);
        return dto;
    }

    @Override
    public Map<Long, ReviewStatsDTO> getReviewStatsBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> statsList = baseMapper.selectReviewStatsByProductIds(productIds);
        Map<Long, ReviewStatsDTO> result = new HashMap<>();
        // 初始化所有ID为0
        for (Long pid : productIds) {
            result.put(pid, new ReviewStatsDTO(0.0, 0));
        }
        for (Map<String, Object> row : statsList) {
            Long productId = ((Number) row.get("productId")).longValue();
            Double avgRating = row.get("avgRating") != null ? ((Number) row.get("avgRating")).doubleValue() : 0.0;
            Integer reviewCount = row.get("reviewCount") != null ? ((Number) row.get("reviewCount")).intValue() : 0;
            result.put(productId, new ReviewStatsDTO(avgRating, reviewCount));
        }
        return result;
    }

    private OrderReviewVO toReviewVO(OrderReview review) {
        OrderReviewVO vo = new OrderReviewVO();
        vo.setId(review.getId());
        vo.setOrderItemId(review.getOrderItemId());
        vo.setUserId(review.getUserId());
        vo.setNickname(review.getNickname());
        vo.setAvatar(review.getAvatar());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setCreateTime(review.getCreateTime());

        // 查询晒图
        List<ReviewImage> images = reviewImageMapper.selectList(
                new LambdaQueryWrapper<ReviewImage>()
                        .eq(ReviewImage::getReviewId, review.getId())
                        .eq(ReviewImage::getDelFlag, "0")
                        .orderByAsc(ReviewImage::getSortOrder));
        vo.setImages(images.stream().map(ReviewImage::getImageUrl).collect(Collectors.toList()));

        return vo;
    }
}
