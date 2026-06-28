# Tasks: 售后/退款闭环 (004-refund-closed-loop)

**Input**: Design documents from `/specs/004-refund-closed-loop/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), data-model.md, contracts/interfaces.md

**Tests**: Not requested in spec — test tasks excluded.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **share-payment**: `share-modules/share-payment/src/main/java/com/share/payment/`
- **share-order**: `share-modules/share-order/src/main/java/com/share/order/`
- **share-api-payment**: `share-api/share-api-payment/src/main/java/com/share/payment/`
- **share-common-core**: `share-common/share-common-core/src/main/java/com/share/common/core/`

---

## Phase 1: Setup (Shared MQ Infrastructure)

**Purpose**: 新增退款 MQ 消息基础设施 — topic 常量、DTO、生产者

- [ ] T001 在 `MqConstants.java` 新增 `String REFUND_SUCCESS_TOPIC = "order-refund-success";` — 文件: `share-common/share-common-core/src/main/java/com/share/common/core/constant/MqConstants.java`
- [ ] T002 [P] 新增 `RefundSuccessMessage` DTO（含 orderNo, transactionId, refundAmount），参考 `PaymentSuccessMessage` 模式 — 文件: `share-api/share-api-payment/src/main/java/com/share/payment/domain/dto/RefundSuccessMessage.java`
- [ ] T003 [P] 在 `PaymentMQProducer` 新增 `sendRefundSuccessMessage(RefundSuccessMessage)` 方法，参照 `sendPaySuccessMessage()` 实现，支持 `required=false` 降级 — 文件: `share-modules/share-payment/src/main/java/com/share/payment/mq/PaymentMQProducer.java`

---

## Phase 2: Foundational (Core Refund Processing)

**Purpose**: 核心退款链路 — 支付侧发 MQ + 订单侧收 MQ + 状态机扩展 + 数据层

**⚠️ CRITICAL**: 此阶段完成后，US1 和 US2 才能实现独立退款处理

- [ ] T004 修改 `PaymentInfoServiceImpl.handleRefundCallback()`：在退款状态变为 `REFUNDED` 后调用 `paymentMQProducer.sendRefundSuccessMessage()` 发送退款 MQ — 文件: `share-modules/share-payment/src/main/java/com/share/payment/service/impl/PaymentInfoServiceImpl.java`
- [ ] T005 [P] 扩展 `OrderStatusServiceImpl`：在 COMPLETED(3) 状态配置 REFUND 操作路由，支持 COMPLETED → REFUND → CANCELLED(4) — 文件: `share-modules/share-order/src/main/java/com/share/order/service/impl/OrderStatusServiceImpl.java`
- [ ] T006 [P] 在 `OrderInfoServiceImpl` 新增 `processRefundSuccess(RefundSuccessMessage)` 方法：更新 payStatus=PAY_REFUNDED("2")、refundAmount、refundTime、refundCount — 文件: `share-modules/share-order/src/main/java/com/share/order/service/impl/OrderInfoServiceImpl.java`
- [ ] T007 [P] 新增 `PaymentRefundConsumer`：监听 `order-refund-success` topic，幂等检查后调 `orderInfoService.processRefundSuccess()` + `orderStatusService.transition()`，异常不重抛 — 文件: `share-modules/share-order/src/main/java/com/share/order/consumer/PaymentRefundConsumer.java`
- [ ] T008 [P] 执行 `after_sale_request` 建表 DDL（见 data-model.md），创建 `AfterSaleRequest` Entity（extends BaseEntity）+ Mapper 接口 — 共享 Entity 在 `share-order` 模块
- [ ] T009 [P] 执行 `order_info` ALTER TABLE 新增 `refund_amount`/`refund_time`/`refund_count` 字段（见 data-model.md），更新 `OrderInfo.java` Entity 新增对应字段 — 文件: `share-order` 模块
- [ ] T010 [P] 执行 `payment_info` ALTER TABLE 新增 `refund_amount`/`refund_time` 字段（可选，见 data-model.md）— 文件: `share-payment` 模块

**Checkpoint**: 核心退款链路就绪 — 支付侧退款成功后可发 MQ，订单侧可接收并更新状态

---

## Phase 3: User Story 1 - 管理员发起退款并同步订单状态 (Priority: P1) 🎯 MVP

**Goal**: 商家/管理员可通过审核流程对已完成订单发起退款，系统同步更新订单状态和支付状态

**Independent Test**: 管理员审核售后申请 → 系统调退款 → 支付状态变为 REFUNDED → 订单状态变为 CANCELLED

### Implementation for User Story 1

- [ ] T011 [US1] 新增 `IAfterSaleRequestService` + `AfterSaleRequestServiceImpl`：实现商家审核（agree/reject/toAdmin）、客服审核（adminAgree/adminReject）、状态机校验 — 文件: `share-modules/share-order/src/main/java/com/share/order/service/`
- [ ] T012 [US1] 新增 `AfterSaleRequestController`：`POST /api/v1/order/after-sale/audit`（商家审核）+ `POST /api/v1/order/after-sale/admin-audit`（客服审核），权限 `order:after-sale:audit` / `order:after-sale:admin-audit` — 文件: `share-modules/share-order/src/main/java/com/share/order/controller/`
- [ ] T013 [US1] 在 `RemotePaymentService` 新增 `refund()` Feign 接口（含 `RefundRequest` DTO），在商家审核同意时调用支付侧发起退款 — 文件: `share-api/share-api-payment/src/main/java/com/share/payment/api/RemotePaymentService.java`

**Checkpoint**: US1 完整链路可用 — 商家审核 → 支付退款 → MQ 通知 → 订单状态同步

---

## Phase 4: User Story 2 - 用户申请售后并查看进度 (Priority: P2)

**Goal**: 用户对已完成订单发起售后申请，查看审核进度；商家审核 → 客服兜底

**Independent Test**: 用户申请售后 → 商家审核同意 → 系统退款 → 用户查看进度已退款

### Implementation for User Story 2

- [ ] T014 [US2] 在 `AfterSaleRequestServiceImpl` 新增 `apply()` 方法：幂等校验（同订单未完成的申请重入检查）、创建 `after_sale_request` 记录、调用 `orderStatusService.transition()` 变更订单为 AFTER_SALE(5) — 文件: `share-modules/share-order/src/main/java/com/share/order/service/impl/AfterSaleRequestServiceImpl.java`
- [ ] T015 [US2] 新增 `AfterSaleRequestVO`：封装售后进度查询返回字段 — 文件: `share-modules/share-order/src/main/java/com/share/order/domain/vo/AfterSaleRequestVO.java`
- [ ] T016 [US2] 在 `AfterSaleRequestController` 新增 `POST /api/v1/order/after-sale/apply`（用户申请，`@RequiresLogin`）+ `GET /api/v1/order/after-sale/status`（进度查询，`@RequiresLogin`）— 文件: `share-modules/share-order/src/main/java/com/share/order/controller/AfterSaleRequestController.java`

**Checkpoint**: US2 完整退款流程可用 — 用户申请 → 商家审核 → 退款 → 进度查询

---

## Phase 5: User Story 3 - 部分退款场景 (Priority: P3)

**Goal**: 管理员对同一订单进行部分退款，记录累计退款金额，订单主状态保持

**Independent Test**: 管理员对订单部分退款 → 支付侧记录部分退款明细 → 订单 `refund_amount` 累加

### Implementation for User Story 3

- [ ] T017 [US3] 在 `AfterSaleRequestServiceImpl` 新增部分退款校验逻辑：`refundAmount <= payAmount - existingRefundAmount`，超额拒绝 — 文件: `share-modules/share-order/src/main/java/com/share/order/service/impl/AfterSaleRequestServiceImpl.java`
- [ ] T018 [US3] 修改 `processRefundSuccess()` 支持增量退款：`refundAmount` 累加而非覆盖，payStatus 在部分退款时保持 PAY_PAID("1") — 文件: `share-modules/share-order/src/main/java/com/share/order/service/impl/OrderInfoServiceImpl.java`
- [ ] T019 [US3] 在 `processRefundSuccess()` 中判断当 `refundAmount >= payAmount` 时再变更订单主状态为 CANCELLED — 文件: `share-modules/share-order/src/main/java/com/share/order/service/impl/OrderInfoServiceImpl.java`

**Checkpoint**: 部分退款可正常执行，累计退款金额不超限

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 补偿机制 + 管理端集成 + 验证

- [ ] T020 [P] 新增 `RefundCompensationTask`：每分钟扫描 payment_status=REFUNDED 但 order.pay_status!="2" 的记录，通过 Feign 查询支付侧状态，已退款则重新触发 `processRefundSuccess()`，Redisson 分布式锁防止多实例 — 文件: `share-modules/share-order/src/main/java/com/share/order/task/RefundCompensationTask.java`
- [ ] T021 管理端退款页面：在订单详情页增加"退款"按钮 + 退款金额输入 + 审核表单（基于 RuoYi Element Plus 组件库）— 文件: `share-ui` 前端项目
- [ ] T022 运行 quickstart.md 验证 4 个场景 —— 管理员退款、用户售后、客服介入、MQ 降级
- [ ] T023 更新 `AGENTS.md` SPECKIT 区块标记 004 实现完成

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational completion — No dependencies on other stories
- **US2 (Phase 4)**: Depends on Foundational completion — Shares `after_sale_request` table with US1
- **US3 (Phase 5)**: Depends on US1 (partial refund builds on refund infrastructure)
- **Polish (Phase 6)**: Depends on US1+US2 completion

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **US2 (P2)**: Can start after Foundational (Phase 2) — Independent from US1 (different endpoints)
- **US3 (P3)**: Depends on US1 foundational infrastructure

### Within Each Phase

- Setup tasks T002+T003 marked [P] can run in parallel
- Foundational tasks T005+T006+T007+T008+T009+T010 marked [P] can run in parallel
- Within US1: T011 (service) → T012 (controller) + T013 (Feign) can be parallel
- Within US2: T014+T015 (service+VO) → T016 (controller)
- Within US3: sequential (all modify same service)

### Parallel Opportunities

- T002 (DTO) and T003 (Producer) can be done simultaneously
- T005 (status machine), T006 (processRefundSuccess), T007 (consumer), T008 (after_sale_request table), T009 (order_info ALTER), T010 (payment_info ALTER) all independent
- US1 and US2 implementation phases can be done in parallel by different developers

---

## Parallel Example: Phase 1 + Phase 2 Kickoff

```bash
# Setup tasks in parallel:
Task: "T001 MqConstants" + "T002 DTO" + "T003 Producer"

