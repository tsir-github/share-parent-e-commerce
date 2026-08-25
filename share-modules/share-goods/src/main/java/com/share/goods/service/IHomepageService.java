package com.share.goods.service;

import com.share.goods.domain.vo.HomepageVO;

/**
 * 首页数据 Service 接口
 *
 * <p>非实体 CRUD，不继承 IService。职责是聚合首页各模块数据。</p>
 *
 * @author share
 */
public interface IHomepageService {

    /**
     * 获取首页聚合数据（含 Redis 缓存）
     */
    HomepageVO getHomepage();
}
