package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.ProductStatus;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.DateUtils;
import com.share.goods.domain.Product;
import com.share.goods.domain.SeckillActivity;
import com.share.goods.mapper.SeckillActivityMapper;
import com.share.goods.service.ISeckillActivityService;
import com.share.goods.service.ISeckillCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 秒杀活动Service业务层处理
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements ISeckillActivityService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final RedissonClient redissonClient;
    private final ISeckillCacheService seckillCacheService;

    @Override
    public List<SeckillActivity> selectSeckillActivityList(SeckillActivity activity) {
        return seckillActivityMapper.selectSeckillActivityList(activity);
    }

    @Override
    public SeckillActivity selectSeckillActivityById(Long id) {
        return seckillActivityMapper.selectSeckillActivityById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSeckillActivity(SeckillActivity activity) {
        // 校验时间冲突
        checkTimeConflict(activity);
        activity.setStatus("0"); // 默认未开始
        activity.setVersion(0);
        activity.setDelFlag("0");
        int result = baseMapper.insert(activity);
        seckillCacheService.evictList();
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSeckillActivity(SeckillActivity activity) {
        SeckillActivity existing = baseMapper.selectById(activity.getId());
        if (existing == null) {
            throw new ServiceException("秒杀活动不存在");
        }
        // 已结束或已禁用的活动不可编辑
        if ("2".equals(existing.getStatus()) || "3".equals(existing.getStatus())) {
            throw new ServiceException("已结束或已禁用的活动不可编辑");
        }
        // 校验时间冲突（排除自身）
        checkTimeConflict(activity);
        // 使用 LambdaUpdateWrapper 只更新允许修改的字段，防止并发回退 status/version
        LambdaUpdateWrapper<SeckillActivity> uw = new LambdaUpdateWrapper<>();
        uw.eq(SeckillActivity::getId, activity.getId());
        uw.eq(SeckillActivity::getVersion, existing.getVersion());
        if (activity.getName() != null) uw.set(SeckillActivity::getName, activity.getName());
        if (activity.getProductId() != null) uw.set(SeckillActivity::getProductId, activity.getProductId());
        if (activity.getSkuId() != null) uw.set(SeckillActivity::getSkuId, activity.getSkuId());
        if (activity.getSeckillPrice() != null) uw.set(SeckillActivity::getSeckillPrice, activity.getSeckillPrice());
        if (activity.getStock() != null) uw.set(SeckillActivity::getStock, activity.getStock());
        if (activity.getLimitPerUser() != null) uw.set(SeckillActivity::getLimitPerUser, activity.getLimitPerUser());
        if (activity.getSort() != null) uw.set(SeckillActivity::getSort, activity.getSort());
        if (activity.getStartTime() != null) uw.set(SeckillActivity::getStartTime, activity.getStartTime());
        if (activity.getEndTime() != null) uw.set(SeckillActivity::getEndTime, activity.getEndTime());
        uw.set(SeckillActivity::getVersion, existing.getVersion() + 1);
        int result = baseMapper.update(null, uw);
        // 库存变更时同步 Redis
        if (activity.getStock() != null && !activity.getStock().equals(existing.getStock())) {
            try {
                String key = CacheConstants.SECKILL_STOCK_KEY + activity.getId();
                redissonClient.getBucket(key).set(activity.getStock());
                log.info("秒杀库存同步 Redis: activityId={}, stock {}→{}", activity.getId(), existing.getStock(), activity.getStock());
            } catch (Exception e) {
                log.warn("秒杀库存同步 Redis 失败: activityId={}", activity.getId(), e);
            }
        }
        // 失效读缓存
        seckillCacheService.evictList();
        seckillCacheService.evictDetail(activity.getId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStatus(Long id, String status) {
        SeckillActivity existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("秒杀活动不存在");
        }
        // 状态流转校验：只有未开始(0)可启用(1)，只有进行中(1)/未开始(0)可禁用(3)
        if ("1".equals(status) && !"0".equals(existing.getStatus())) {
            throw new ServiceException("只有未开始的活动可以启用");
        }
        if ("3".equals(status) && !"0".equals(existing.getStatus()) && !"1".equals(existing.getStatus())) {
            throw new ServiceException("只有未开始或进行中的活动可以禁用");
        }
        // 启用时预加载库存到 Redis
        if ("1".equals(status)) {
            try {
                String key = CacheConstants.SECKILL_STOCK_KEY + id;
                redissonClient.getBucket(key).set(existing.getStock());
                log.info("秒杀库存预加载成功: activityId={}, stock={}", id, existing.getStock());
            } catch (Exception e) {
                log.error("秒杀库存预加载失败: activityId={}", id, e);
                throw new ServiceException("活动启用失败：缓存写入异常，请稍后重试");
            }
        }
        LambdaUpdateWrapper<SeckillActivity> uw = new LambdaUpdateWrapper<>();
        uw.eq(SeckillActivity::getId, id);
        uw.eq(SeckillActivity::getVersion, existing.getVersion());
        uw.set(SeckillActivity::getStatus, status);
        uw.set(SeckillActivity::getVersion, existing.getVersion() + 1);
        int result = baseMapper.update(null, uw);
        seckillCacheService.evictList();
        seckillCacheService.evictDetail(id);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSeckillActivityByIds(Long[] ids) {
        List<Long> idList = Arrays.asList(ids);
        for (Long id : idList) {
            SeckillActivity existing = baseMapper.selectById(id);
            if (existing == null) {
                throw new ServiceException("秒杀活动不存在: " + id);
            }
            if ("1".equals(existing.getStatus())) {
                throw new ServiceException("进行中的活动不可删除: " + id);
            }
        }
        // 逻辑删除
        int result = baseMapper.update(null, new LambdaUpdateWrapper<SeckillActivity>()
                .in(SeckillActivity::getId, idList)
                .set(SeckillActivity::getDelFlag, "2"));
        seckillCacheService.evictList();
        for (Long id : idList) {
            seckillCacheService.evictDetail(id);
        }
        return result;
    }

    @Override
    public void checkTimeConflict(SeckillActivity activity) {
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            return;
        }
        if (activity.getStartTime().after(activity.getEndTime())) {
            throw new ServiceException("开始时间不能晚于结束时间");
        }
        if (activity.getProductId() == null) {
            return;
        }
        // 查同一SKU在同一时间范围是否有其他活动
        LambdaQueryWrapper<SeckillActivity> qw = new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getDelFlag, "0")
                .eq(SeckillActivity::getProductId, activity.getProductId())
                .eq(SeckillActivity::getSkuId, activity.getSkuId())
                .ne(activity.getId() != null, SeckillActivity::getId, activity.getId())
                .ne(SeckillActivity::getStatus, "3") // 禁用不参与冲突检测
                .ne(SeckillActivity::getStatus, "2") // 已结束不参与
                .and(w -> w
                        .between(SeckillActivity::getStartTime, activity.getStartTime(), activity.getEndTime())
                        .or(c -> c.between(SeckillActivity::getEndTime, activity.getStartTime(), activity.getEndTime()))
                        .or(c -> c
                                .le(SeckillActivity::getStartTime, activity.getStartTime())
                                .ge(SeckillActivity::getEndTime, activity.getEndTime())));
        long count = baseMapper.selectCount(qw);
        if (count > 0) {
            throw new ServiceException("该商品在同一时间段已存在秒杀活动");
        }
    }
}
