package com.share.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class EndOrderVo {

    @Schema(description = "消息编号")
    private String messageNo;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "结束时间")
    private Date endTime;
}

