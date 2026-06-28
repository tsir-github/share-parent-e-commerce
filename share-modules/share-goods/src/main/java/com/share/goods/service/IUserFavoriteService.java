package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.domain.UserFavorite;

import java.util.List;
import java.util.Map;

/**
 * 用户商品收藏 Service 接口
 *
 * @author share
 */
public interface IUserFavoriteService extends IService<UserFavorite> {

    /**
     * 切换收藏状态（已收藏则取消，未收藏则添加）
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return true=已收藏, false=已取消
     */
    boolean toggle(Long userId, Long productId);

    /**
     * 查询用户是否已收藏
     */
    boolean isFavorited(Long userId, Long productId);

    /**
     * 查询用户收藏列表（含商品信息）
     */
    List<Map<String, Object>> selectUserFavorites(Long userId);
}
