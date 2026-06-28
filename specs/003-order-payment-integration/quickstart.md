# Quickstart: 订单-支付流程验证

**Date**: 2026-06-24 | **Feature**: 003-order-payment-integration

## 前置条件

- [ ] VM 中间件运行: Nacos, Redis Sentinel, MySQL, RocketMQ
- [ ] share-payment 服务运行（端口 9213）
- [ ] share-order 服务运行（端口 9211）
- [ ] share-gateway 服务运行（端口 8080）

## 端到端验证（mock 模式）

### Step 1: 创建订单

`
POST http://localhost:8080/order/orderInfo/createOrder
Authorization: Bearer <token>
Content-Type: application/json

{
  \"userId\": 1,
  \"skus\": [{
    \"skuId\": 1,
    \"quantity\": 2,
    \"price\": 99.00
  }],
  \"receiverName\": \"张三\",
  \"receiverPhone\": \"13800138000\",
  \"receiverAddress\": \"北京市朝阳区xxx小区\"
}
`

**预期结果**: 返回 orderNo（如 193847562938475）

### Step 2: 查询订单状态

`
GET http://localhost:8080/order/orderInfo/getByOrderNo/{orderNo}
Authorization: Bearer <token>
`

**预期结果**: status=\"0\" (待支付), payStatus=\"0\" (未支付)

### Step 3: 创建支付单

`
POST http://localhost:8080/payment/api/v1/payment/create
Authorization: Bearer <token>
Content-Type: application/json

{
  \"orderNo\": \"{orderNo}\",
  \"amount\": 198.00,
  \"description\": \"小区购物订单\"
}
`

**预期结果**: 返回 mock 支付参数（mock 模式下为模拟参数）

### Step 4: 模拟支付回调

`
POST http://localhost:8080/payment/api/v1/payment/mock/callback
Content-Type: application/json

{
  \"orderNo\": \"{orderNo}\"
}
`

**预期结果**: R.ok() — 支付单状态已更新为 PAID

### Step 5: 验证订单状态

`
GET http://localhost:8080/order/orderInfo/getByOrderNo/{orderNo}
Authorization: Bearer <token>
`

**预期结果**: status=\"1\" (待发货), payStatus=\"1\" (已支付), 	ransactionId 不为空, payTime 不为空

## 异常场景验证

### 场景 1: 重复回调幂等

执行 Step 4 两次 → 第二次应返回成功但 status 不重复变更。
验证: 数据库 payment_status=1 只更新一次，order_info 状态只转一次。

### 场景 2: 订单超时自动取消

创建一个订单不支付 → 等待超时时间（开发可临时改小）
验证: 订单 status=\"4\" (已取消), 库存释放

## 验证结果记录

| 场景 | 结果 | 备注 |
|------|------|------|
| 创建订单 | □ pass / □ fail | |
| 创建支付单 | □ pass / □ fail | |
| mock 回调 | □ pass / □ fail | |
| 订单状态更新 | □ pass / □ fail | |
| 重复回调幂等 | □ pass / □ fail | |
| 超时取消 | □ pass / □ fail | |

## 代码验证记录（2026-06-24）

### Phase 1 — 代码审查

| 文件 | 检查项 | 结果 |
|------|--------|------|
| OrderInfoServiceImpl.processPaySuccess() | payTime 字段写入 | ✅ 已设置 payTime (line 94) |
| PaymentSuccessConsumer | topic/consumerGroup/消息类型 | ✅ 正确 |
| OrderTimeoutConsumer | 消费者逻辑 | ✅ 正确 (delayLevel在createOrder中) |
| PaymentMQProducer | required=false降级 | ✅ 正确 |
| PaymentCompensationTask | SQL范围+分布式锁+重发 | ✅ 正确 |
| PaymentInfoController | getPaymentStatus端点 | ✅ 已存在 /inner/payment/status/{orderNo} |
| RemotePaymentService | Feign方法 | ✅ getPaymentStatus已补全 |
| PaymentInfoServiceImpl | updateToPaid幂等+MQ发送 | ✅ 正确 |

### 代码变更记录

| 变更 | 文件 |
|------|------|
| + getPaymentStatus Feign | `share-api-payment/.../api/RemotePaymentService.java` |
| + getPaymentStatus fallback | `share-api-payment/.../factory/RemotePaymentFallbackFactory.java` |
| + @Value payTimeoutMinutes + computeDelayLevel | `share-order/.../service/impl/OrderInfoServiceImpl.java` |
| + order.pay.timeout-minutes=30 | Nacos `share-order-dev.yml` |
| + seata.service.vgroup-mapping | Nacos `share-payment-dev.yml` |
