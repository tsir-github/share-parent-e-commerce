package com.share.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.coupon.domain.CouponTemplate;

import java.util.List;

/**
 * 优惠券模板 Service 接口
 *
 * @author share
 */
public interface ICouponTemplateService extends IService<CouponTemplate> {

    /**
     * 查询可领取优惠券模板列表（status=1 且在有效期内）
     */
    List<CouponTemplate> queryAvailableList();

    /**
     * 查询商家的优惠券模板列表
     */
    List<CouponTemplate> queryByMerchantId(Long merchantId);
}
