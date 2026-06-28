package com.share.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.coupon.constant.CouponStatus;
import com.share.coupon.domain.CouponTemplate;
import com.share.coupon.mapper.CouponTemplateMapper;
import com.share.coupon.service.ICouponTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 优惠券模板Service实现
 *
 * @author share
 */
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements ICouponTemplateService {

    /** 默认平台自营商户ID */
    private static final Long DEFAULT_MERCHANT_ID = 1L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(CouponTemplate entity) {
        if (entity.getMerchantId() == null) {
            entity.setMerchantId(DEFAULT_MERCHANT_ID);
        }
        return super.save(entity);
    }

    @Override
    public List<CouponTemplate> queryAvailableList() {
        Date now = new Date();
        return baseMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, CouponStatus.TEMPLATE_ENABLED)
                .eq(CouponTemplate::getDelFlag, "0")
                .le(CouponTemplate::getStartTime, now)
                .ge(CouponTemplate::getEndTime, now)
                .ne(CouponTemplate::getRemainCount, 0)
                .orderByDesc(CouponTemplate::getCreateTime));
    }

    @Override
    public List<CouponTemplate> queryByMerchantId(Long merchantId) {
        return baseMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getMerchantId, merchantId)
                .eq(CouponTemplate::getDelFlag, "0")
                .orderByDesc(CouponTemplate::getCreateTime));
    }
}
