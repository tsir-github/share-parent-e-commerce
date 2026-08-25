package com.share.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.merchant.domain.MerchantFavorite;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.mapper.MerchantFavoriteMapper;
import com.share.merchant.service.IMerchantFavoriteService;
import com.share.merchant.service.IMerchantInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商家收藏 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class MerchantFavoriteServiceImpl extends ServiceImpl<MerchantFavoriteMapper, MerchantFavorite> implements IMerchantFavoriteService {

    private final IMerchantInfoService merchantInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, Long merchantId) {
        try {
            int deleted = baseMapper.delete(new LambdaQueryWrapper<MerchantFavorite>()
                    .eq(MerchantFavorite::getUserId, userId)
                    .eq(MerchantFavorite::getMerchantId, merchantId));
            if (deleted > 0) {
                return false; // 已取消
            }
            MerchantFavorite fav = new MerchantFavorite();
            fav.setUserId(userId);
            fav.setMerchantId(merchantId);
            baseMapper.insert(fav);
            return true; // 已收藏
        } catch (DuplicateKeyException e) {
            // ponytail: 并发插入冲突，唯一索引 uk_user_merchant 兜底
            return true;
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long merchantId) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<MerchantFavorite>()
                        .eq(MerchantFavorite::getUserId, userId)
                        .eq(MerchantFavorite::getMerchantId, merchantId)) > 0;
    }

    @Override
    public long countByMerchantId(Long merchantId) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<MerchantFavorite>()
                        .eq(MerchantFavorite::getMerchantId, merchantId));
    }

    @Override
    public List<Map<String, Object>> selectUserFavorites(Long userId) {
        List<MerchantFavorite> favs = baseMapper.selectList(
                new LambdaQueryWrapper<MerchantFavorite>()
                        .eq(MerchantFavorite::getUserId, userId)
                        .orderByDesc(MerchantFavorite::getCreateTime));
        if (favs.isEmpty()) return Collections.emptyList();

        List<Long> merchantIds = favs.stream().map(MerchantFavorite::getMerchantId).collect(Collectors.toList());
        List<MerchantInfo> merchants = merchantInfoService.listByIds(merchantIds);
        Map<Long, MerchantInfo> map = merchants.stream().collect(Collectors.toMap(MerchantInfo::getId, m -> m));

        return favs.stream().map(f -> {
            MerchantInfo m = map.get(f.getMerchantId());
            Map<String, Object> item = new HashMap<>();
            item.put("merchantId", f.getMerchantId());
            item.put("name", m != null ? m.getName() : "");
            item.put("logo", m != null ? m.getLogo() : "");
            item.put("createTime", f.getCreateTime());
            return item;
        }).collect(Collectors.toList());
    }
}
