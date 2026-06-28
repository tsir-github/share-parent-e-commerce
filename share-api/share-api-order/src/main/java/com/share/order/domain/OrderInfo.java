package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.share.common.core.annotation.Excel;
import com.share.common.core.web.domain.BaseEntity;
import com.share.order.domain.vo.UserInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单对象 order_info
 *
 * @author atguigu
 * @date 2024-10-25
 */
@Data
@TableName("order_info")
@Schema(description = "订单")
public class OrderInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Excel(name = "用户ID")
    @Schema(description = "用户ID")
    private Long userId;

    /** 订单号 */
    @Excel(name = "订单号")
    @Schema(description = "订单号")
    private String orderNo;

    /** 订单开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "订单开始时间")
    @TableField(exist = false)
    private Date startTime;

    /** 订单结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "订单结束时间")
    @TableField(exist = false)
    private Date endTime;

    /** 供应商ID */
    @Excel(name = "供应商ID")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 商品总金额 */
    @Excel(name = "商品总金额")
    @Schema(description = "商品总金额")
    private BigDecimal totalAmount;

    /** 优惠金额 */
    @Excel(name = "优惠金额")
    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    /** 运费 */
    @Excel(name = "运费")
    @Schema(description = "运费")
    private BigDecimal freightAmount;

    /** 实付金额 */
    @Excel(name = "实付金额")
    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    /** 已退款金额累计 */
    @Excel(name = "已退款金额")
    @Schema(description = "已退款金额累计")
    private BigDecimal refundAmount;

    /** 最近退款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近退款时间")
    private Date refundTime;

    /** 退款次数 */
    @Excel(name = "退款次数")
    @Schema(description = "退款次数")
    private Integer refundCount;

    /** 支付状态：0-未支付 1-已支付 */
    @Excel(name = "支付状态")
    @Schema(description = "支付状态：0-未支付 1-已支付")
    private String payStatus;

    /** 订单类型：0-普通 1-秒杀 2-拼团 */
    @Excel(name = "订单类型")
    @Schema(description = "订单类型：0-普通 1-秒杀 2-拼团")
    private String orderType;

    /** 订单状态：0-待支付 1-待发货 2-配送中 3-已完成 4-已取消 5-售后中 */
    @Excel(name = "订单状态")
    @Schema(description = "订单状态：0-待支付 1-待发货 2-配送中 3-已完成 4-已取消 5-售后中")
    private String status;

    /** 配送状态：0-未配送 1-配送中 2-已送达 3-已确认 */
    @Excel(name = "配送状态")
    @Schema(description = "配送状态：0-未配送 1-配送中 2-已送达 3-已确认")
    private String deliveryStatus;

    /** 配送员ID */
    @Schema(description = "配送员ID")
    private Long deliveryBy;

    /** 配送员姓名 */
    @Excel(name = "配送员姓名")
    @Schema(description = "配送员姓名")
    private String deliveryName;

    /** 配送员电话 */
    @Excel(name = "配送员电话")
    @Schema(description = "配送员电话")
    private String deliveryPhone;

    /** 微信支付交易号 */
    @Excel(name = "微信支付交易号")
    @Schema(description = "微信支付交易号")
    private String transactionId;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "支付时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间")
    private Date payTime;

    /** 配送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "配送时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "配送时间")
    private Date deliveryTime;

    /** 签收时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "签收时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "签收时间")
    private Date receiveTime;

    /** 收货人姓名 */
    @Excel(name = "收货人姓名")
    @Schema(description = "收货人姓名")
    private String receiverName;

    /** 收货人电话 */
    @Excel(name = "收货人电话")
    @Schema(description = "收货人电话")
    private String receiverPhone;

    /** 收货地址 */
    @Excel(name = "收货地址")
    @Schema(description = "收货地址")
    private String receiverAddress;

    /** 关闭原因 */
    @Schema(description = "关闭原因")
    private String closeReason;

    /** 关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "关闭时间")
    private Date closeTime;

    /** 关闭类型：1-用户取消 2-超时取消 3-商家取消 4-客服取消 */
    @Schema(description = "关闭类型：1-用户取消 2-超时取消 3-商家取消 4-客服取消")
    private String closeType;

    /** 使用的优惠券ID列表，逗号分隔 */
    @Schema(description = "使用的优惠券ID列表")
    private String couponIds;

    /** 金额明细JSON */
    @Schema(description = "金额明细JSON")
    private String amountDetail;

    /** 扩展字段JSON */
    @Schema(description = "扩展字段JSON")
    private String extJson;

    /** 乐观锁版本号 */
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "订单账单明细")
    @TableField(exist = false)
    private List<OrderBill> orderBillList;

    @Schema(description = "用户信息")
    @TableField(exist = false)
    private UserInfoVo userInfoVo;
}
