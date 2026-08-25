package com.share.coupon.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.coupon.domain.dto.UsableCouponDTO;
import com.share.coupon.domain.vo.AvailableCouponVO;
import com.share.coupon.domain.vo.MyCouponVO;
import com.share.coupon.domain.vo.UsableCouponVO;
import com.share.coupon.mapper.CouponUserMapper;
import com.share.coupon.service.ICouponTemplateService;
import com.share.coupon.service.ICouponUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * C端业主优惠券 API
 *
 * @author share
 */
@Tag(name = "C端优惠券")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/coupon")
public class CouponApiController {

    private final ICouponUserService couponUserService;
    private final ICouponTemplateService couponTemplateService;
    private final CouponUserMapper couponUserMapper;

    @Operation(summary = "可领取优惠券列表")
    @RequiresLogin
    @GetMapping("/available")
    public R<List<AvailableCouponVO>> available() {
        Long userId = SecurityUtils.getUserId();
        List<com.share.coupon.domain.CouponTemplate> templates = couponTemplateService.queryAvailableList();
        // 批量查询已领数量，替代 N+1 循环 COUNT
        List<Long> templateIds = templates.stream().map(t -> t.getId()).collect(Collectors.toList());
        Map<Long, Integer> claimedMap = new java.util.HashMap<>();
        if (!templateIds.isEmpty()) {
            for (Map<String, Object> row : couponUserMapper.countClaimedByUserAndTemplates(userId, templateIds)) {
                Long tid = ((Number) row.get("templateId")).longValue();
                Integer cnt = ((Number) row.get("cnt")).intValue();
                claimedMap.put(tid, cnt);
            }
        }
        List<AvailableCouponVO> list = templates.stream().map(t ->
                AvailableCouponVO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .type(t.getType())
                        .conditionAmt(t.getConditionAmt())
                        .discountAmt(t.getDiscountAmt())
                        .discountRate(t.getDiscountRate())
                        .startTime(t.getStartTime())
                        .endTime(t.getEndTime())
                        .remainCount(t.getRemainCount())
                        .totalCount(t.getTotalCount())
                        .limitPerUser(t.getLimitPerUser())
                        .claimedCount(claimedMap.getOrDefault(t.getId(), 0))
                        .build()
        ).collect(Collectors.toList());
        return R.ok(list);
    }

    @Operation(summary = "领取优惠券")
    @RequiresLogin
    @PostMapping("/claim/{templateId}")
    public R<Void> claim(@PathVariable Long templateId) {
        couponUserService.claimCoupon(SecurityUtils.getUserId(), templateId);
        return R.ok();
    }

    @Operation(summary = "我的优惠券列表")
    @RequiresLogin
    @GetMapping("/my")
    public R<PageInfo<MyCouponVO>> myCoupons(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        PageHelper.startPage(pageNum, pageSize);
        List<MyCouponVO> list = couponUserService.queryMyCoupons(SecurityUtils.getUserId(), status);
        PageInfo<MyCouponVO> pageInfo = new PageInfo<>(list);
        return R.ok(pageInfo);
    }

    @Operation(summary = "下单可用优惠券")
    @RequiresLogin
    @GetMapping("/usable")
    public R<List<UsableCouponVO>> usable(@Valid UsableCouponDTO dto) {
        return R.ok(couponUserService.queryUsableCoupons(SecurityUtils.getUserId(), dto.getAmount()));
    }
}
