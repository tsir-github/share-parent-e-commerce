# 数据模型设计

**Branch**: 001-project-audit | **Date**: 2026-06-23

## 1. 实体关系总览

```
UserInfo (share-user)
  │
  ├──< OrderInfo (share-order)
  │       ├──< OrderItem          (share-order, 订单商品行)
  │       ├──< OrderLog           (share-order, 状态变更流水)
  │       ├──< OrderBill          (share-order, 账单分账)
  │       └──< PaymentInfo        (share-payment, 支付记录)
  │
  ├──< CouponUser (share-coupon)
  │       └──< CouponTemplate    (share-coupon, 优惠券模板)
  │
  ├──< Cart     (share-order, 购物车)
  └──< UserAddress (源码丢失, 待重建)

MerchantInfo (share-merchant)
  ├──< Product (share-goods)
  │       ├──< ProductSku        (share-goods)
  │       └──< ProductImage      (代码缺失, 待实现)
  └──< MerchantUser (源码丢失, 待重建)
```

### 核心模块 ER (订单-支付-优惠券-商品)

```
UserInfo ──1:N──> OrderInfo ──1:N──> OrderItem ──N:1──> ProductSku
                        │                                       │
                        │ 1:1                                    │ 1:1
                        v                                       v
                  PaymentInfo                              Product
                        │
                        │ (couponIds 逗号分隔, 逻辑关联)
                        v
                  CouponUser ──N:1──> CouponTemplate
```

---

## 2. 订单状态模型（三维）

### 主状态 status
```
    +-----[创建订单]----> 0:待支付 ──[支付成功]──> 1:待发货
    │                           │                       │
    │                     [超时/取消]               [分配配送员]
    │                           │                       │
    │                           v                       v
    │                        4:已取消                2:配送中
    │                                                   │
    │                                             [确认收货]
    │                                                   │
    │                                                   v
    │                                                3:已完成
    │                                                   │
    │                                              [申请售后]
    │                                                   │
    │                                                   v
    │                                                5:售后中
    └──────────────────────────────────────────────────────┘
```

### 状态转换表

| 操作 | 前状态 | 后状态 | 前提 | 关联操作 |
|---|---|---|---|---|
| 创建订单 | — | 0:待支付 | 库存校验通过 | 扣库存、锁优惠券、发 MQ 延迟消息 |
| 支付成功 | 0:待支付 | 1:待发货 | 微信回调 | 更新 payStatus=已支付, payTime, transactionId |
| 超时取消 | 0:待支付 | 4:已取消 | 30分钟未支付 | 释放库存、释放优惠券 |
| 用户取消 | 0:待支付 | 4:已取消 | 用户主动操作 | 释放库存、释放优惠券 |
| 分配配送员 | 1:待发货 | 2:配送中 | 商家操作 | 记录配送员信息 |
| 确认收货 | 2:配送中 | 3:已完成 | 用户/系统自动 | 记录 receiveTime |
| 申请售后 | 3:已完成 | 5:售后中 | 用户发起 | (当前未实现) |

### 支付状态 pay_status

- 0: 未支付 (PAY_UNPAID)
- 1: 已支付 (PAY_PAID)
- 2: 已退款 (PAY_REFUNDED) -- 仅在订单维度标记

### 配送状态 delivery_status

- 0: 未配送 (DELIVERY_UNDELIVERED)
- 1: 配送中 (DELIVERY_IN_TRANSIT)
- 2: 已送达 (DELIVERY_DELIVERED)
- 3: 已确认 (DELIVERY_CONFIRMED)

---

## 3. 支付状态模型

### 当前状态（PaymentStatus.java）

```
   UNPAID (0) ──[支付成功]──> PAID (1) ──[退款]──> REFUNDED (2)
```

### 修复后（新增 REFUNDING=3）

```
   UNPAID (0) ──[支付成功]──> PAID (1) ──[发起退款]──> REFUNDING (3) ──[退款到账]──> REFUNDED (2)
                                                              │
                                                         [退款异常]
                                                              │
                                                              v
                                                           PAID (1) 恢复
```

| 操作 | 前状态 | 后状态 | 说明 |
|---|---|---|---|
| 支付成功 | UNPAID | PAID | 微信异步通知 |
| 发起退款 | PAID | REFUNDING | 调用微信退款 API |
| 退款成功 | REFUNDING | REFUNDED | 微信异步通知 |
| 退款异常 | REFUNDING | PAID | 退款被拒，恢复已支付 |

---

## 4. 优惠券状态模型

### CouponTemplate（模板状态）

- `status`: 0-未启用, 1-已启用, 2-已过期
- `totalCount`: 发行总量（-1 表示不限）
- 注意：当前实体**缺少** `remainCount` 和 `version` 字段，无法实现库存校验和乐观锁

### 待新增字段（CouponTemplate）

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| remain_count | int | 0 | 剩余数量（初始化时 = totalCount），-1 表示不限 |
| version | int | 0 | 乐观锁版本号 |

### CouponUser（用户优惠券状态）

- `status`: 0-未使用, 1-已使用/已锁定, 2-已过期

```
  0:未使用 ──[下单锁定]──> 1:已锁定/已使用 ──[取消订单]──> 0:未使用 (释放)
                           │
                       [订单完成] → 保持 1:已使用
```

---

## 5. 关键字段约束

