package com.share.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.coupon.domain.CouponUser;
import com.share.coupon.domain.vo.MyCouponVO;
import com.share.coupon.domain.vo.UsableCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户领取记录Service接口
 *
 * @author share
 */
public interface ICouponUserService extends IService<CouponUser> {

    /**
     * 根据订单号释放所有关联优惠券（取消订单回退）
     *
     * @param orderNo 订单号
     */
    void releaseByOrderNo(String orderNo);

    /**
     * 用户领取优惠券（基于模板）
     *
     * @param userId     用户ID
     * @param templateId 模板ID
     */
    void claimCoupon(Long userId, Long templateId);

    /**
     * 锁定优惠券（下单时占用）
     *
     * @param userId      用户ID
     * @param couponUserId 用户领取记录ID
     * @param orderNo     订单号
     */
    void lockForOrder(Long userId, Long couponUserId, String orderNo);

    /**
     * 释放优惠券（取消订单时回退）
     *
     * @param couponUserId 用户领取记录ID
     * @param orderNo      订单号
     */
    void releaseForOrder(Long couponUserId, String orderNo);

    /**
     * 查询用户可用优惠券数量
     *
     * @param userId 用户ID
     * @return 可用数量
     */
    long countAvailableByUserId(Long userId);

    /**
     * 查询我的优惠券列表
     *
     * @param userId 用户ID
     * @param status 状态筛选（可为空）
     * @return 优惠券列表
     */
    List<MyCouponVO> queryMyCoupons(Long userId, String status);

    /**
     * 查询下单可用优惠券
     *
     * @param userId 用户ID
     * @param amount 订单金额
     * @return 可用优惠券列表（按优惠力度降序）
     */
    List<UsableCouponVO> queryUsableCoupons(Long userId, BigDecimal amount);

    /**
     * 消费优惠券（订单完成后标记已使用）
     *
     * @param couponUserId 用户领取记录ID
     */
    void consumeCoupon(Long couponUserId);
}
