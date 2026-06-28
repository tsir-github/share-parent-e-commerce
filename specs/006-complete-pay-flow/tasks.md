---
description: "Task list: 完成微信支付全链路集成收尾"
---

# Tasks: 完成微信支付全链路集成

**Input**: Design documents from `specs/006-complete-pay-flow/`

**Prerequisites**: plan.md (required), spec.md (required), research.md (completed)

**Organization**: Tasks grouped by phase. Note: 代码审计发现 US1(RefundConsumer) 和 US2(MQ对齐) 实际已实现，仅 US3(Nacos配置) 需操作。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **share-modules/share-payment/** — 支付模块 (src/main/java/com/share/payment/)
- **share-modules/share-order/** — 订单模块 (src/main/java/com/share/order/)
- **middleware/nacos-configs/** — Nacos 配置模板（本地副本）
- **share-common/share-common-core/** — 公共核心模块
- **share-api/share-api-payment/** — 支付接口 DTO 模块

---

## Phase 1: 代码审计确认（验证现有代码完整性）

**Purpose**: 确认 US1/US2 实际已实现，无需新增代码

- [X] T001 [P] Read `PaymentRefundConsumer` at `share-modules/share-order/src/main/java/com/share/order/consumer/PaymentRefundConsumer.java` — 确认 Consumer 存在、topic 正确、调用 `processRefundSuccess` 方法
- [X] T002 [P] Read `RefundSuccessMessage` at `share-api/share-api-payment/src/main/java/com/share/payment/domain/dto/RefundSuccessMessage.java` — 确认消息体字段：orderNo(String)、transactionId(String)、refundAmount(BigDecimal)
- [X] T003 [P] Read `MqConstants.REFUND_SUCCESS_TOPIC` at `share-common/share-common-core/src/main/java/com/share/common/core/constant/MqConstants.java` — 确认 topic = `"order-refund-success"`，与两端一致
- [X] T004 [P] Read `PaymentMQProducer.sendRefundSuccessMessage()` at `share-modules/share-payment/src/main/java/com/share/payment/mq/PaymentMQProducer.java` — 确认 producer 使用 `convertAndSend` + 同一 topic 常量

**Checkpoint**: ✅ US1(RefundConsumer) 和 US2(MQ对齐) 确认已实现，无需新增代码

---

## Phase 2: Nacos 配置修正 (US3)

**Purpose**: 修复 share-payment-dev.yml 中的 MySQL 驱动类和 host

**⚠️ 注意事项（AGENTS.md 约定）**:
- 配置管理：环境差异配置放 Nacos，禁止硬编码
- MySQL driver 必须使用 `com.mysql.cj.jdbc.Driver`（对应 `mysql-connector-j` 新驱动）
- 数据库 host 必须指向 VM 地址 `192.168.10.129`

### 本地配置模板修正

- [X] T005 [US3] Fix `driver-class-name` in `middleware/nacos-configs/share-payment-dev.yml`: `com.mysql.jdbc.Driver` → `com.mysql.cj.jdbc.Driver`
- [X] T006 [US3] Fix `url` host in `middleware/nacos-configs/share-payment-dev.yml`: `localhost:3306` → `192.168.10.129:3306`

### Nacos 配置推送

- [X] T007 [US3] Push updated `share-payment-dev.yml` to Nacos via curl to `192.168.10.129:8848`（命名空间 `a746e297-417e-4aec-bfb1-e42df33fbe93`）
- [X] T008 [US3] Verify Nacos config: curl GET config → 确认 `driver-class-name=com.mysql.cj.jdbc.Driver` 且 `url` 指向 `192.168.10.129`

**Checkpoint**: Nacos 配置已修正并推送

---

## Phase 3: 编译验证

**Purpose**: 确保 share-payment 模块编译通过

- [X] T009 Run `mvn clean compile -pl share-modules/share-payment -am` — 编译通过
- [X] T010 Run `mvn clean compile -pl share-modules/share-payment -am` — 静默成功（LSP 未安装，Maven 权威验证通过）

**Checkpoint**: ✅ 编译通过

---

## Phase 4: Mock 模式端到端验证

**Purpose**: 验证退款全链路在 mock 模式下正常工作

- [ ] T011 Start `SharePaymentApplication` + `ShareOrderApplication` + `ShareAuthApplication` — 需在 IDEA 手动启动微服务后验证
- [ ] T012 [P] Verify `share-order` registers `PaymentRefundConsumer` to `order-refund-success` topic — 需启动后查看日志
- [ ] T013 [P] Verify `share-payment` registers to Nacos — 需启动后查看 Nacos 服务列表
- [ ] T014 Simulate a payment → refund flow: 触发 mock 支付成功 → mock 退款 → 确认 `PaymentRefundConsumer` 收到 MQ → 确认订单状态变更为已退款

**Checkpoint**: 全链路验证通过

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (代码审计)**: 无依赖，可立即执行 — T001~T004 完全并行
- **Phase 2 (Nacos修正)**: 依赖 Phase 1 确认（无 blocking issue）— T005~T006 并行，T007~T008 串行
- **Phase 3 (编译)**: 依赖 Phase 2 完成（配置修正后编译）
- **Phase 4 (验证)**: 依赖 Phase 3 完成（编译通过后启动验证）

### User Story Dependencies

- **US3 (Nacos配置)**: 独立，不依赖其他 story — Phase 2 中完成
- **US1/US2**: 已验证已实现，无操作

### Within Each Phase

- T001~T004: 完全并行（不同文件，无依赖）
- T005~T006: 并行（不同行修改同一文件，但不相邻)
- T007: 依赖 T005+T006 完成
- T008: 依赖 T007 完成
- T009~T014: 串行执行

### Parallel Opportunities

- Phase 1 全部 [P] 任务可并行：读 4 个不同文件
- T005~T006 可并行（修改同一文件的不同行，不冲突）
- T012~T013 可在等待期间并行

---

## Parallel Example: Phase 1

```bash
# 并行读取 4 个审计文件：
Task: "Read PaymentRefundConsumer.java"
Task: "Read RefundSuccessMessage.java"
Task: "Read MqConstants.java"
Task: "Read PaymentMQProducer.java"
```

---

## Implementation Strategy

### MVP Scope

真正的任务量很小——只有 **Nacos 配置两行修改 + 推送 + 编译验证**。建议一次性完成所有 Phase。

1. **Phase 1**: 快速代码审计确认（~2 分钟）
2. **Phase 2**: 修改配置模板 → 推送 Nacos → 验证（~5 分钟）
3. **Phase 3**: 编译验证（~2 分钟，Maven 依赖下载）
4. **Phase 4**: 启动验证（~5 分钟，微服务启动）

总计预估：~15 分钟（不含下载依赖和启动等待）

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Nacos 配置修改后需手动推送（不会自动同步）
- 修改后建议重启 share-payment 模块验证配置生效
