# Tasks: 订单-支付流程打通

**Input**: Design documents from `specs/003-order-payment-integration/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/interfaces.md

**Tests**: No test tasks generated — not requested in spec, not TDD approach.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Verify existing code, extract hardcoded config, ensure prerequisites before user story work.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T001 [P] Read existing `processPaySuccess` in `share-order/.../service/impl/OrderInfoServiceImpl.java` — verify `payTime` is actually being set in the `LambdaUpdateWrapper`. If missing, add: `.set(OrderInfo::getPayTime, new Date())`.
- [ ] T002 [P] Read existing `PaymentSuccessConsumer` in `share-order/.../consumer/PaymentSuccessConsumer.java` — verify MQ consumer `@RocketMQMessageListener` config (topic, consumerGroup), confirm message deserialization and `processPaySuccess` call are correct.
- [ ] T003 [P] Read existing `OrderTimeoutConsumer` in `share-order/.../consumer/OrderTimeoutConsumer.java` — confirm hardcoded `delayLevel=16` location.
- [ ] T004 [P] Read existing `PaymentMQProducer` in `share-payment/.../mq/PaymentMQProducer.java` — verify `sendPaySuccessMessage` topic, message construction, error handling (non-blocking).
- [ ] T005 [P] Read existing `PaymentCompensationTask` in `share-payment/.../task/PaymentCompensationTask.java` — verify SQL scope, LIMIT, MQ resend logic.
- [ ] T006 [P] Read existing `PaymentInfoController` in `share-payment/.../controller/PaymentInfoController.java` — verify `/inner/payment/status/{orderNo}` endpoint exists and returns correct status.
- [ ] T007 [P] Read existing `RemotePaymentService` in `share-api/share-api-payment/.../api/RemotePaymentService.java` — verify current Feign methods, confirm whether `getPaymentStatus` needs to be added.
- [ ] T008 Extract hardcoded `delayLevel=16` in `OrderTimeoutConsumer.java` to Nacos config `order.pay.timeout-minutes`. Implement: (a) add `@Value("${order.pay.timeout-minutes:30}")` in consumer, (b) add config to `share-order-dev.yml` in Nacos, (c) compute delayLevel from minutes using RocketMQ `MessageDelayLevel` mapping (1s/5s/10s/30s/1m/2m/3m/4m/5m/6m/7m/8m/9m/10m/20m/30m/1h/2h — pick closest).
- [ ] T009 Create Nacos config entry `order.pay.timeout-minutes=30` in `share-order-dev.yml` and in local `bootstrap-dev.yml` fallback (if applicable).

**Checkpoint**: Foundation ready — all existing code verified, timeout config extractable. User story implementation can now proceed.

---

## Phase 2: User Story 1 — 下单支付完整流程 (Priority: P1) 🎯 MVP

**Goal**: Verify end-to-end order-pay flow works: createOrder → createPayment → mock callback → MQ → order status update.

**Independent Test**: Quickstart.md Step 1-5: createOrder → query pending payment → createPayment → mockCallback → verify order status=已支付.

- [ ] T010 [P] [US1] If `payTime` was missing in T001, update `OrderInfoServiceImpl.processPaySuccess()` to add `.set(OrderInfo::getPayTime, new Date())` in the `LambdaUpdateWrapper`.
- [ ] T011 [P] [US1] If `getPaymentStatus` Feign missing in T007, add it to `RemotePaymentService.java` and ensure `PaymentInfoController.java` has the corresponding `@InnerAuth` endpoint mapping to `/inner/payment/status/{orderNo}`.
- [ ] T012 [P] [US1] Verify `PaymentMQProducer` is properly called in `PaymentInfoController` or `PaymentInfoServiceImpl` on mock callback — ensure `sendPaySuccessMessage(orderNo, transactionId)` is invoked.
- [ ] T013 [P] [US1] Add `required=false` to `RocketMQTemplate` `@Autowired` in `PaymentSuccessConsumer` (share-order) — matching the pattern in `PaymentMQProducer`, so MQ unavailability doesn't crash the service during mock mode.
- [ ] T014 [US1] Restart share-order (9211) and share-payment (9213) with local Nacos configs — verify both register and startup cleanly.
- [ ] T015 [US1] Execute quickstart.md Step 1-5 end-to-end: createOrder → getByOrderNo → createPayment → mockCallback → verify order status. Record results.

**Checkpoint**: At this point, User Story 1 should be fully functional — user can complete payment flow end-to-end.

---

## Phase 3: User Story 2 — 支付失败和异常恢复 (Priority: P1)

**Goal**: Verify compensation and idempotency mechanisms work correctly.

**Independent Test**: (1) Trigger mock callback twice → verify no duplicate status change. (2) Simulate MQ failure → verify compensation task retries.

- [ ] T016 [P] [US2] Verify `PaymentCompensationTask` SQL correctness: query payment records with `payment_status=PAID` AND `callback_time BETWEEN now()-2h AND now()-30s` AND `mq_sent=0` (or equivalent mq_sent flag). If `mq_sent` column doesn't exist, add a reasonable filter (e.g., join with order status).
- [ ] T017 [P] [US2] Verify `PaymentMQProducer.sendPaySuccessMessage` retry logic — confirm `SendStatus.SEND_OK` check, log warning on failure, no exception thrown.
- [ ] T018 [P] [US2] Verify `PaymentSuccessConsumer` MQ consumer idempotency — transition() state machine + status pre-check should prevent duplicate processing. If not, add `orderNo+transactionId` dedup check before calling `processPaySuccess`.
- [ ] T019 [US2] Test Scenario 1: Execute mockCallback twice — confirm status only transitions once (idempotency). Log results.
- [ ] T020 [US2] Test Scenario 2: Temporarily disable MQ, trigger callback, verify compensation task picks up pending message when MQ re-enabled.

**Checkpoint**: Failure recovery and idempotency verified.

---

## Phase 4: User Story 3 — 退款流程 (Priority: P2)

**Goal**: Implement basic admin-triggered refund flow with status updates.

**Independent Test**: Admin calls refund API → payment status changes to REFUNDING → mock refund callback → status changes to REFUNDED.

- [ ] T021 [P] [US3] Add `refund(orderNo, amount, reason)` method to `RemotePaymentService` Feign interface in `share-api-payment/.../api/RemotePaymentService.java`.
- [ ] T022 [P] [US3] Add `refund` endpoint in `PaymentInfoController.java` — `@PostMapping("/api/v1/payment/refund")`, `@RequiresPermissions`, validate order paid status, call service.
- [ ] T023 [US3] Implement `processRefund` in `PaymentInfoServiceImpl.java` — (a) validate current payment status is PAID, (b) transition to REFUNDING, (c) call WeChat refund API (mock-mode wrapper), (d) send MQ message `order-refund` topic.
- [ ] T024 [US3] Add refund callback handler in `PaymentInfoServiceImpl.java` — (a) verify refund success notification, (b) transition payment status to REFUNDED, (c) send MQ message to order service.
- [ ] T025 [US3] Add `RefundConsumer` in share-order `consumer/` — listen to `order-refund` topic, update order status via `transition(orderId, orderNo, FINISHED_REFUND, ...)`.

**Checkpoint**: Admin can initiate refund → payment+order status updated.

---

## Phase 5: User Story 4 — 订单支付超时自动取消 (Priority: P2)

**Goal**: Timeout cancellation is configurable and correct. (Code mostly exists — config extraction done in T008.)

**Independent Test**: Create order with short timeout (e.g., 1 min via Nacos), wait, verify auto-cancel + stock release.

- [ ] T026 [P] [US4] Verify `OrderTimeoutConsumer` correctly calls `cancelOrder()` — confirm stock release Feign call, coupon release, order log.
- [ ] T027 [P] [US4] After T008 (Nacos config), verify `delayLevel` is correctly computed from `order.pay.timeout-minutes`. Document the mapping table (e.g., 1min→level 5, 5min→level 9, 10min→level 13, 15min→level 14, 30min→level 16).
- [ ] T028 [US4] Test Scenario: Set `order.pay.timeout-minutes=1` in Nacos, create order, wait 1min, verify order auto-cancelled, stock released.

**Checkpoint**: Timeout cancellation works with configurable duration.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories.

- [ ] T029 [P] Add structured logging to all payment callback handling (entry/exit with orderNo, duration, result) — ensure JSON-friendly format.
- [ ] T030 [P] Add payment key metrics log: `payment.success.rate`, `payment.callback.latency`, `compensation.task.count`. Use Micrometer `MeterRegistry` or simple counters in Redis.
- [ ] T031 [P] Fix Seata cluster name warning in share-payment Nacos config (add `seata.service.vgroupMapping.share-payment-group` with correct cluster name, or disable Seata for payment).
- [ ] T032 Fill production `wx.pay.v3` values in Nacos `share-payment-dev.yml` (APIv3 key, mchId, cert serial, private key path).
- [ ] T033 Execute quickstart.md complete validation — record all results in verification table.
- [ ] T034 Update `AGENTS.md` SPECKIT section to mark feature implementation complete.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No code dependencies on other phases — BLOCKS all user stories
- **US1 (Phase 2)**: Depends on Foundational — MVP, highest priority
- **US2 (Phase 3)**: Depends on Foundational — can run parallel to US1 (different files, different focus)
- **US3 (Phase 4)**: Depends on US1 completion (refund requires payment flow to exist)
- **US4 (Phase 5)**: Depends on Foundational T008 (config extraction) — can start after Phase 1
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### Within Each Phase

- Code reading tasks ([P]) can run in parallel
- Implementation tasks follow logical order within their phase
- Verification tasks document results for traceability

### Parallel Opportunities

| Phase | Parallelizable Tasks | Reason |
|-------|---------------------|--------|
| Phase 1 | T001-T007 | All read-only, different files |
| Phase 2 | T010-T013 | Different files, independent changes |
| Phase 3 | T016-T018 | Different files, independent verification |
| Phase 5 | T026-T027 | Different files |

---

## Parallel Example: Phase 1 — Foundational

```bash
# Launch all code reading tasks in parallel (T001-T007):
Task: "Read processPaySuccess in OrderInfoServiceImpl.java"
Task: "Read PaymentSuccessConsumer in consumer/"
Task: "Read OrderTimeoutConsumer in consumer/"
Task: "Read PaymentMQProducer in mq/"
Task: "Read PaymentCompensationTask in task/"
Task: "Read PaymentInfoController controller/"
Task: "Read RemotePaymentService Feign interface"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (code verification + config extraction)
2. Complete Phase 2: User Story 1 (verify end-to-end flow)
3. **STOP and VALIDATE**: Run quickstart.md — confirm order-pay-status chain works
4. Deploy/demo if ready

### Incremental Delivery

1. **Foundational + US1** → MVP: Payment flow works end-to-end
2. **+ US2** → Resilient: Compensation and idempotency verified
3. **+ US4** → Complete: Timeout auto-cancel configurable
4. **+ US3** → Full: Refund flow available
5. **+ Polish** → Production-ready: Observability, metrics, configs

### Parallel Team Strategy

| Developer | Phase |
|-----------|-------|
| A | Phase 1 (T001-T009) → Phase 2 (US1) |
| B | Phase 3 (US2) — after Phase 1 |
| C | Phase 5 (US4) — after Phase 1 T008 |
| A | Phase 4 (US3) — after Phase 2 |
| All | Phase 6 (Polish) |

---

## Notes

- [P] tasks = different files, no dependencies — launch simultaneously
- [Story] label maps task to user story for traceability
- Each user story independently testable (see Independent Test in each phase)
- Commit after each logical task group (e.g., after Phase 1, after each story)
- Stop at any checkpoint to validate independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence
- **User requirement**: "以实现的话你也看一遍" — all Phase 1 T001-T007 code reading tasks satisfy this requirement
