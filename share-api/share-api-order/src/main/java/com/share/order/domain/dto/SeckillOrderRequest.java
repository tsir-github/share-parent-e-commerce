package com.share.order.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 秒杀订单创建请求（Feign 内部调用 DTO）
 *
 * @author share
 */
@Data
public class SeckillOrderRequest {

    /** 预生成的订单号（MQ 异步模式下由 share-goods 预生成） */
    private String orderNo;
    private Long userId;
    private Long seckillActivityId;
    private BigDecimal seckillPrice;
    private List<SeckillItem> items;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;

    @Data
    public static class SeckillItem {
        private Long skuId;
        private Integer quantity;
        private String productName;
        private String skuSpecs;
    }
}
