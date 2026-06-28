# 接口合约 — 006-complete-pay-flow

## 本次无新增接口

涉及的所有合约（Feign 接口、MQ 消息体、REST API）均已存在。无变更。

### 现有合约清单

| 合约 | 位置 | 说明 |
|------|------|------|
| `RemotePaymentService` | share-api-payment | Feign 接口：退款等 |
| `RefundSuccessMessage` | share-api-payment | MQ 消息体 DTO |
| `IOrderInfoService.processRefundSuccess()` | share-order | 退款成功处理 |
| `PaymentMQProducer` | share-payment | MQ 消息生产者 |
| `PaymentRefundConsumer` | share-order | MQ 消息消费者 |

### MQ 交互合约（确认）

```
Topic:       order-refund-success
Producer:    share-payment (PaymentMQProducer)
Consumer:    share-order   (PaymentRefundConsumer)
Message:     RefundSuccessMessage { orderNo, transactionId, refundAmount }
```

无需修改。
