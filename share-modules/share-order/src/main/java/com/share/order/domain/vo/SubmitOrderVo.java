package com.share.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SubmitOrderVo {

    @Schema(description = "消息编号")
    private String messageNo;

    @Schema(description = "用户Id")
    private Long userId;

    @Schema(description = "订单金额")
    private java.math.BigDecimal totalAmount;
}

