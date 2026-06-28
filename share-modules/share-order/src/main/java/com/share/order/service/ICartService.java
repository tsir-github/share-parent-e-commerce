package com.share.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.Cart;

import java.util.List;

/**
 * 购物车 Service 接口
 *
 * @author share
 */
public interface ICartService extends IService<Cart> {

    /**
     * 查询用户购物车列表
     */
    List<Cart> selectCartListByUserId(Long userId);

    /**
     * 添加商品到购物车（已存在则累加数量）
     */
    void addToCart(Long userId, Long productId, Long skuId, Integer quantity);

    /**
     * 更新购物车项数量
     */
    void updateQuantity(Long id, Long userId, Integer quantity);

    /**
     * 切换选中状态
     */
    void toggleChecked(Long id, Long userId);
}
