# Implementation Plan: 售后/退款闭环

**Branch**: `004-refund-closed-loop` | **Date**: 2026-06-24 | **Spec**: specs/004-refund-closed-loop/spec.md

**Input**: Feature specification from `specs/004-refund-closed-loop/spec.md`

## Summary

社区电商退款闭环：补齐支付侧退款成功 → 订单侧状态同步的缺失链路。支付侧 `refund()` 和 `handleRefundCallback()` 已实现，但回调后未通知订单模块。需要新增退款 MQ 主题、订单侧消费者、商家审核流水线、管理端退款页面。

## Technical Context

**Language/Version**: Java 8+ (Spring Boot 2.7.18)

**Primary Dependencies**:
- Spring Cloud Alibaba + MyBatis-Plus (ORM)
- RocketMQ (消息队列，已用 `order-pay-success` 主题)
- Redisson (分布式锁，已有 `payment:lock:` 前缀)
- WeChat Pay v3 (退款 API `POST /v3/refund/domestic/refunds`)
- Sentinel (Feign 熔断)

**Storage**: MySQL 5.7 (share-payment/share-order 库) + Redis Sentinel (Redisson)

**Testing**: JUnit 5 + Spring Boot Test (模块内已有测试目录但为空)

**Target Platform**: Windows 开发机 IDEA 启动微服务，VM CentOS 7 运行 Nacos/Redis/RocketMQ/MySQL

**Project Type**: Spring Cloud 微服务 (share-payment 9213 + share-order 9211 + share-merchant 9215)

**Performance Goals**: 退款状态同步延迟 < 5s (MQ 端到端)；补偿任务 2min 内兜底

**Constraints**: 订单状态变更必须通过 `OrderStatusServiceImpl.transition()` 走状态机 + 乐观锁；支付状态变更通过 `LambdaUpdateWrapper` + 条件更新保证幂等

**Scale/Scope**: 小区电商平台，单量级小

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 为模板占位符，无实际约束规则。门禁通过。

## 已有实现（代码级确认）

### 支付侧（share-payment 9213）— ✅ 已就绪

| 组件 | 状态 | 说明 |
|---|---|---|
| `PaymentInfoServiceImpl.refund()` | ✅ 完成 | 分布式锁 + 状态校验(PAID) + 调用微信退款API + 更新payment_status |
| `PaymentInfoServiceImpl.handleRefundCallback()` | ✅ 完成 | 验签解密 + 更新 `REFUNDING→REFUNDED` |
| `WxPayUtil.createRefund()` | ✅ 完成 | `POST /v3/refund/domestic/refunds`，含 notify_url |
| `PaymentInfoController@refund` | ✅ 完成 | `POST /api/v1/payment/refund`，permission=`payment:payment:refund` |
| `PaymentInfoController@refundCallback` | ✅ 完成 | `POST /api/v1/payment/refund/callback` |
| `PaymentStatus` 常量 | ✅ 完成 | `UNPAID=0, PAID=1, REFUNDED=2, REFUNDING=3` |

### 订单侧（share-order 9211）— 🟡 部分就绪

| 组件 | 状态 | 说明 |
|---|---|---|
| `OrderStatusServiceImpl.transition()` | ✅ 完成 | 支持 `AFTER_SALE(5)−REFUND→CANCELLED(4)` |
| `OrderOperateType.REFUND("7")` | ✅ 完成 | 已定义 REFUND 操作 |
| `PaymentSuccessConsumer` | ✅ 完成 | 监听 `order-pay-success`，可参照此模式 |
| `OrderInfo` Entity | ✅ 完成 | 含 `freightAmount`, `payStatus` 等字段 |
| `MqConstants` | ✅ 完成 | 有 `PAYMENT_SUCCESS_TOPIC`，需新增退款主题 |
| `IOrderInfoService.processPaySuccess()` | ✅ 完成 | 支付成功处理，可参照实现 `processRefundSuccess()` |

### 已知缺口

| 缺口 | 影响 |
|---|---|
| ❌ `MqConstants` 无退款 MQ 主题 | 无法通过 MQ 通知订单侧 |
| ❌ `PaymentMQProducer` 无退款消息发送方法 | 同上 |
| ❌ 无 `PaymentRefundConsumer` (order 侧) | 订单侧收不到退款通知 |
| ❌ `handleRefundCallback()` 不发送 MQ | 退款成功后订单状态不同步 |
| ❌ `RemotePaymentService` 无 refund Feign | 商家/订单模块无法 Feign 调用退款 |
| ❌ `payment_info` 表无 `refund_status`/`refund_amount` 字段 | 付款信息页面无法展示退款详情 |
| ❌ 无 `after_sale_request` 表 | 用户售后申请无存储 |
| ❌ 商家审核流程无实现 | US2 需要商家同意→退款或客服介入 |
| ❌ 管理端无退款操作页面 | 当前仅 raw API |

