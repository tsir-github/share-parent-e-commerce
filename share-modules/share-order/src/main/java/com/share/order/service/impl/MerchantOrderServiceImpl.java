package com.share.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.order.domain.OrderInfo;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.service.IOrderInfoService;
import com.share.order.service.IMerchantOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家端订单 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class MerchantOrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IMerchantOrderService {

    private final IOrderInfoService orderInfoService;

    @Override
    public List<OrderInfo> selectMerchantOrderList(Long merchantId) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        return orderInfoService.lambdaQuery()
                .eq(OrderInfo::getSupplierId, merchantId)
                .orderByDesc(OrderInfo::getCreateTime)
                .list();
    }

    @Override
    public OrderInfo getMerchantOrder(Long id, Long merchantId) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        OrderInfo order = orderInfoService.selectOrderInfoById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (!merchantId.equals(order.getSupplierId())) {
            throw new ServiceException("无权访问");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverMerchantOrder(String orderNo, Long merchantId, String deliveryName, String deliveryPhone) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        if (StringUtils.isEmpty(orderNo) || StringUtils.isEmpty(deliveryName) || StringUtils.isEmpty(deliveryPhone)) {
            throw new ServiceException("参数不完整");
        }
        orderInfoService.deliverOrder(orderNo, merchantId, deliveryName, deliveryPhone);
    }

    @Override
    public Map<String, Object> batchDeliver(List<Map<String, String>> items, Long merchantId) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        if (items == null || items.isEmpty()) {
            throw new ServiceException("发货列表不能为空");
        }

        int success = 0;
        int failed = 0;
        List<Map<String, String>> errors = new ArrayList<>();

        for (Map<String, String> item : items) {
            String orderNo = item.get("orderNo");
            String deliveryName = item.get("deliveryName");
            String deliveryPhone = item.get("deliveryPhone");

            try {
                if (StringUtils.isEmpty(orderNo) || StringUtils.isEmpty(deliveryName) || StringUtils.isEmpty(deliveryPhone)) {
                    throw new ServiceException("参数不完整：订单号/配送员/电话不能为空");
                }
                orderInfoService.deliverOrder(orderNo, merchantId, deliveryName, deliveryPhone);
                success++;
            } catch (Exception e) {
                failed++;
                Map<String, String> err = new HashMap<>();
                err.put("orderNo", orderNo);
                err.put("reason", e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }
}
