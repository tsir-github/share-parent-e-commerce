package com.share.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.order.domain.Cart;
import com.share.order.mapper.CartMapper;
import com.share.order.service.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 购物车 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    @Override
    public List<Cart> selectCartListByUserId(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.orderByDesc(Cart::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long userId, Long productId, Long skuId, Integer quantity) {
        // 已存在则累加数量
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .eq(Cart::getProductId, productId)
               .eq(Cart::getSkuId, skuId);
        Cart existing = baseMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            baseMapper.updateById(existing);
            return;
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setSkuId(skuId);
        cart.setQuantity(quantity);
        cart.setChecked(true);
        baseMapper.insert(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long id, Long userId, Integer quantity) {
        Cart cart = baseMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new ServiceException("购物车项不存在");
        }
        if (quantity <= 0) {
            baseMapper.deleteById(id);
            return;
        }
        cart.setQuantity(quantity);
        baseMapper.updateById(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleChecked(Long id, Long userId) {
        Cart cart = baseMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new ServiceException("购物车项不存在");
        }
        cart.setChecked(!Boolean.TRUE.equals(cart.getChecked()));
        baseMapper.updateById(cart);
    }
}
