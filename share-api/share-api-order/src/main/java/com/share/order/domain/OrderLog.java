package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.share.common.core.annotation.Excel;

import java.util.Date;

/**
 * 订单操作流水表 order_log
 *
 * 记录订单的每一步状态变更，用于追溯和审计。
 * 每次操作（下单/支付/发货/配送/取消等）生成一条记录。
 */
@Data
@Schema(description = "订单操作流水")
@TableName("order_log")
public class OrderLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @Excel(name = "订单ID")
    @Schema(description = "订单ID")
    private Long orderId;

    /** 订单号 */
    @Excel(name = "订单号")
    @Schema(description = "订单号")
    private String orderNo;

    /** 操作类型：0-下单 1-支付 2-发货 3-配送员送达 4-确认收货 5-取消 6-售后申请 7-同意退款 */
    @Excel(name = "操作类型")
    @Schema(description = "操作类型：0-下单 1-支付 2-发货 3-配送员送达 4-确认收货 5-取消 6-售后申请 7-同意退款")
    private String operateType;

    /** 操作前状态 */
    @Excel(name = "操作前状态")
    @Schema(description = "操作前状态")
    private String beforeStatus;

    /** 操作后状态 */
    @Excel(name = "操作后状态")
    @Schema(description = "操作后状态")
    private String afterStatus;

    /** 操作人 */
    @Excel(name = "操作人")
    @Schema(description = "操作人：用户/系统/后台管理员")
    private String operateUser;

    /** 备注 */
    @Excel(name = "备注")
    @Schema(description = "备注")
    private String note;

}

