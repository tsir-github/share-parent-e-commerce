# 退款闭环数据模型

## 1. 新建：售后申请表 `after_sale_request`

```sql
CREATE TABLE `after_sale_request` (
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `order_no`      varchar(32)  NOT NULL                  COMMENT '订单号',
  `user_id`       bigint(20)   NOT NULL                  COMMENT '用户ID',
  `merchant_id`   bigint(20)   DEFAULT NULL              COMMENT '商家ID（关联 merchant_info.id）',
  `refund_amount` decimal(10,2) NOT NULL DEFAULT '0.00'  COMMENT '退款金额',
  `refund_reason` varchar(500) DEFAULT NULL              COMMENT '退款原因',
  `audit_status`  char(1)     NOT NULL DEFAULT '0'       COMMENT '审核状态：0-待审核 1-商家同意 2-商家拒绝 3-客服介入 4-客服同意退款 5-客服拒绝',
  `audit_remark`  varchar(500) DEFAULT NULL              COMMENT '审核备注',
  `audit_time`    datetime     DEFAULT NULL              COMMENT '审核时间',
  `audit_by`      varchar(64)  DEFAULT NULL              COMMENT '审核人',
  `close_time`    datetime     DEFAULT NULL              COMMENT '关闭时间',
  `close_type`    char(1)     DEFAULT NULL               COMMENT '关闭类型（留扩展）',
  `ext_json`      varchar(1000) DEFAULT NULL             COMMENT '扩展字段JSON',
  `del_flag`      char(1)     NOT NULL DEFAULT '0'       COMMENT '删除标志（0存在 2删除）',
  `create_by`     varchar(64)  DEFAULT ''                COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL              COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT ''                COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL              COMMENT '更新时间',
  `remark`        varchar(255) DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后申请表';
```

### 审核状态流转

```
待审核(0) ──商家同意──→ 商家同意(1) → 触发退款
待审核(0) ──商家拒绝──→ 商家拒绝(2) → 订单状态恢复
待审核(0) ──客服介入──→ 客服介入(3)
客服介入(3) ──客服同意──→ 客服同意退款(4) → 触发退款
客服介入(3) ──客服拒绝──→ 客服拒绝(5) → 订单状态恢复
```

### Java 实体 (share-order/domain/)

```java
/**
 * 售后申请表
 */
@Data
@TableName("after_sale_request")
public class AfterSaleRequest extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal refundAmount;
    private String refundReason;
    private String auditStatus;  // 0-待审核 1-商家同意 2-商家拒绝 3-客服介入 4-客服同意退款 5-客服拒绝
    private String auditRemark;
    private Date auditTime;
    private String auditBy;
    private Date closeTime;
    private String closeType;
    private String extJson;
}
```

## 2. 扩展：订单主表 `order_info`（新增字段）

```sql
ALTER TABLE `order_info`
  ADD COLUMN `refund_amount`  decimal(10,2) DEFAULT '0.00' COMMENT '已退款金额累计' AFTER `pay_amount`,
  ADD COLUMN `refund_time`   datetime       DEFAULT NULL    COMMENT '最近退款时间' AFTER `refund_amount`,
  ADD COLUMN `refund_count`  int(11)        DEFAULT '0'     COMMENT '退款次数' AFTER `refund_time`;
```

对应 OrderInfo.java Entity 新增字段：
```java
/** 已退款金额累计 */
private BigDecimal refundAmount;

/** 最近退款时间 */
private Date refundTime;

/** 退款次数 */
private Integer refundCount;
```

## 3. 退款 MQ 消息 DTO

参考 `PaymentSuccessMessage` 模式，新增 `RefundSuccessMessage`：

```java
package com.share.payment.domain.dto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundSuccessMessage implements Serializable {
    /** 订单号 */
    private String orderNo;

    /** 微信退款单号 */
    private String transactionId;

    /** 退款金额 */
    private BigDecimal refundAmount;
}
```

路径：`share-api/share-api-payment/src/main/java/com/share/payment/domain/dto/RefundSuccessMessage.java`

## 4. MQ 主题常量

在 `MqConstants.java` 新增：
```java
/** 退款成功 → 订单状态变更 */
String REFUND_SUCCESS_TOPIC = "order-refund-success";
```

## 5. payment_info 表（可选扩展）

当前退款状态通过 `payment_status` 字段管理（REFUNDED=2, REFUNDING=3），但不记录退款金额和时间。若需展示退款详情，可加：

```sql
-- 可选：跟踪单次退款（若需多次部分退款则抽离退款明细表）
ALTER TABLE `payment_info`
  ADD COLUMN `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '已退款金额' AFTER `amount`,
  ADD COLUMN `refund_time`   datetime      DEFAULT NULL COMMENT '最近退款时间' AFTER `refund_amount`;
```

## 6. 状态常量对照

| 模块 | 字段 | 常量位置 | 值 |
|---|---|---|---|
| order_info.status | 主状态 | OrderStatus | 0-待支付 1-待发货 2-配送中 3-已完成 4-已取消 5-售后中 |
| order_info.pay_status | 支付状态 | OrderStatus | 0-未支付 1-已支付 2-已退款 |
| order_info.delivery_status | 配送状态 | OrderStatus | 0-未配送 1-配送中 2-已送达 3-已确认 |
| payment_info.payment_status | 支付记录状态 | PaymentStatus | 0-未支付 1-已支付 2-已退款 3-退款中 |
| after_sale_request.audit_status | 审核状态 | 新建常量类 | 0-待审核 1-商家同意 2-商家拒绝 3-客服介入 4-客服同意退款 5-客服拒绝 |
