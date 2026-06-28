package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.goods.domain.Product;
import com.share.goods.domain.UserFavorite;
import com.share.goods.mapper.ProductMapper;
import com.share.goods.mapper.UserFavoriteMapper;
import com.share.goods.service.IUserFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户商品收藏 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements IUserFavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, Long productId) {
        // 查询是否已收藏
        UserFavorite existing = userFavoriteMapper.selectOne(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getProductId, productId));

        if (existing != null) {
            // 已收藏 → 取消收藏（物理删除）
            userFavoriteMapper.deleteById(existing.getId());
            return false;
        } else {
            // 未收藏 → 添加收藏
            UserFavorite fav = new UserFavorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            fav.setCreateTime(new Date());
            userFavoriteMapper.insert(fav);
            return true;
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        return userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getProductId, productId)) > 0;
    }

    @Override
    public List<Map<String, Object>> selectUserFavorites(Long userId) {
        // 查收藏列表（按收藏时间倒序）
        List<UserFavorite> favorites = userFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime));

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查商品信息
        Set<Long> productIds = favorites.stream()
                .map(UserFavorite::getProductId).collect(Collectors.toSet());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 合并结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserFavorite fav : favorites) {
            Product product = productMap.get(fav.getProductId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("favoriteId", fav.getId());
            item.put("productId", fav.getProductId());
            item.put("createTime", fav.getCreateTime());
            if (product != null) {
                item.put("name", product.getName());
                item.put("mainImage", product.getMainImage());
                item.put("minPrice", product.getMinPrice());
                item.put("sales", product.getSales());
                item.put("status", product.getStatus());
            } else {
                // 商品已被物理删除
                item.put("deleted", true);
            }
            result.add(item);
        }
        return result;
    }
}
