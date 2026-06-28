# 退款闭环接口契约

## 1. MQ 消息契约

### 1.1 退款成功通知

```
Topic:      order-refund-success (MqConstants.REFUND_SUCCESS_TOPIC)
Producer:   share-payment (PaymentMQProducer.sendRefundSuccessMessage)
Consumer:   share-order (RefundSuccessConsumer)
Group:      share-order-refund-consumer
DTO:        RefundSuccessMessage { orderNo, transactionId, refundAmount }
幂等:       order_info.pay_status = "2" 时跳过
```

**发送时机**：`handleRefundCallback()` 中退款状态变为 `REFUNDED` 时。

**消费者行为**：
1. 幂等检查：`orderInfo.payStatus == PAY_REFUNDED("2")` 则跳过
2. 调用 `OrderStatusServiceImpl.transition()`：AFTER_SALE(5) → REFUND → CANCELLED(4) 或 COMPLETED(3) → REFUND → CANCELLED(4)
3. 更新 `payStatus = PAY_REFUNDED("2")`
4. 更新 `refundAmount`、`refundTime`、`refundCount`
5. 异常捕获不重抛，补偿任务兜底

### 1.2 现有 MQ 主题（无变更）

| Topic | 用途 | Producer | Consumer |
|---|---|---|---|
| `order-pay-success` | 支付成功 | share-payment | share-order |
| `order-timeout-cancel` | 订单超时取消 | share-order | share-order |
| `order-auto-confirm` | 自动确认收货 | share-order | share-order |

## 2. API 接口契约

### 2.1 支付侧现有端点（复用）

| 方法 | URI | 鉴权 | 用途 |
|---|---|---|---|
| POST | `/api/v1/payment/refund?orderNo&amount&reason` | `payment:payment:refund` | 发起退款 |
| POST | `/api/v1/payment/refund/callback` | 微信回调 | 退款回调通知 |

### 2.2 订单侧新增端点

#### 商家审核售后

```
POST /api/v1/order/after-sale/audit
Request:  { id, auditStatus, auditRemark }
鉴权:     @RequiresPermissions("order:after-sale:audit")
用途:     商家对售后申请进行审核（同意/拒绝/请求客服介入）
Response: R.ok()
```

#### 客服处理售后

```
POST /api/v1/order/after-sale/admin-audit
Request:  { id, auditStatus, auditRemark }
鉴权:     @RequiresPermissions("order:after-sale:admin-audit")
用途:     平台管理员对售后申请进行最终审核
Response: R.ok()
```

#### 用户申请售后

```
POST /api/v1/order/after-sale/apply
Request:  { orderNo, refundReason }
鉴权:     @RequiresLogin
用途:     用户对已完成订单发起售后申请
Response: R.ok({ id, orderNo, auditStatus })
```

#### 用户查询售后进度

```
GET /api/v1/order/after-sale/status?orderNo=xxx
鉴权:     @RequiresLogin
用途:     用户查看售后申请的审核状态和进展
Response: R.ok(AfterSaleRequestVO)
```

### 2.3 管理端现有支付退款接口

| 方法 | URI | 鉴权 | 用途 |
|---|---|---|---|
| POST | `/api/v1/payment/refund?orderNo&amount&reason` | `payment:payment:refund` | 管理员在支付模块直接发起退款 |
| GET | `/api/v1/payment/list` | `payment:payment:list` | 查看支付记录列表 |

### 2.4 Feign 接口

```java
// RemotePaymentService 新增（可选）
@FeignClient(contextId = "remotePaymentService",
             value = ServiceNameConstants.PAYMENT_SERVICE,
             fallbackFactory = RemotePaymentFallbackFactory.class)
public interface RemotePaymentService {
    // 已有
    @GetMapping("/inner/payment/status/{orderNo}")
    R<Integer> getPaymentStatus(@PathVariable("orderNo") String orderNo,
                                @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    // 新增：发起退款
    @PostMapping("/inner/payment/refund")
    R<Void> refund(@RequestBody RefundRequest request,
                   @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}

// RefundRequest DTO
@Data
public class RefundRequest implements Serializable {
    private String orderNo;
    private BigDecimal amount;
    private String reason;
}
```

## 3. 补偿机制

### 3.1 退款补偿任务

参考 `PaymentCompensationTask` 模式，在 share-order 新增 `RefundCompensationTask`：

```java
@Component
@Slf4j
public class RefundCompensationTask {
    // 每分钟扫描：payment_status=REFUNDED 但 order.pay_status!="2" 的记录
    // Redisson 分布式锁防止多实例
    // 通过 Feign 查询 payment 状态，若已退款则重新触发 processRefundSuccess
}
```

## 4. 幂等性保证

| 操作 | 幂等策略 |
|---|---|
| 用户申请售后 | unique key (order_no, user_id, audit_status in [0,3]) 或查重 |
| 商家审核 | after_sale_request.id 唯一，状态机校验（仅 0→1/2/3） |
| 退款 MQ 消费 | order.payStatus == "2" 时跳过 |
| 微信退款回调 | payment.paymentStatus == REFUNDED 时跳过 |
| 客服强制操作 | after_sale_request.id + 状态校验（仅 0/3→4/5） |
