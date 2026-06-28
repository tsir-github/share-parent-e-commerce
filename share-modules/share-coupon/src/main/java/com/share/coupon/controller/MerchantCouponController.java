package com.share.coupon.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.coupon.constant.CouponStatus;
import com.share.coupon.domain.CouponUser;
import com.share.coupon.domain.CouponTemplate;
import com.share.coupon.service.ICouponTemplateService;
import com.share.coupon.service.ICouponUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家优惠券管理 API
 *
 * @author share
 */
@Tag(name = "商家优惠券")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/merchant/coupon")
public class MerchantCouponController {

    private final ICouponTemplateService couponTemplateService;
    private final ICouponUserService couponUserService;

    @Operation(summary = "商家优惠券模板列表")
    @RequiresLogin
    @GetMapping("/templates")
    public R<List<CouponTemplate>> templates() {
        Long merchantId = SecurityUtils.getUserId();
        return R.ok(couponTemplateService.queryByMerchantId(merchantId));
    }

    @Operation(summary = "新增优惠券模板")
    @RequiresLogin
    @PostMapping("/template")
    public R<Void> addTemplate(@RequestBody CouponTemplate template) {
        template.setMerchantId(SecurityUtils.getUserId());
        template.setStatus(CouponStatus.TEMPLATE_ENABLED);
        couponTemplateService.save(template);
        return R.ok();
    }

    @Operation(summary = "统计用户领取情况")
    @RequiresLogin
    @GetMapping("/stats/{templateId}")
    public R<Map<String, Long>> stats(@PathVariable Long templateId) {
        long total = couponUserService.count(new LambdaQueryWrapper<CouponUser>()
                .eq(CouponUser::getTemplateId, templateId));
        long used = couponUserService.count(new LambdaQueryWrapper<CouponUser>()
                .eq(CouponUser::getTemplateId, templateId)
                .eq(CouponUser::getStatus, CouponStatus.USER_USED));
        long consumed = couponUserService.count(new LambdaQueryWrapper<CouponUser>()
                .eq(CouponUser::getTemplateId, templateId)
                .eq(CouponUser::getStatus, CouponStatus.USER_CONSUMED));
        return R.ok(Map.of("total", total, "locked", used, "consumed", consumed));
    }
}
