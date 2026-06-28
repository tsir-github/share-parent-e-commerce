package com.share.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.OrderInfo;

import java.util.List;
import java.util.Map;

/**
 * 商家端订单 Service 接口
 *
 * @author share
 */
public interface IMerchantOrderService extends IService<OrderInfo> {

    /**
     * 查询商家订单列表
     */
    List<OrderInfo> selectMerchantOrderList(Long merchantId);

    /**
     * 查询订单详情（含归属校验）
     */
    OrderInfo getMerchantOrder(Long id, Long merchantId);

    /**
     * 商家发货
     */
    void deliverMerchantOrder(String orderNo, Long merchantId, String deliveryName, String deliveryPhone);

    /**
     * 商家批量发货
     *
     * @param items      发货列表 [{orderNo, deliveryName, deliveryPhone}, ...]
     * @param merchantId 商家ID
     * @return {success: 成功数, failed: 失败数, errors: [{orderNo, reason}]}
     */
    Map<String, Object> batchDeliver(List<Map<String, String>> items, Long merchantId);
}
