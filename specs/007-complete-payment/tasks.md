---
description: "Task list: share-payment 模块收尾清理"
---

# Tasks: share-payment 模块收尾清理

**Input**: Design documents from `specs/007-complete-payment/`

**Prerequisites**: spec.md (completed)

**Organization**: Tasks grouped by phase. 所有修改不涉及业务逻辑变更，纯代码质量修复。

**路径约定：**
- **PaymentMQProducer**: `share-modules/share-payment/src/main/java/com/share/payment/mq/PaymentMQProducer.java`
- **PaymentInfoController**: `share-modules/share-payment/src/main/java/com/share/payment/controller/PaymentInfoController.java`
- **CreatePaymentDTO**: `share-modules/share-payment/src/main/java/com/share/payment/domain/dto/CreatePaymentDTO.java`

---

## Phase 1: PaymentMQProducer 构造器注入改造 (US1)

**Purpose**: 将 `@Autowired(required=false) RocketMQTemplate` 改为构造器注入，保留可选降级语义

- [ ] T001 [US1] 修改 `PaymentMQProducer`：`@Autowired(required=false) private RocketMQTemplate rocketMQTemplate` → 构造器注入
  - 方案：声明 `private final RocketMQTemplate rocketMQTemplate;` + `@Autowired(required=false)` 标注在构造器参数上
  - 保留 `sendPaySuccessMessage` / `sendRefundSuccessMessage` 中的 `rocketMQTemplate == null` 降级检查

**预期结果**：LSP 无 error，`@Autowired` 字段注入清零

---

## Phase 2: Controller 返回类型统一 (US2)

**Purpose**: `PaymentInfoController.add()` 从 AjaxResult 改为 R<Void>

- [ ] T002 [US2] 修改 `PaymentInfoController.add()`：
  - 返回类型 `AjaxResult` → `R<Void>`
  - 方法体 `return toAjax(paymentInfoService.save(paymentInfo));` → `return R.ok();`（保留 `paymentInfoService.save()` 调用）
- [ ] T003 [US2] 移除 `PaymentInfoController` 中未使用的 import：`AjaxResult`、`BaseController`

**预期结果**：PaymentInfoController 全部方法统一使用 R<T>（/callback 端点返回 Map<String,String> 保留——微信网关要求）

---

## Phase 3: Feign 内部接口入参 DTO 化 (US3)

**Purpose**: `/inner/payment/create` 的 `Map<String, Object>` 入参改为 `CreatePaymentDTO`

- [ ] T004 [US3] `CreatePaymentDTO` 补充 `userId` 字段（原有字段：orderNo/amount/description/openid）
  - 添加 `private Long userId;`，无需校验注解（Feign 内部调用）
- [ ] T005 [US3] 修改 `PaymentInfoController.createPaymentInner()`：
  - `@RequestBody Map<String, Object> params` → `@RequestBody @Valid CreatePaymentDTO dto`
  - 方法体内 `params.get(...)` 转换 → 直接使用 `dto.getXxx()`
  - 调用链：`paymentInfoService.createPayment(dto.getOrderNo(), dto.getUserId(), dto.getAmount(), dto.getDescription(), dto.getOpenid())`
- [ ] T006 [US3] 移除 `createPaymentInner` 中不再使用的 `import java.util.Map`（如果其他方法不再使用 Map）

**预期结果**：`/inner/payment/create` 入参类型安全，不再有 `(String) params.get("orderNo")` 手动转换

---

## Phase 4: 编译验证 (US4)

**Purpose**: 确认所有修改后编译通过

- [ ] T007 [P] `mvn compile -pl share-modules/share-payment -am` — 输出 BUILD SUCCESS
- [ ] T008 [P] `mvn compile -pl share-order,share-goods,share-coupon,share-user,share-merchant -am` — 确认不改动不影响其他模块

---

## 执行顺序

### Phase 依赖

- **Phase 1 (构造器注入)**: 无依赖，可立即执行
- **Phase 2 (返回类型)**: 无依赖，可并行
- **Phase 3 (DTO)**: 无依赖，可并行（但需注意 T004 新增 userId 字段会在 T005 中使用）
- **Phase 4 (编译)**: 依赖 Phase 1~3 全部完成

### 并行策略

- T001, T002(T003), T004 可完全并行（不同文件，不影响）
- T005 依赖 T004 完成
- T007, T008 在 Phase 4 可并行

### 估算

- 代码修改：~5 分钟
- 编译验证：~2 分钟（Maven 依赖下载）
- 总计：~10 分钟