### OrderInfo

| 字段 | 类型 | Null | 默认值 | 索引 | 备注 |
|---|---|---|---|---|---|
| id | bigint | NO | auto_inc | PK | 主键 |
| order_no | varchar(64) | NO | — | UNIQUE | 订单号 |
| user_id | bigint | NO | — | idx | 用户ID |
| status | char(1) | NO | '0' | — | 主状态 |
| pay_status | char(1) | NO | '0' | — | 支付状态 |
| delivery_status | char(1) | NO | '0' | — | 配送状态 |
| pay_amount | decimal(10,2) | NO | 0.00 | — | 实付金额 |
| transaction_id | varchar(64) | YES | — | — | 微信支付交易号 |
| version | int | NO | 0 | — | 乐观锁 |
| close_type | char(1) | YES | — | — | 关闭类型 |
| coupon_ids | varchar(255) | YES | — | — | 逗号分隔 |
| del_flag | char(1) | NO | '0' | — | 逻辑删除 |

### PaymentInfo

| 字段 | 类型 | Null | 默认值 | 索引 | 备注 |
|---|---|---|---|---|---|
| id | bigint | NO | auto_inc | PK | 主键 |
| order_no | varchar(64) | NO | — | idx | 订单号 |
| user_id | bigint | NO | — | — | 用户ID |
| pay_way | tinyint | NO | 1 | — | 1-微信 |
| transaction_id | varchar(64) | YES | — | — | 微信交易号 |
| amount | decimal(10,2) | NO | — | — | 金额 |
| payment_status | tinyint | NO | 0 | — | 0-未支付 1-已支付 2-已退款 **3-退款中** |
| callback_content | text | YES | — | — | 微信回调原文 |
| remark | varchar(255) | YES | — | — | 备注(存 refundId) |

### CouponTemplate

| 字段 | 类型 | Null | 默认值 | 备注 |
|---|---|---|---|---|
| id | bigint | NO | auto_inc | PK |
| name | varchar(100) | NO | — | 优惠券名称 |
| merchant_id | bigint | YES | — | 商家ID |
| type | char(1) | NO | '0' | 0-现金券 1-折扣券 |
| condition_amt | decimal(10,2) | YES | 0.00 | 满减条件 |
| discount_amt | decimal(10,2) | YES | — | 优惠金额 |
| discount_rate | decimal(5,2) | YES | — | 折扣率 |
| total_count | int | NO | 0 | 发行总量(-1不限) |
| **remain_count** | **int** | **待加** | **0** | **剩余数量** |
| limit_per_user | int | NO | 1 | 每人限领 |
| start_time | datetime | NO | — | 有效期开始 |
| end_time | datetime | NO | — | 有效期结束 |
| status | char(1) | NO | '0' | 0-未启用 1-已启用 2-已过期 |
| **version** | **int** | **待加** | **0** | **乐观锁** |

### CouponUser

| 字段 | 类型 | Null | 默认值 | 索引 | 备注 |
|---|---|---|---|---|---|
| id | bigint | NO | auto_inc | PK | |
| user_id | bigint | NO | — | idx | 用户ID |
| template_id | bigint | NO | — | idx | 模板ID |
| order_no | varchar(64) | YES | — | — | 锁定订单号 |
| status | char(1) | NO | '0' | — | 0-未使用 1-已锁定 2-已过期 |
| used_time | datetime | YES | — | — | 使用时间 |

### Cart（购物车）

| 字段 | 类型 | Null | 默认值 | 备注 |
|---|---|---|---|---|
| id | bigint | NO | auto_inc | |
| user_id | bigint | NO | — | 用户ID |
| sku_id | bigint | NO | — | SKU ID |
| quantity | int | NO | 1 | 数量 |
| selected | tinyint | YES | 1 | 是否选中 |

---

## 6. MQ 消息模型

### Topic 定义（MqConstants）

| Topic | 消息体 | 用途 | 消费者 |
|---|---|---|---|
| ORDER_TIMEOUT_CANCEL_TOPIC | String (orderNo) | 订单超时取消 | OrderTimeoutConsumer |
| PAYMENT_SUCCESS_TOPIC | PaymentSuccessMessage | 支付成功通知 | PaymentSuccessConsumer |
| ORDER_AUTO_CONFIRM_TOPIC | (待确认) | 订单自动确认 | **未实现** |

### PaymentSuccessMessage

```java
orderNo: String      // 订单号
transactionId: String // 微信交易号
payTime: Date        // 支付时间
```

---

## 7. 现有问题记录

| # | 模型问题 | 影响 | 修复 |
|---|---|---|---|
| M01 | CouponTemplate 缺 remain_count 字段 | 无法校验库存 | 加字段 + 迁移脚本 |
| M02 | CouponTemplate 缺 version 字段 | 扣库存无乐观锁 | 加字段 + 迁移脚本 |
| M03 | PaymentStatus 缺 REFUNDING=3 | 退款中状态不明确 | 加常量 + 迁移改字段注释 |
| M04 | cancelOrder 不释放库存/优惠券 | 支付后取消库存不回滚 | P1 修复 Feign 调用 |
| M05 | ORDER_AUTO_CONFIRM_TOPIC 无消费者 | 订单不会自动确认收货 | P2 实现 |
| M06 | 8 个源码文件丢失 | 远程调用失败 | P2 恢复 |
