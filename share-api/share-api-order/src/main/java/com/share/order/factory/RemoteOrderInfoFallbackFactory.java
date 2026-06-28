package com.share.order.factory;

import com.share.common.core.domain.R;
import com.share.order.api.RemoteOrderInfoService;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.vo.OrderSqlVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单服务降级处理
 *
 * <p>Feign 调用失败时返回友好提示，不抛异常，确保 Sentinel 熔断降级正常生效。</p>
 *
 * @author share
 */
@Component
public class RemoteOrderInfoFallbackFactory implements FallbackFactory<RemoteOrderInfoService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteOrderInfoFallbackFactory.class);

    @Override
    public RemoteOrderInfoService create(Throwable throwable) {
        log.error("订单服务调用失败: {}", throwable.getMessage(), throwable);
        return new RemoteOrderInfoService() {
            @Override
            public R<OrderInfo> getNoFinishOrder(Long userId) {
                return R.fail("获取未完成订单失败:" + throwable.getMessage());
            }

            @Override
            public R<OrderInfo> getByOrderNo(String orderNo) {
                return R.fail("获取订单信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Map<String, Object>> getOrderCount(OrderSqlVo orderSqlVo) {
                return R.fail("获取订单统计失败:" + throwable.getMessage());
            }
        };
    }
}
