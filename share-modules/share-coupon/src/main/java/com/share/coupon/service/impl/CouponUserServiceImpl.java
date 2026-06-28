package com.share.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.coupon.constant.CouponStatus;
import com.share.coupon.domain.CouponTemplate;
import com.share.coupon.domain.CouponUser;
import com.share.coupon.domain.vo.MyCouponVO;
import com.share.coupon.domain.vo.UsableCouponVO;
import com.share.coupon.mapper.CouponTemplateMapper;
import com.share.coupon.mapper.CouponUserMapper;
import com.share.coupon.service.ICouponUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * 用户领取记录Service实现
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements ICouponUserService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimCoupon(Long userId, Long templateId) {
        // 1. 先做一次无锁前置校验（快速失败，减轻锁压力）
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || !CouponStatus.TEMPLATE_ENABLED.equals(template.getStatus())) {
            throw new ServiceException("优惠券不存在或未启用");
        }

        String lockKey = "coupon:claim:" + templateId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new ServiceException("操作太频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("系统繁忙，请重试");
        }

        try {
            // ---- 锁内：重新查询最新数据 ----
            CouponTemplate lockedTemplate = couponTemplateMapper.selectById(templateId);

            // 2. 校验有效期
            Date now = new Date();
            if (lockedTemplate.getStartTime() != null && now.before(lockedTemplate.getStartTime())) {
                throw new ServiceException("优惠券尚未到领取时间");
            }
            if (lockedTemplate.getEndTime() != null && now.after(lockedTemplate.getEndTime())) {
                throw new ServiceException("优惠券已过有效期");
            }

            // 3. 校验发行量 — remainCount == -1 表示不限
            if (lockedTemplate.getRemainCount() != null
                    && lockedTemplate.getRemainCount() != -1
                    && lockedTemplate.getRemainCount() <= 0) {
                throw new ServiceException("优惠券已领完");
            }

            // 4. 校验每人限领
            if (lockedTemplate.getLimitPerUser() != null && lockedTemplate.getLimitPerUser() > 0) {
                long userClaimed = baseMapper.selectCount(new LambdaQueryWrapper<CouponUser>()
                        .eq(CouponUser::getUserId, userId)
                        .eq(CouponUser::getTemplateId, templateId));
                if (userClaimed >= lockedTemplate.getLimitPerUser()) {
                    throw new ServiceException("已达到领取上限");
                }
            }

            // 5. 乐观锁扣减 remainCount（不限量时跳过）
            if (lockedTemplate.getRemainCount() != null && lockedTemplate.getRemainCount() != -1) {
                int updated = couponTemplateMapper.update(null,
                        new LambdaUpdateWrapper<CouponTemplate>()
                                .eq(CouponTemplate::getId, templateId)
                                .eq(CouponTemplate::getVersion, lockedTemplate.getVersion())
                                .set(CouponTemplate::getRemainCount, lockedTemplate.getRemainCount() - 1)
                                .set(CouponTemplate::getVersion, lockedTemplate.getVersion() + 1));
                if (updated == 0) {
                    throw new ServiceException("领取失败，请重试");
                }
            }

            // 6. 插入领取记录
            CouponUser record = CouponUser.builder()
                    .userId(userId)
                    .templateId(templateId)
                    .status(CouponStatus.USER_UNUSED) // 未使用
                    .build();
            baseMapper.insert(record);

            log.info("用户领取优惠券成功: userId={}, templateId={}", userId, templateId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockForOrder(Long userId, Long couponUserId, String orderNo) {
        String lockKey = "coupon:lock:" + couponUserId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new ServiceException("操作太频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("系统繁忙，请重试");
        }

        try {
            CouponUser couponUser = baseMapper.selectById(couponUserId);
            if (couponUser == null || !CouponStatus.USER_UNUSED.equals(couponUser.getStatus())) {
                throw new ServiceException("优惠券不可用");
            }
            if (!couponUser.getUserId().equals(userId)) {
                throw new ServiceException("无权操作此优惠券");
            }

            baseMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                    .eq(CouponUser::getId, couponUserId)
                    .eq(CouponUser::getStatus, CouponStatus.USER_UNUSED)
                    .set(CouponUser::getStatus, CouponStatus.USER_USED)
                    .set(CouponUser::getOrderNo, orderNo));

            log.info("优惠券已锁定: couponUserId={}, orderNo={}", couponUserId, orderNo);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseForOrder(Long couponUserId, String orderNo) {
        CouponUser couponUser = baseMapper.selectById(couponUserId);
        if (couponUser == null) {
            log.warn("释放优惠券不存在: id={}", couponUserId);
            return;
        }
        if (!orderNo.equals(couponUser.getOrderNo())) {
            log.warn("释放优惠券订单不匹配: couponUserId={}, orderNo={}", couponUserId, orderNo);
            return;
        }

        baseMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                .eq(CouponUser::getId, couponUserId)
                .eq(CouponUser::getStatus, CouponStatus.USER_USED)
                .set(CouponUser::getStatus, CouponStatus.USER_UNUSED)
                .set(CouponUser::getOrderNo, null)
                .set(CouponUser::getUsedTime, null));

        log.info("优惠券已释放: couponUserId={}, orderNo={}", couponUserId, orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseByOrderNo(String orderNo) {
        List<CouponUser> list = baseMapper.selectList(
                new LambdaQueryWrapper<CouponUser>().eq(CouponUser::getOrderNo, orderNo));
        for (CouponUser cu : list) {
            releaseForOrder(cu.getId(), orderNo);
        }
    }

    @Override
    public long countAvailableByUserId(Long userId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<CouponUser>()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getStatus, CouponStatus.USER_UNUSED));
    }

    @Override
    public List<MyCouponVO> queryMyCoupons(Long userId, String status) {
        List<Map<String, Object>> rows = baseMapper.selectMyCouponList(userId, status);
        Date now = new Date();
        return rows.stream().map(row -> {
            Date endTime = (Date) row.get("endTime");
            String st = (String) row.get("status");
            if (CouponStatus.USER_UNUSED.equals(st) && endTime != null && endTime.before(now)) {
                st = CouponStatus.USER_EXPIRED;
            }
            return MyCouponVO.builder()
                    .id(toLong(row.get("id")))
                    .templateId(toLong(row.get("templateId")))
                    .name((String) row.get("name"))
                    .type((String) row.get("type"))
                    .conditionAmt(toBigDecimal(row.get("conditionAmt")))
                    .discountAmt(toBigDecimal(row.get("discountAmt")))
                    .discountRate(toBigDecimal(row.get("discountRate")))
                    .status(st)
                    .startTime((Date) row.get("startTime"))
                    .endTime(endTime)
                    .usedTime((Date) row.get("usedTime"))
                    .createTime((Date) row.get("createTime"))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<UsableCouponVO> queryUsableCoupons(Long userId, BigDecimal amount) {
        List<Map<String, Object>> rows = baseMapper.selectUsableCouponList(userId);
        return rows.stream()
                .map(row -> {
                    String type = (String) row.get("type");
                    BigDecimal conditionAmt = toBigDecimal(row.get("conditionAmt"));
                    BigDecimal discountAmt = toBigDecimal(row.get("discountAmt"));
                    BigDecimal discountRate = toBigDecimal(row.get("discountRate"));
                    BigDecimal discount = BigDecimal.ZERO;

                    if ("0".equals(type)) {
                        if (conditionAmt != null && amount.compareTo(conditionAmt) < 0) return null;
                        discount = discountAmt != null ? discountAmt : BigDecimal.ZERO;
                    } else if ("1".equals(type)) {
                        if (conditionAmt != null && amount.compareTo(conditionAmt) < 0) return null;
                        if (discountRate != null) {
                            discount = amount.multiply(BigDecimal.ONE.subtract(discountRate))
                                    .setScale(2, RoundingMode.HALF_UP);
                        }
                    } else {
                        discount = discountAmt != null ? discountAmt : BigDecimal.ZERO;
                    }

                    BigDecimal finalDiscount = discount;
                    return UsableCouponVO.builder()
                            .id(toLong(row.get("id")))
                            .templateId(toLong(row.get("templateId")))
                            .name((String) row.get("name"))
                            .type(type)
                            .conditionAmt(conditionAmt)
                            .discountAmt(discountAmt)
                            .discountRate(discountRate)
                            .discount(finalDiscount)
                            .endTime((Date) row.get("endTime"))
                            .build();
                })
                .filter(v -> v != null)
                .sorted((a, b) -> b.getDiscount().compareTo(a.getDiscount()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeCoupon(Long couponUserId) {
        CouponUser couponUser = baseMapper.selectById(couponUserId);
        if (couponUser == null) {
            log.warn("消费优惠券不存在: id={}", couponUserId);
            return;
        }
        baseMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                .eq(CouponUser::getId, couponUserId)
                .eq(CouponUser::getStatus, CouponStatus.USER_USED)
                .set(CouponUser::getStatus, CouponStatus.USER_CONSUMED)
                .set(CouponUser::getUsedTime, new Date())
                .set(CouponUser::getOrderNo, null));
        log.info("优惠券已消费: couponUserId={}", couponUserId);
    }

    // --- 辅助方法 ---

    private static Long toLong(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        return null;
    }
}