## 设计决策

### 决策 1: MQ 还是 Feign？

**结论: MQ**

退款成功是异步事件（微信回调），与支付成功模式一致，复用 `PaymentMQProducer` 风格。

- MQ topic: `order-refund-success`
- Consumer group: `share-order-refund-consumer`
- DTO: `RefundSuccessMessage` (orderNo, transactionId, refundAmount)

### 决策 2: 订单状态转换路径

| 场景 | 路径 | 说明 |
|---|---|---|
| US1 管理员直接退款 | `COMPLETED(3) → CANCELLED(4)` | 需在状态机加 COMPLETED 的 REFUND 操作 |
| US2 用户申请→商家同意 | `COMPLETED(3) → AFTER_SALE(5) → CANCELLED(4)` | 现有状态机支持 |
| 客服介入强制退款 | 同上，操作人=客服 | 复用 REFUND 操作 |

### 决策 3: 退款金额

- 全额退款: `totalAmount + freightAmount`，优惠券保留
- 部分退款: 管理员指定金额
- 商品不退（社区电商模式）

## Project Structure

### Documentation (this feature)

```text
specs/004-refund-closed-loop/
├── plan.md                    # This file
├── spec.md                    # Feature spec
├── research.md                # Phase 0 research
├── data-model.md              # Phase 1 data model
├── quickstart.md              # Phase 1 validation guide
├── contracts/
│   └── interfaces.md          # Feign + MQ contracts
├── checklists/
│   └── refund-quality.md      # Quality checklist
└── tasks.md                   # Phase 2 (generated later)
```

### Source Code

```text
share-payment/                          # 支付模块 9213
├── src/main/java/com/share/payment/
│   ├── mq/
│   │   └── PaymentMQProducer.java      # [MODIFY] 新增 sendRefundSuccessMessage()
│   ├── domain/dto/
│   │   └── RefundSuccessMessage.java   # [NEW] 退款成功 MQ DTO
│   └── service/impl/
│       └── PaymentInfoServiceImpl.java # [MODIFY] handleRefundCallback() 发 MQ
│
share-order/                            # 订单模块 9211
├── src/main/java/com/share/order/
│   ├── consumer/
│   │   └── PaymentRefundConsumer.java  # [NEW] 退款成功消费者
│   ├── service/
│   │   ├── IOrderInfoService.java      # [MODIFY] 新增 processRefundSuccess()
│   │   └── impl/
│   │       └── OrderInfoServiceImpl.java # [MODIFY] 实现退款处理逻辑
│   └── service/impl/
│       └── OrderStatusServiceImpl.java # [MODIFY] COMPLETED 加 REFUND 操作
│
share-common-core/                      # 公共常量
├── src/main/java/com/share/common/core/constant/
│   └── MqConstants.java                # [MODIFY] 新增 REFUND_SUCCESS_TOPIC
│
share-api-payment/                      # Feign 接口
└── src/main/java/com/share/payment/api/
    └── RemotePaymentService.java       # [MODIFY] 可选新增 refund Feign
```

## 分阶段任务

### Phase 0: 研究确认 ✅（本文件已完成）

### Phase 1: 设计产出

1. `data-model.md` — after_sale_request 表结构 + order_info 新增字段
2. `contracts/interfaces.md` — MQ topic + Feign 接口 + 退款状态码
3. `quickstart.md` — 验证流程

### Phase 2: 实现（通过 `/speckit.tasks` 生成）

1. MqConstants 新增 `REFUND_SUCCESS_TOPIC`
2. 新增 `RefundSuccessMessage` DTO
3. `PaymentMQProducer` 新增 `sendRefundSuccessMessage()`
4. `PaymentInfoServiceImpl.handleRefundCallback()` 发送退款 MQ
5. `OrderStatusServiceImpl` 扩展 COMPLETED 状态支持 REFUND 操作
6. 新增 `PaymentRefundConsumer` (order 侧)
7. `OrderInfoServiceImpl` 新增 `processRefundSuccess()`
8. 新增 `after_sale_request` 表 + Entity + Mapper + Service
9. 商家审核 Service + Controller
10. 售后申请流程 (US2 用户申请→商家审核→客服介入)
11. 管理端退款页面
12. 补偿任务：扫描已退款但订单未同步记录

## Complexity Tracking

无违规模块需要合理化。