# Foundational tasks in parallel (after Setup done):
Task: "T005 status machine" + "T006 processRefundSuccess" + "T007 consumer"
Task: "T008 after_sale_request table" + "T009 order_info ALTER" + "T010 payment_info ALTER"
```

---

## Implementation Strategy

### MVP First (Phase 1 → 2 → 3 = US1 Only)

1. Complete Phase 1: Setup (MQ infra)
2. Complete Phase 2: Foundational (core logic + data layer)
3. Complete Phase 3: US1 (admin refund flow)
4. **STOP and VALIDATE**: Run quickstart.md Scenario 1
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Core refund pipeline ready
2. Add US1 → Admin refund flow → Deploy (MVP!)
3. Add US2 → User after-sale flow → Deploy
4. Add US3 → Partial refund → Deploy
5. Polish → Compensation + Admin UI

### Parallel Team Strategy

With multiple developers:
- Dev A: Phase 1 (MQ infra)
- Dev B: Phase 2 Foundation tasks in parallel (status machine, consumer, data layer)
- After Phase 2 done: Dev A takes US1, Dev B takes US2

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Each story is independently testable
- Order status changes MUST go through `OrderStatusServiceImpl.transition()` — never direct `updateById`
- Payment status changes use `LambdaUpdateWrapper.set()` + conditions for idempotency
- MQ consumer uses `required=false` injection for graceful RocketMQ degradation
- Commit after each task or logical group
- Run `lsp_diagnostics` after each file change
