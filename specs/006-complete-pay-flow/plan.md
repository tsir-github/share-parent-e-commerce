# Implementation Plan: 完成微信支付全链路集成

**Created**: 2026-06-26 | **Spec**: specs/006-complete-pay-flow/spec.md

## Summary

完成 share-payment 微信支付 v3 对接的收尾工作。经代码审计发现：

- **Refund MQ 消费端**（原 spec P1）：`PaymentRefundConsumer` 已存在于 share-order（`com.share.order.consumer`），监听 `order-refund-success` topic，调用 `processRefundSuccess()`，幂等 + 异常不重抛。**无需新建**。
- **Nacos 配置修正**（P2）：`share-payment-dev.yml` 中 MySQL 驱动类为旧版 `com.mysql.jdbc.Driver`，host 为 `localhost`，需改为 `com.mysql.cj.jdbc.Driver` + `192.168.10.129`。
- **编译验证**：修正后编译验证 + Nacos 推送。

## Technical Context

**Language/Version**: Java 8（JDK 1.8），Spring Boot 2.7.x

**Primary Dependencies**: Spring Cloud Alibaba, MyBatis-Plus, RocketMQ-Spring, Sentinel, Seata

**Storage**: MySQL 5.7.38（VM 192.168.10.129:3306），Redis Sentinel（VM）

**MQ**: RocketMQ 4.9.7（VM 192.168.10.129:9876），topic `order-refund-success` 已定义

**Testing**: 无单元测试——功能验证通过 mock 模式启动后手动触发退款流程

**Target Platform**: Windows 本地 IDEA 启动微服务，VM 运行中间件

**Project Type**: Spring Cloud Alibaba 微服务（share-payment 模块骨架已搭建）

**Performance Goals**: N/A（收尾修复，不涉及性能优化）

**Constraints**: 
- Nacos 配置修改后必须手动推送
- 配置推送前需确认 Nacos 可达（192.168.10.129:8848）
- MySQL 连接器为 `mysql-connector-j`（MySQL 8.x 新驱动），必须使用 `com.mysql.cj.jdbc.Driver`

**Scale/Scope**: 仅涉及 share-payment 模块的 Nacos 配置文件

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 文件为骨架模板（无实际约束），Gate 通过。

## Project Structure

### Documentation (this feature)

```text
specs/006-complete-pay-flow/
├── plan.md              # 本文档
├── research.md          # Phase 0: 代码分析结论
├── data-model.md        # Phase 1: 数据模型（无变化）
├── quickstart.md        # Phase 1: 验证指南
├── contracts/           # Phase 1: 接口合约（无变化）
└── tasks.md             # Phase 2: 任务分解
```

### Source Code（涉及文件）

```text
middleware/nacos-configs/share-payment-dev.yml    # 需修改：MySQL 驱动类 + host
share-modules/share-order/src/main/java/com/share/order/consumer/
├── PaymentRefundConsumer.java                    # ✅ 已存在，无需修改
```

## Complexity Tracking

无需（无违反约束项）

## Phase 0: Research Plan

### 需确认事项

| # | 问题 | 状态 | 结论 |
|---|------|------|------|
| 1 | RefundSuccessConsumer 是否缺失？ | ✅ 已确认 | `PaymentRefundConsumer` 已存在于 share-order，实现完整 |
| 2 | MQ 消息体格式是否对齐？ | ✅ 已确认 | `RefundSuccessMessage`(orderNo/transactionId/refundAmount) 两端一致 |
| 3 | Nacos 配置问题 | ✅ 已确认 | `com.mysql.jdbc.Driver` → `com.mysql.cj.jdbc.Driver`，`localhost` → `192.168.10.129` |
| 4 | Nacos 可达性 | ⚠️ 待验证 | SSH 连接 timeout，需确认 VM 网络 |

### 代码审计结论（research.md 输出）

详见 `research.md`。
