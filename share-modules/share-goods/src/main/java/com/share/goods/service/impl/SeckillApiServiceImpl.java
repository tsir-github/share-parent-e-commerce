package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.MqConstants;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.uuid.Seq;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.dto.SeckillOrderRequest;
import com.share.goods.domain.SeckillActivity;
import com.share.goods.domain.SeckillOrder;
import com.share.goods.domain.dto.SeckillOrderRequestDTO;
import com.share.goods.domain.vo.SeckillActivityVO;
import com.share.goods.domain.vo.SeckillDetailVO;
import com.share.goods.domain.vo.SeckillOrderResultVO;
import com.share.goods.mapper.SeckillOrderMapper;
import com.share.goods.service.ISeckillActivityService;
import com.share.goods.service.ISeckillApiService;
import com.share.goods.service.ISeckillCacheService;
import com.share.order.api.RemoteOrderInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * C端秒杀 Service 实现
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillApiServiceImpl implements ISeckillApiService {



    private final ISeckillActivityService seckillActivityService;
    private final SeckillOrderMapper seckillOrderMapper;
    private final RedissonClient redissonClient;
    private final RemoteOrderInfoService remoteOrderInfoService;
    private final RocketMQTemplate rocketMQTemplate;
    private final ISeckillCacheService seckillCacheService;

    public List<SeckillActivityVO> getSeckillList() {
        // 先查缓存
        List<SeckillActivityVO> cached = seckillCacheService.getSeckillList();
        if (cached != null) {
            return cached;
        }
        Date now = new Date();
        List<SeckillActivity> list = seckillActivityService.list(
                new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getDelFlag, "0")
                        .in(SeckillActivity::getStatus, "0", "1")
                        .orderByAsc(SeckillActivity::getSort)
                        .orderByAsc(SeckillActivity::getStartTime));
        List<SeckillActivityVO> result = list.stream().map(a -> {
            SeckillActivityVO vo = SeckillActivityVO.from(a);
            return vo;
        }).collect(Collectors.toList());
        seckillCacheService.setSeckillList(result);
        return result;
    }

    @Override
    public SeckillDetailVO getSeckillDetail(Long activityId, Long userId) {
        SeckillActivity a = seckillCacheService.getSeckillDetail(activityId);
        if (a == null) {
            a = seckillActivityService.getById(activityId);
            if (a != null) {
                seckillCacheService.setSeckillDetail(activityId, a);
            }
        }
        if (a == null) {
            return null;
        }
        SeckillDetailVO vo = SeckillDetailVO.from(a);
        // 查当前用户已购数量
        if (userId != null) {
            int purchased = seckillOrderMapper.countByUserAndActivity(activityId, userId);
            vo.setUserPurchasedCount(purchased);
            vo.setLimitReached(purchased >= a.getLimitPerUser());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSeckillOrder(Long activityId, Long userId, SeckillOrderRequestDTO dto) {
        // 1. 查活动
        SeckillActivity activity = seckillActivityService.getById(activityId);
        if (activity == null) {
            throw new ServiceException("秒杀活动不存在");
        }

        // 2. 校验活动状态和时间
        Date now = new Date();
        if (!"1".equals(activity.getStatus())) {
            if ("0".equals(activity.getStatus())) {
                throw new ServiceException("活动尚未开始");
            }
            throw new ServiceException("活动已结束");
        }
        if (now.before(activity.getStartTime())) {
            throw new ServiceException("活动尚未开始");
        }
        if (now.after(activity.getEndTime())) {
            throw new ServiceException("活动已结束");
        }

        // 3. 校验限购
        int purchased = seckillOrderMapper.countByUserAndActivity(activityId, userId);
        int remainingLimit = activity.getLimitPerUser() - purchased;
        if (remainingLimit <= 0) {
            throw new ServiceException("已达限购上限");
        }
        if (dto.getQuantity() > remainingLimit) {
            throw new ServiceException("超过限购数量，还可购买" + remainingLimit + "件");
        }

        // 4. Redis Lua 预扣库存
        boolean deducted = preDeductStock(activityId, dto.getQuantity());
        if (!deducted) {
            throw new ServiceException("库存不足");
        }

        // 5. 预生成订单号
        String orderNo = Seq.nextOrderNo();

        // 6. 记录秒杀订单快照（限购校验 + 超时归还库存依赖此表）
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setActivityId(activityId);
        seckillOrder.setUserId(userId);
        seckillOrder.setOrderNo(orderNo);
        seckillOrder.setQuantity(dto.getQuantity());
        seckillOrder.setSeckillPrice(activity.getSeckillPrice());
        seckillOrderMapper.insert(seckillOrder);

        // 7. 构造 MQ 消息并投递
        SeckillOrderRequest request = new SeckillOrderRequest();
        request.setOrderNo(orderNo);
        request.setUserId(userId);
        request.setSeckillActivityId(activityId);
        request.setSeckillPrice(activity.getSeckillPrice());
        request.setReceiverName(dto.getReceiverName());
        request.setReceiverPhone(dto.getReceiverPhone());
        request.setReceiverAddress(dto.getReceiverAddress());
        request.setRemark(dto.getRemark());

        SeckillOrderRequest.SeckillItem item = new SeckillOrderRequest.SeckillItem();
        item.setSkuId(activity.getSkuId());
        item.setQuantity(dto.getQuantity());
        item.setProductName(activity.getProductName());
        item.setSkuSpecs(activity.getSkuSpecs());
        request.setItems(List.of(item));

        try {
            rocketMQTemplate.syncSend(MqConstants.SECKILL_ORDER_CREATE_TOPIC, request);
            log.info("秒杀下单消息投递成功: activityId={}, orderNo={}, userId={}", activityId, orderNo, userId);
        } catch (Exception e) {
            // MQ 投递失败 → 归还 Redis 库存 + 删除 seckill_order 记录
            preReleaseStock(activityId, dto.getQuantity());
            seckillOrderMapper.deleteById(seckillOrder.getId());
            log.error("秒杀下单 MQ 投递失败，已归还库存: activityId={}, orderNo={}", activityId, orderNo, e);
            throw new ServiceException("下单服务繁忙，请稍后再试");
        }

        return orderNo;
    }

    private boolean preDeductStock(Long activityId, int quantity) {
        String key = CacheConstants.SECKILL_STOCK_KEY + activityId;
        String lua = "local key = KEYS[1] " +
                "local qty = tonumber(ARGV[1]) " +
                "local stock = redis.call('get', key) " +
                "if stock == false then " +
                "    return -2 " +   // cache miss signal
                "end " +
                "stock = tonumber(stock) " +
                "if stock >= qty then " +
                "    redis.call('decrby', key, qty) " +
                "    return stock - qty " +
                "else " +
                "    return -1 " +   // insufficient
                "end";
        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        Long result = script.eval(RScript.Mode.READ_WRITE, lua, RScript.ReturnType.INTEGER,
                Collections.singletonList(key), quantity);

        // cache miss → fallback to DB
        if (result != null && result == -2L) {
            log.warn("秒杀库存缓存 miss，回源 DB: activityId={}", activityId);
            SeckillActivity activity = seckillActivityService.getById(activityId);
            if (activity == null) return false;
            redissonClient.getBucket(key).set(activity.getStock());
            // retry deduction
            result = script.eval(RScript.Mode.READ_WRITE, lua, RScript.ReturnType.INTEGER,
                    Collections.singletonList(key), quantity);
        }
        return result != null && result >= 0L;
    }

    private void preReleaseStock(Long activityId, int quantity) {
        String key = CacheConstants.SECKILL_STOCK_KEY + activityId;
        String lua = "local key = KEYS[1] " +
                "local qty = tonumber(ARGV[1]) " +
                "redis.call('incrby', key, qty) " +
                "return 1";
        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        script.eval(RScript.Mode.READ_WRITE, lua, RScript.ReturnType.INTEGER,
                Collections.singletonList(key), quantity);
    }

    @Override
    public void releaseStock(Long activityId, int quantity) {
        preReleaseStock(activityId, quantity);
        log.info("秒杀库存归还: activityId={}, quantity={}", activityId, quantity);
    }

    @Override
    public void releaseStockByOrderNo(String orderNo) {
        SeckillOrder so = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getOrderNo, orderNo));
        if (so != null) {
            preReleaseStock(so.getActivityId(), so.getQuantity());
            log.info("超时取消归还秒杀库存: orderNo={}, activityId={}, quantity={}", orderNo, so.getActivityId(), so.getQuantity());
        }
    }

    private String doCreateOrder(SeckillActivity activity, Long userId, SeckillOrderRequestDTO dto) {
        // 构造 Feign 请求
        SeckillOrderRequest request = new SeckillOrderRequest();
        request.setUserId(userId);
        request.setSeckillActivityId(activity.getId());
        request.setSeckillPrice(activity.getSeckillPrice());
        request.setReceiverName(dto.getReceiverName());
        request.setReceiverPhone(dto.getReceiverPhone());
        request.setReceiverAddress(dto.getReceiverAddress());
        request.setRemark(dto.getRemark());

        SeckillOrderRequest.SeckillItem item = new SeckillOrderRequest.SeckillItem();
        item.setSkuId(activity.getSkuId());
        item.setQuantity(dto.getQuantity());
        item.setProductName(activity.getProductName());
        item.setSkuSpecs(activity.getSkuSpecs());
        request.setItems(List.of(item));

        R<String> result = remoteOrderInfoService.createSeckillOrder(request, SecurityConstants.INNER);
        if (result == null || result.getData() == null) {
            throw new ServiceException("订单服务调用失败");
        }
        return result.getData();
    }
}
