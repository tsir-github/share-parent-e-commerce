# Feature Specification: 完成微信支付全链路集成

**Created**: 2026-06-26

**Status**: Draft

**Input**: 完成 share-payment 微信支付 v3 对接的收尾工作，包括退款 MQ 消费端、配置修正、编译验证。

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - 售后审核通过后退款状态同步 (Priority: P1)

商家在售后审核中同意退款后，share-order 能收到退款成功 MQ 消息并更新订单状态为已退款，确保退款状态在订单和支付两侧同步。

**Why this priority**: 退款闭环是整个支付流程的最后关键环节。当前 share-payment 发送 `order-refund-success` MQ，但 share-order 缺少对应 Consumer，导致退款成功时订单状态未更新。

**Independent Test**: 模拟一笔支付 → 发起退款 → 发送 mock MQ 消息 → 验证 order 侧收到后订单状态变为 REFUNDED。

**Acceptance Scenarios**:

1. **Given** 用户有一笔已支付订单并申请售后，**When** 商户同意退款，支付侧发起退款成功并发送 `order-refund-success` MQ，**Then** 订单侧消费者接收到消息后将订单状态更新为已退款
2. **Given** 订单侧收到重复的退款 MQ 消息，**When** 幂等检查发现订单已退款，**Then** 跳过处理，不抛异常

---

### User Story 2 - MQ 消息体格式对齐 (Priority: P1)

RefundSuccessMessage 的消息格式和 topic 与消费端对齐，确保消息能正常序列化/反序列化，订单侧正确解析退款金额和订单号。

**Why this priority**: 消息体字段不一致（字段名、类型不匹配）会导致消费端反序列化异常，退款状态无法同步。

**Independent Test**: 发送一条 RefundSuccessMessage MQ 消息，消费端正确解析 orderNo、transactionId、refundAmount 字段。

**Acceptance Scenarios**:

1. **Given** 支付侧发送 RefundSuccessMessage，**When** 订单侧 RocketMQListener 接收消息，**Then** 消息体中的 orderNo、transactionId、refundAmount 字段正确解析
2. **Given** RefundSuccessMessage 的 topic 为 `order-refund-success`，**When** 支付侧 producer 发送消息，**Then** 订单侧 Consumer 监听同一 topic

---

### User Story 3 - Nacos 配置环境就绪 (Priority: P2)

share-payment 的 Nacos 配置中的 MySQL 连接参数（驱动类、host）指向正确的生产环境值，确保模块启动时数据库连接正常。

**Why this priority**: 当前配置存在 MySQL 驱动用旧类名（com.mysql.jdbc.Driver）、host 用 localhost 的问题，需要修正才能正常连接数据库。

**Independent Test**: 从 Nacos 拉取 share-payment-dev.yml，检查 MySQL 驱动类和 host 正确。

**Acceptance Scenarios**:

1. **Given** share-payment-dev.yml 配置，**When** 检查 datasource 配置项，**Then** driver-class-name 为 `com.mysql.cj.jdbc.Driver`，url 指向 `192.168.10.129:3306/share-payment`
2. **Given** share-payment-dev.yml 配置，**When** 检查 wx.pay.v3 配置，**Then** mock-mode 为 true（开发阶段），证书路径不为空

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

- 退款 MQ 消息重复投递：消费端需幂等处理，已退款状态的订单跳过
- MQ 消费端处理异常：记录日志，不阻止 RocketMQ 重试（默认 16 次）
- 消息体字段变更：RefundSuccessMessage 字段修改需同步两端
- Nacos 配置修改后未推送：需手动 publish 到 Nacos

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: 订单侧 MUST 监听 `order-refund-success` topic 的 RocketMQ 消息
- **FR-002**: 订单侧 MUST 在收到退款成功消息后更新订单状态为 REFUNDED
- **FR-003**: 消费端 MUST 具备幂等性，对重复消息不做重复处理
- **FR-004**: 消费端 MUST 对处理异常进行日志记录，不阻止 RocketMQ 重试
- **FR-005**: Nacos 配置 MUST 使用 `com.mysql.cj.jdbc.Driver` 驱动类
- **FR-006**: Nacos 配置 MUST 将 MySQL host 指向 `192.168.10.129`

### Key Entities

- **RefundSuccessMessage**：退款成功 MQ 消息体，包含 orderNo（订单号）、transactionId（微信退款单号）、refundAmount（退款金额）
- **RefundSuccessConsumer**：订单侧的 RocketMQ 消费者，监听 `order-refund-success` topic
- **share-payment-dev.yml**：Nacos 配置，包含数据源、微信支付、Redis 等参数

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001**: 退款 MQ 发出后 30 秒内订单状态更新为已退款
- **SC-002**: 重复退款消息不导致订单状态异常或重复退款
- **SC-003**: mock 模式下完整走通：创建支付 → 支付成功 → 发起退款 → 退款成功 → 订单状态已退款
- **SC-004**: Nacos 配置修正后 share-payment 模块数据库连接正常

## Assumptions

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right assumptions based on reasonable defaults
  chosen when the feature description did not specify certain details.
-->

- RocketMQ `autoCreateTopicEnable=true`，无需手动创建 topic
- 开发阶段使用 mock-mode，不连接真实微信支付 API
- `MqConstants.REFUND_SUCCESS_TOPIC = "order-refund-success"` 已在 share-common-core 定义
- `IOrderInfoService.processRefundSuccess()` 方法已在 share-order 中实现
- Nacos `dev.yml` 中 share-payment 数据源配置需手动修正并推送
