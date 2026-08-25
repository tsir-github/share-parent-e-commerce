package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单账单明细对象 order_bill
 *
 * @author atguigu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_bill")
@Schema(description = "订单账单明细")
public class OrderBill extends BaseEntity {

    private static final long serialVersionUID = 1L;

    //@Schema(description = "ID")
    //private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "账单项")
    private String billItem;

    @Schema(description = "账单金额")
    private BigDecimal billAmount;

    @Schema(description = "备注")
    private String remark;
}
