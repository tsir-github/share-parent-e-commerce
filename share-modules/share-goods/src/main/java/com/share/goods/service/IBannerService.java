package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.domain.Banner;

import java.util.List;

/**
 * Banner Service 接口
 *
 * @author share
 */
public interface IBannerService extends IService<Banner> {

    /**
     * 查询启用中的 Banner 列表（按排序升序）
     */
    List<Banner> selectEnabledBanners();

    /**
     * 修改 Banner 状态
     */
    int updateStatus(Long id, String status);
}
