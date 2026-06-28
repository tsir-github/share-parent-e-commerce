# 数据模型 — 006-complete-pay-flow

## 变更说明

本次无新增实体、无数据库表变更。涉及的数据模型均已存在于现有模块中：

| 实体 | 模块 | 说明 |
|------|------|------|
| `OrderInfo` | share-api-order | 订单主表，含 `status`/`payStatus`/`refundAmount`/`version` 字段 |
| `PaymentInfo` | share-api-payment | 支付记录表，含 `refundStatus`/`refundAmount` |
| `RefundSuccessMessage` | share-api-payment | MQ 消息 DTO（非实体） |

## 关键常量

- `OrderStatus.PAY_REFUNDED = "2"` — 支付状态：已退款
- `OrderStatus.CANCELLED = "4"` — 订单主状态：已取消（全额退款终态）
- `OrderOperateType.REFUND = "7"` — 操作类型：退款

## 状态机

```
售后中(5) ──退款──→ 已取消(4)
已完成(3) ──退款──→ 已取消(4)
```

## 无变更

- 无新表
- 无新字段
- 无新索引
- 无新枚举值
