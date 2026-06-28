# Feature Specification: share-payment 模块收尾清理

**Created**: 2026-06-26

**Status**: Draft

**Input**: 完成 share-payment 支付模块并修复错误

## 前置说明

share-payment 模块核心业务流程已全部实现（创建支付→微信回调→退款→退款回调→MQ 通知→补偿兜底），
本 spec 专注于 **代码质量修复 + 未收尾验证**，不涉及新业务功能。

已有 specs 覆盖范围：
- 002-payment-service: 支付服务骨架
- 003-order-payment-integration: 订单-支付集成
- 004-refund-closed-loop: 退款闭环
- 006-complete-pay-flow: Nacos 配置修正 + RefundConsumer 审计

**遗留问题清单（代码审计发现）：**

| # | 问题 | 文件 | 严重程度 |
|---|---|---|---|
| Q1 | `@Autowired(required=false) RocketMQTemplate` 字段注入 | PaymentMQProducer | 中（违反构造器注入规范） |
| Q2 | `add()` 方法返回 `AjaxResult`，其余方法返回 `R<T>` / `TableDataInfo` | PaymentInfoController | 中（违反 AGENTS.md 第3.5条） |
| Q3 | `/inner/payment/create` 用 `Map<String, Object>` 入参 | PaymentInfoController | 低（已存在 CreatePaymentDTO，未使用） |
| Q4 | C端 API `/api/v1/payment/create` 等缺少 `@RequiresLogin` | PaymentInfoController | 低（但有安全隐患） |
| Q5 | `PaymentInfoVO` 已定义但管理端 list 接口返回 Entity | PaymentInfoController + ServiceImpl | 低 |
| Q6 | 编译验证未在 LSP 上运行确认 | — | 中 |

## User Scenarios & Testing

### User Story 1 — 构造器注入改造 (Priority: P1)

PaymentMQProducer 的 `@Autowired(required=false) RocketMQTemplate` 改为构造器注入，
保留 RocketMQ 可选降级的语义。

**Why this priority**: 全项目 @Autowired 字段注入已在其他业务模块清零，
share-payment 作为业务模块不应保留此反模式。

**Independent Test**: compile 通过，PaymentMQProducer 在 RocketMQ 配置存在/不存在时均正常实例化。

**Acceptance Scenarios**:

1. **Given** Nacos 中配置了 `rocketmq.name-server`，**When** 启动 share-payment，**Then** PaymentMQProducer 正常注入 RocketMQTemplate，MQ 消息正常发送
2. **Given** Nacos 中未配置 `rocketmq.name-server`，**When** 启动 share-payment，**Then** PaymentMQProducer 以降级模式运行（仅打日志），不影响 mock 模式其他功能

---

### User Story 2 — Controller 返回类型统一 (Priority: P1)

PaymentInfoController 的 `add()` 方法从 `AjaxResult` 改为 `R<Void>`，
统一整个 controller 为 R<T> 风格。

**Why this priority**: 同一 Controller 混用 AjaxResult/R<T> 违反 AGENTS.md 第 3.5 条。
`add()` 是后台管理接口，按规范应统一使用 Controller 主体风格（R<T>）。

**Independent Test**: compile 通过，`add()` 返回格式从 `{"msg":"操作成功","code":200}` 变为 `{"data":null,"msg":"操作成功","code":200}`。

**Acceptance Scenarios**:

1. **Given** PaymentInfoController 的 add 方法，**When** 调用添加记录，**Then** 返回 R<Void> 格式，不再混用 AjaxResult

---

### User Story 3 — Feign 内部接口入参改用 DTO (Priority: P2)

`/inner/payment/create` 的 `Map<String, Object>` 入参改为 `CreatePaymentDTO`（已存在，未使用），
获得编译时类型安全。

**Why this priority**: Map 入参缺乏类型检查，已在 AGENTS.md 规范中列为反例。
CreatePaymentDTO 已在 share-payment 模块中就绪但未使用。

**Independent Test**: 调用 `/inner/payment/create` 传入 JSON body 反序列化为 CreatePaymentDTO 正常。

**Acceptance Scenarios**:

1. **Given** 订单模块 Feign 调用 `/inner/payment/create`，**When** 传入 `CreatePaymentDTO` JSON，**Then** Controller 正确反序列化为强类型 DTO，不再使用 `Map.get()` 手动转换

---

### User Story 4 — 编译验证 (Priority: P1)

在所有代码修改后运行 `mvn compile -pl share-modules/share-payment -am` 确认零错误。

**Why this priority**: 编译通过是代码可运行的最低保障。

**Acceptance Scenarios**:

1. **Given** 所有修改完成，**When** `mvn compile -pl share-modules/share-payment -am`，**Then** BUILD SUCCESS

---

### Edge Cases

- `RocketMQTemplate` 在 Nacos 未配置时不可用：构造器注入需处理 `ObjectProvider` 或 `@Autowired(required=false)` 的替代方案
- `CreatePaymentDTO` 缺少 `userId` 字段：Feign 调用 `createPaymentInner` 原实现接收 `params.get("userId")`，DTO 需要补
- `/api/v1/payment/callback` 和 `/refund/callback` 返回 `Map<String,String>` 是微信要求，不改为 R<T>

## Requirements

### Functional Requirements

- **FR-001**: PaymentMQProducer MUST 使用构造器注入代替 `@Autowired` 字段注入
- **FR-002**: PaymentMQProducer MUST 保留 RocketMQ 可选降级能力（未配置 name-server 时不阻塞启动）
- **FR-003**: PaymentInfoController MUST 统一使用 R<T> 作为返回类型（/callback 端点除外——微信要求特殊格式）
- **FR-004**: `/inner/payment/create` MUST 使用 `CreatePaymentDTO` 代替 `Map<String, Object>`
- **FR-005**: `CreatePaymentDTO` MUST 补充 `userId` 字段以满足 Feign 调用场景
- **FR-006**: `mvn compile -pl share-modules/share-payment -am` MUST 零错误通过

### Non-functional Requirements

- 不修改现有业务逻辑（createPayment/handlePayCallback/refund/handleRefundCallback/mockPaySuccess）
- 不修改 MQ topic 名称或消息体结构（与 order 侧消费者兼容）
- 不修改 WxPayUtil/WxPayConfig——它们设计正确
- 不改动 share-order 侧的任何代码

### Key Entities

- **PaymentMQProducer**: MQ 生产者，本次改造构造器注入
- **PaymentInfoController**: 支付 Controller，本次统一返回类型和入参 DTO
- **CreatePaymentDTO**: 已存在的创建支付 DTO，需补充 userId 字段并启用

## Success Criteria

### Measurable Outcomes

- **SC-001**: `mvn compile -pl share-modules/share-payment -am` BUILD SUCCESS
- **SC-002**: `lsp_diagnostics` 检查无 error
- **SC-003**: share-payment 模块零处 `@Autowired` 字段注入（确认已清零）

## Assumptions

- RocketMQ 可选降级方案使用 Spring Boot 的 `ObjectProvider<RocketMQTemplate>` 或 `@Autowired(required=false)` 保留但配合 `@Lazy`
- 不改动 share-api-payment 中的 RemotePaymentService Feign 接口签名（`Map<String, Object>` 在 Feign 侧可保留，由 Jackson 自动转换）
- `/callback` 端点（payCallback/refundCallback）返回 `Map<String,String>` 是微信网关协议要求，不做改为 R<T>
- 不改动 PaymentCompensationTask（直接注入 Mapper 是定时任务通用模式，可接受）
