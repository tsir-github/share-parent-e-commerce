---
description: "Task list for data-report feature implementation"
---

# Tasks: 平台数据报表

**Input**: Design documents from `specs/015-data-report/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**Tests**: 本项目不使用自动化测试框架，验证方式为编译检查 + 手动 curl 验证（见 quickstart.md）

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: 确认项目结构，无需新建模块或配置

- [x] T001 确认 share-order 模块的 Nacos 配置已包含跨库查询权限（share_order/share_payment/share_merchant）
- [x] T002 [P] 确认 share-order 的 pom.xml 已包含 share-common-security 依赖（用于 @RequiresPermissions）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 创建核心聚合查询 Mapper + XML，所有 US 都依赖

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Create `DataReportMapper.java` in share-modules/share-order/src/main/java/com/share/order/mapper/DataReportMapper.java
- [x] T004 Create `DataReportMapper.xml` in share-modules/share-order/src/main/resources/mapper/DataReportMapper.xml (含所有聚合 SQL)

**Checkpoint**: Foundation ready - all report SQL queries are available

---

## Phase 3: User Story 1 - 交易总览 (Priority: P1) 🎯 MVP

**Goal**: 平台管理员查看今日/本周/本月/自定义范围的订单数、交易额、客单价、退款金额

**Independent Test**: `GET /order/data-report/overview` 返回 todayOrderCount/weekRevenue/monthRevenue/totalOrders 等字段，无数据时返回 0

- [x] T005 Create `IDataReportService.java` interface in share-modules/share-order/src/main/java/com/share/order/service/IDataReportService.java
- [x] T006 Implement `getOverview()` in `DataReportServiceImpl.java` in share-modules/share-order/src/main/java/com/share/order/service/impl/DataReportServiceImpl.java
- [x] T007 [US1] Create `DataReportController.java` in share-modules/share-order/src/main/java/com/share/order/controller/DataReportController.java with `/data-report/overview` endpoint

**Checkpoint**: 交易总览接口可用，可在浏览器独立验证

---

## Phase 4: User Story 2 - 订单趋势 (Priority: P1)

**Goal**: 管理员查看指定日期范围内每日的订单数和交易额趋势

**Independent Test**: `GET /order/data-report/trend?startDate=2026-06-01&endDate=2026-06-28` 返回按日期排序的数组

- [x] T008 [P] [US2] Implement `getTrend()` in `DataReportServiceImpl.java`
- [x] T009 [US2] Add `/data-report/trend` endpoint in `DataReportController.java`

**Checkpoint**: 订单趋势接口可用，返回数据适用于绘制折线图

---

## Phase 5: User Story 3 - 商品销售排行 (Priority: P2)

**Goal**: 管理员查看按销量或交易额排序的商品排行，支持日期范围筛选和 Top N

**Independent Test**: `GET /order/data-report/product-ranking?sortBy=salesCount&topN=5` 返回含 rank/productName/salesCount/revenue 的数组

- [x] T010 [P] [US3] Implement `getProductRanking()` in `DataReportServiceImpl.java`
- [x] T011 [US3] Add `/data-report/product-ranking` endpoint in `DataReportController.java`

**Checkpoint**: 商品销售排行接口可用

---

## Phase 6: User Story 4 - 商家销售排行 (Priority: P2)

**Goal**: 管理员查看按交易额排序的商家排行

**Independent Test**: `GET /order/data-report/merchant-ranking?topN=10` 返回含 merchantName/orderCount/revenue 的数组

- [x] T012 [P] [US4] Implement `getMerchantRanking()` in `DataReportServiceImpl.java`
- [x] T013 [US4] Add `/data-report/merchant-ranking` endpoint in `DataReportController.java`

**Checkpoint**: 商家销售排行接口可用

---

## Phase 7: User Story 5 - 用户统计 (Priority: P3)

**Goal**: 管理员查看用户总数、新增用户数（今日/本周/本月）、活跃用户数、新增趋势

**Independent Test**: `GET /user/userInfo/statistics` 返回 totalUsers/todayNewUsers/activeUsers/dailyTrend

- [x] T014 [P] [US5] Add `getUserStatistics()` method in `UserInfoServiceImpl.java` in share-modules/share-user/src/main/java/com/share/user/service/impl/UserInfoServiceImpl.java
- [x] T015 [US5] Add `/userInfo/statistics` endpoint in `UserInfoController.java` in share-modules/share-user/src/main/java/com/share/user/controller/UserInfoController.java

**Checkpoint**: 用户统计接口可用

---

## Phase 8: User Story 6 - 支付统计 (Priority: P3)

**Goal**: 管理员查看各支付方式的订单数、金额及占比

**Independent Test**: `GET /order/data-report/payment-stats` 返回各支付方式的订单数/金额/占比

- [x] T016 [P] [US6] Implement `getPaymentStats()` in `DataReportServiceImpl.java`
- [x] T017 [US6] Add `/data-report/payment-stats` endpoint in `DataReportController.java`

**Checkpoint**: 支付统计接口可用

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 编译验证与整体检查

- [x] T018 [P] 编译验证 share-order 模块（`mvn compile -pl share-modules/share-order -am -q`）
- [x] T019 [P] 编译验证 share-user 模块（`mvn compile -pl share-modules/share-user -am -q`）
- [x] T020 检查所有 Controller 遵循 `BaseController` + `AjaxResult` + `@RequiresPermissions` 模式
- [x] T021 检查所有 Service 类使用 `@RequiredArgsConstructor` + `final`（禁止 `@Autowired`）
- [ ] T022 运行 quickstart.md 中的 curl 验证命令，确认所有接口正常

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: T003→T004 must complete before any user story
- **US1 (Phase 3)**: T005→T006→T007 sequential (interface→service→controller)
- **US2-6 (Phase 4-8)**: Each depends on Phase 2 complete, independent of each other
- **Polish (Phase 9)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: No dependencies on other stories - T005/T006 (Service interface/impl) must be created here, reused by US2/3/4/6
- **US2-6 (P1-P3)**: All depend on Phase 2 completion; US1's Service interface is reused
- **US5 (P3)**: Independent in share-user module, no dependency on share-order

### Within Each User Story

- Service method before Controller endpoint
- Compile check before marking complete

---

## Parallel Opportunities

| Phase | Tasks | Can Parallel With |
|-------|-------|-------------------|
| Setup | T001, T002 | Both [P], can run simultaneously |
| Foundational | T003, T004 | T003+004 are sequential (Mapper + XML) |
| US1 | T005, T006, T007 | Sequential (interface→impl→controller) |
| US2-US6 | Per-phase service + controller | US1's service must exist first, then US2-6 service methods AND controllers can all be built in parallel |
| Polish | T018, T019 | share-order and share-user compile in parallel |
| US5 | T014, T015 | Independent from all share-order tasks |

**Optimal parallel execution**:
1. Phase 1 + Phase 2 (sequential within)
2. Phase 3 (US1 - creates Service interface/base impl)
3. **US2 + US3 + US4 + US6 all in parallel** (add methods to existing Service + add to existing Controller)
4. **US5 in parallel with any of the above** (different module)

---

## Parallel Example: US2 + US3 + US4 + US6

```bash
# Launch all in parallel after US1 is done:
Task: T008 [US2] getTrend() in DataReportServiceImpl.java
Task: T009 [US2] /data-report/trend in DataReportController.java
Task: T010 [US3] getProductRanking() in DataReportServiceImpl.java
Task: T011 [US3] /data-report/product-ranking in DataReportController.java
Task: T012 [US4] getMerchantRanking() in DataReportServiceImpl.java
Task: T013 [US4] /data-report/merchant-ranking in DataReportController.java
Task: T016 [US6] getPaymentStats() in DataReportServiceImpl.java
Task: T017 [US6] /data-report/payment-stats in DataReportController.java
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup (2 tasks)
2. Complete Phase 2: Foundational (2 tasks)
3. Complete Phase 3: US1 交易总览 (3 tasks)
4. **STOP and VALIDATE**: `GET /order/data-report/overview` returns correct data

### Incremental Delivery

1. US1 (P1) 交易总览 → MVP: 管理员可见平台核心经营指标
2. US2 (P1) 订单趋势 → 折线图数据就绪
3. US3/4 (P2) 商品/商家排行 → 排行榜数据就绪
4. US5 (P3) 用户统计 → 用户增长数据就绪
5. US6 (P3) 支付统计 → 支付分布数据就绪

### Parallel Team Strategy

1. Person A: US1 (Service + Controller) + US2
2. Person B: US3 + US4 (parallel with A after US1 foundation)
3. Person C: US5 (different module, fully independent)
4. Person D: US6 (parallel after US1 foundation)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All reports are **GET only** - no write operations
- No new tables needed - all data from existing tables via SQL aggregation
- Cross-database queries use `db.table` syntax (verified in MerchantDashboardMapper)
- US5 lives in share-user module, all others in share-order module
- Compile verification = `mvn compile -q` (本项目的验证标准)
