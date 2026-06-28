# Research: 订单-支付流程打通

**Date**: 2026-06-24 | **Feature**: 003-order-payment-integration

## Codebase Discovery

### share-order 现有集成点

1. **PaymentSuccessConsumer** (consumer/) — 已存在，消费 topic order-pay-success
   - 消息类型: PaymentSuccessMessage{orderNo, transactionId}
   - 调用: orderInfoService.processPaySuccess(orderNo, transactionId)
   - 幂等: transition() 状态机校验 + 乐观锁

2. **processPaySuccess** (OrderInfoServiceImpl) — 已实现
   - 查询 OrderInfo → transition(orderId, orderNo, PAY, 1, ...) 待支付→待发货
   - LambdaUpdateWrapper: set payStatus=1, payTime=new Date(), transactionId
   - 日志: \"订单支付成功处理: orderNo={}\"

3. **超时取消** (OrderTimeoutConsumer) — 已实现
   - 延迟消息 level=16 (≈30min) → 硬编码
   - 消费 topic order-timeout-cancel → cancelOrder() → 库存回滚 + 优惠券释放

4. **Feign 接口** — RemoteOrderInfoService 只有查询
   - 支付成功回调不走 Feign（走 MQ）
   - Controller 有 /inner/paySuccess/{orderNo}/{transactionId} 但 Feign 未暴露

### share-payment 现有集成点

1. **MQ生产者** — PaymentMQProducer.sendPaySuccessMessage(orderNo, transactionId)
   - Topic: order-pay-success (MqConstants.PAYMENT_SUCCESS_TOPIC)
   - 同步发送，失败仅打日志不抛异常
   - RocketMQTemplate 为 @Autowired(required=false) — MQ 不可用时降级

2. **补偿任务** — PaymentCompensationTask 每60s扫描
   - WHERE callback_time BETWEEN now-2h AND now-30s AND payment_status=PAID
   - LIMIT 50，按 callback_time ASC

3. **Feign 接口** — RemotePaymentService 缺 getPaymentStatus
   - Controller 有 /inner/payment/status/{orderNo} 端点 (@InnerAuth)
   - 可通过 getPaymentByOrderNo(orderNo) 获取 status 字段作为替代

### 状态映射

| 概念 | PaymentInfo | OrderInfo |
|------|------------|-----------|
| 支付状态 | paymentStatus: 0/1/2/3 (Integer) | payStatus: 0/1/2 (String) |
| 主状态 | 无 | status: 0~5 (String) |
| 关联 | orderNo → OrderInfo.orderNo | transactionId, payTime, payStatus |

### 决策

- **Decision**: 支付成功使用 MQ 通知而非 Feign 直调
  - **Rationale**: 解耦 + 削峰 + 补偿措施已就位
  - **Alternative**: Feign 直调 → 紧耦合，支付侧不可用时订单侧异常
- **Decision**: 超时配置从硬编码改为 Nacos
  - **Rationale**: 运行时可调，不需要重启
  - **Implementation**: 计算 RocketMQ delayLevel 映射表
- **Decision**: 复用现有 PaymentSuccessMessage 不扩展
  - **Rationale**: orderNo + transactionId 足够定位
  - **如果需要**: 扩展 DTO + 修改 processPaySuccess 签名
