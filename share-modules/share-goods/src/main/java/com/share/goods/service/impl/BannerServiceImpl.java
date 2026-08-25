package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.goods.domain.Banner;
import com.share.goods.mapper.BannerMapper;
import com.share.goods.service.IBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Banner Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements IBannerService {

    private final BannerMapper bannerMapper;

    @Override
    public List<Banner> selectEnabledBanners() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, "1")
                .eq(Banner::getDelFlag, "0")
                .orderByAsc(Banner::getSort));
    }

    @Override
    public int updateStatus(Long id, String status) {
        return bannerMapper.update(null, new LambdaUpdateWrapper<Banner>()
                .eq(Banner::getId, id)
                .set(Banner::getStatus, status));
    }
}
