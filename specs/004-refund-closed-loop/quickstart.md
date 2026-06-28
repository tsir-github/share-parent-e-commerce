# 退款闭环快速验证指南

## 前置条件

- Nacos、Redis Sentinel、RocketMQ 已在 VM 运行
- share-payment(9213)、share-order(9211) 已在 IDEA 启动
- Nacos 配置 `share-payment-dev.yml` 和 `share-order-dev.yml` 已推送
- mock-mode=true（开发模式，跳过真实微信支付）

## 验证场景

### 场景 1: 管理员直接退款（US1 核心流程）

```bash
# 1. 准备：创建支付单 → 模拟支付成功
curl -X POST "http://localhost:9213/api/v1/payment/create?orderNo=TEST001&amount=100.00&description=测试商品&openid=mock_openid"
curl -X POST "http://localhost:9213/api/v1/payment/mock/callback?orderNo=TEST001"

# 2. 验证：支付状态应为 PAID(1)
curl "http://localhost:9213/inner/payment/status/TEST001"
# → {"code":200,"data":1}

# 3. 模拟微信退款回调（绕过退款 API 直接走到回调）
# 直接调 refund + 手动模拟回调（mock-mode 下 refund() 直接设 REFUNDED）
curl -X POST "http://localhost:9213/api/v1/payment/refund?orderNo=TEST001&amount=100.00&reason=测试退款"

# 4. 验证：支付状态应为 REFUNDED(2)
curl "http://localhost:9213/inner/payment/status/TEST001"
# → {"code":200,"data":2}
```

### 场景 2: 用户申请售后 → 商家审核 → 退款（US2 流程）

```bash
# 1. 用户申请售后
curl -X POST "http://localhost:9211/api/v1/order/after-sale/apply" \
  -H "Content-Type: application/json" \
  -d '{"orderNo":"TEST002","refundReason":"商品不满意"}'
# → {"code":200,"data":{"id":1,"orderNo":"TEST002","auditStatus":"0"}}

# 2. 查询售后进度
curl "http://localhost:9211/api/v1/order/after-sale/status?orderNo=TEST002"
# → {"code":200,"data":{"auditStatus":"0","refundReason":"商品不满意"}}

# 3. 商家审核同意
curl -X POST "http://localhost:9211/api/v1/order/after-sale/audit" \
  -H "Content-Type: application/json" \
  -d '{"id":1,"auditStatus":"1","auditRemark":"同意退款"}'
# → {"code":200}
# 触发：调用 paymentInfoService.refund() → 支付状态更新 → handleRefundCallback
#     → MQ 通知订单侧 → order status: CANCELLED(4), pay_status: REFUNDED("2")
```

### 场景 3: 商家拒绝 → 客服介入

```bash
# 1. 商家拒绝
curl -X POST "http://localhost:9211/api/v1/order/after-sale/audit" \
  -d '{"id":2,"auditStatus":"2","auditRemark":"已发货不可退"}'

# 2. 客服介入（商家拒绝后自动或手动）
curl -X POST "http://localhost:9211/api/v1/order/after-sale/admin-audit" \
  -d '{"id":2,"auditStatus":"4","auditRemark":"客服判定退款"}'
# → 触发退款流程
```

### 场景 4: MQ 降级（RocketMQ 未配置）

当 Nacos 中 `rocketmq.name-server` 未配置时：
- `PaymentMQProducer` 以 `required=false` 注入
- `sendRefundSuccessMessage()` 仅打日志不抛异常
- 补偿任务每分钟扫描未同步记录并重新发送

```bash
# 关闭 RocketMQ 验证降级：直接调退款
curl -X POST "http://localhost:9213/api/v1/payment/refund?orderNo=TEST003&amount=50.00&reason=MQ降级测试"
# → 控制台应有 "RocketMQ 未配置，跳过 MQ 发送" 日志
# → 补偿任务在 1min 内应兜底处理
```

## 验证检查清单

| 检查项 | 预期 | 验证方法 |
|---|---|---|
| 支付状态更新 | REFUNDED(2) | `GET /inner/payment/status/{orderNo}` |
| 订单状态更新 | CANCELLED("4") | 查数据库 `order_info.status` |
| 订单支付状态 | PAY_REFUNDED("2") | 查数据库 `order_info.pay_status` |
| 操作日志记录 | 有 REFUND 操作记录 | 查 `order_log` 表 |
| 幂等性 | 重复 MQ 不产生副作用 | 发两次相同消息，状态无变化 |
| 商家审核 | after_sale_request 状态更新 | 查 `after_sale_request.audit_status` |

## 数据库验证 SQL

```sql
-- 查看订单退款状态
SELECT order_no, status, pay_status, refund_amount, refund_time, refund_count
FROM share-order.order_info
WHERE order_no IN ('TEST001', 'TEST002');

-- 查看支付退款状态
SELECT order_no, payment_status, amount
FROM share-payment.payment_info
WHERE order_no IN ('TEST001', 'TEST002');

-- 查看售后申请记录
SELECT * FROM share-order.after_sale_request
WHERE order_no IN ('TEST001', 'TEST002');

-- 查看订单操作日志
SELECT * FROM share-order.order_log
WHERE order_no IN ('TEST001', 'TEST002')
ORDER BY create_time DESC;
```
