package com.share.order.api;

import com.share.common.core.domain.R;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.vo.OrderSqlVo;
import com.share.order.factory.RemoteOrderInfoFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * 订单服务 Feign 接口
 *
 * @author share
 */
@FeignClient(contextId = "remoteOrderInfoService",
        value = "share-order",
        fallbackFactory = RemoteOrderInfoFallbackFactory.class)
public interface RemoteOrderInfoService {

    @GetMapping("/inner/order/getNoFinishOrder/{userId}")
    R<OrderInfo> getNoFinishOrder(@PathVariable("userId") Long userId);

    @GetMapping("/inner/order/getByOrderNo/{orderNo}")
    R<OrderInfo> getByOrderNo(@PathVariable("orderNo") String orderNo);

    @PostMapping("/inner/order/getOrderCount")
    R<Map<String, Object>> getOrderCount(@RequestBody OrderSqlVo orderSqlVo);
}
