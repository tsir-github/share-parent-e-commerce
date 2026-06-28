# 验证指南 — 006-complete-pay-flow

## 前置条件

1. VM 中间件运行正常：
   - Nacos `192.168.10.129:8848`
   - MySQL `192.168.10.129:3306`
   - RocketMQ `192.168.10.129:9876`
   - Redis Sentinel `192.168.10.129:26379-26381`

2. Nacos 配置已推送（见下文）

## 步骤 1：修正 Nacos 配置

修改 `middleware/nacos-configs/share-payment-dev.yml`：

```yaml
# 修改前
driver-class-name: com.mysql.jdbc.Driver
url: jdbc:mysql://localhost:3306/share-payment?characterEncoding=utf-8&useSSL=false

# 修改后
driver-class-name: com.mysql.cj.jdbc.Driver
url: jdbc:mysql://192.168.10.129:3306/share-payment?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
```

推送至 Nacos：
```bash
curl -X POST "http://192.168.10.129:8848/nacos/v1/cs/configs" \
  -d "dataId=share-payment-dev.yml&group=DEFAULT_GROUP&tenant=a746e297-417e-4aec-bfb1-e42df33fbe93&content=$(cat middleware/nacos-configs/share-payment-dev.yml | jq -sRr @uri)"
```

或通过 Nacos Web UI → 配置管理 → 命名空间 `a746e297...` → share-payment-dev.yml → 编辑 → 发布。

## 步骤 2：编译验证

```bash
mvn clean compile -pl share-modules/share-payment -am
```

## 步骤 3：模块启动

IDEA 中启动 `SharePaymentApplication.main()`，观察日志：
- ✅ 数据库连接成功（无 `com.mysql.jdbc.Driver` 警告）
- ✅ 注册到 Nacos（`share-payment` service）
- ✅ RocketMQ 生产者初始化

## 步骤 4：退款链路验证（Mock 模式）

### 前置条件
- `share-payment-dev.yml` 中 `wx.pay.v3.mock-mode: true`
- share-auth / share-order / share-payment 均已启动

### 验证流程

1. 模拟一笔支付完成（调用 order 支付接口，或直接调 payment mock API）
2. 验证 `share-order` 收到 `order-pay-success` → 订单状态变更为已支付
3. 申请售后/退款（调用 payment refund mock API）
4. 验证 `share-order` `PaymentRefundConsumer` 收到 `order-refund-success` MQ
5. 验证订单状态 `status` 变更为 `CANCELLED`（全额）或 `payStatus` 变更为 `PAY_REFUNDED`

### 关键日志

```
share-order 侧：
[PaymentRefundConsumer] 收到退款成功消息: orderNo=xxx, transactionId=xxx, refundAmount=xxx

有异常时：
[PaymentRefundConsumer] 处理退款成功消息异常: orderNo=xxx (异常堆栈)
```

## 预期结果

| 步骤 | 预期 | 验证方法 |
|------|------|----------|
| Nacos 配置推送 | 无错误返回，dataId 列表可见 | Nacos UI / curl |
| 编译 | BUILD SUCCESS | mvn exit code 0 |
| 模块启动 | 无异常，服务注册成功 | 控制台日志 |
| Mock 退款 | 订单状态正确变更 | 查 DB / 日志 |
