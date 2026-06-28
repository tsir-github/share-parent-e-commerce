# Interface Contracts: 订单-支付流程打通

**Date**: 2026-06-24 | **Feature**: 003-order-payment-integration

## Feign 接口

### RemotePaymentService (share-api-payment)

```java
// [已存在] 创建支付单 — 订单侧调用
@PostMapping("/inner/payment/create")
R<Void> createPayment(@RequestBody Map<String, Object> params,
                      @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
// params: {orderNo, userId, amount, description, openid}

// [已存在] 查询支付信息 — 订单侧查询支付状态
@GetMapping("/inner/payment/{orderNo}")
R<Map<String, Object>> getPaymentByOrderNo(@PathVariable("orderNo") String orderNo,
                                           @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
// 返回 Map: {id, orderNo, amount, paymentStatus, transactionId, payWay, userId}
// paymentStatus: 0=UNPAID, 1=PAID, 2=REFUNDED, 3=REFUNDING

// [待新增] 查询支付状态
@GetMapping("/inner/payment/status/{orderNo}")
R<Integer> getPaymentStatus(@PathVariable("orderNo") String orderNo,
                            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
```

### RemoteOrderInfoService (share-api-order)

```java
// [已存在] 查询未完成订单
@GetMapping("/orderInfo/getNoFinishOrder/{userId}")
R<OrderInfo> getNoFinishOrder(@PathVariable("userId") Long userId);

// [已存在] 按订单号查询
@GetMapping("/orderInfo/getByOrderNo/{orderNo}")
R<OrderInfo> getByOrderNo(@PathVariable("orderNo") String orderNo);
```

**Note**: 支付成功回调使用 MQ（order-pay-success topic），不走 Feign 直调。这是设计决策：解耦 + 削峰 + 补偿措施已就位。

## MQ 合约

### Topic: order-pay-success

| 属性 | 值 |
|------|-----|
| Topic | `order-pay-success` (MqConstants.PAYMENT_SUCCESS_TOPIC) |
| 消息类 | `PaymentSuccessMessage{orderNo, transactionId}` |
| 生产者 | `PaymentMQProducer` (share-payment) |
| 消费者 | `PaymentSuccessConsumer` (share-order) |
| 消费者 Group | `share-order-pay-consumer` |
| 投递语义 | 至少一次（消费端幂等保证） |
| 补偿 | `PaymentCompensationTask` 每60秒重发 |

### Topic: order-timeout-cancel

| 属性 | 值 |
|------|-----|
| Topic | `order-timeout-cancel` (MqConstants.ORDER_TIMEOUT_CANCEL_TOPIC) |
| 消息类型 | `String` (orderNo) |
| 生产者 | `OrderInfoServiceImpl.createOrder` (share-order) |
| 消费者 | `OrderTimeoutConsumer` (share-order) |
| 延迟等级 | 默认 16 (约30min) — 待改为 Nacos 配置 |

## 回调合约

### 微信支付回调

```
POST /payment/api/v1/payment/callback
Headers: Wechatpay-Serial, Wechatpay-Signature, Wechatpay-Timestamp, Wechatpay-Nonce
Body (加密): { id, create_time, resource_type, event_type, resource: { ciphertext } }
```

返回:
```json
{"code":"SUCCESS","message":"成功"}
```
