---
description: "Task list for 商家端 Web 后台 feature implementation"
---

# Tasks: 商家端 Web 后台 (005-merchant-web-portal)

**Input**: Design documents from `specs/005-merchant-web-portal/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/interfaces.md

**Tests**: Manual integration testing per user story (no automated test tasks)

**Organization**: Tasks grouped by user story for independent implementation and testing

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks within phase)
- **[Story]**: Which user story this task belongs to (US1-US5)
- Exact file paths included

## Path Conventions

- **share-auth**: `share-auth/src/main/java/com/share/auth/`
- **share-gateway Nacos**: `share-gateway-dev.yml` (Nacos config, no local path)
- **share-merchant**: `share-modules/share-merchant/src/main/java/com/share/merchant/`
- **share-goods**: `share-modules/share-goods/src/main/java/com/share/goods/`
- **share-order**: `share-modules/share-order/src/main/java/com/share/order/`
- **share-common-security**: `share-common/share-common-security/src/main/java/com/share/common/security/`
- **share-ui**: `share-ui/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration and project scaffolding

- [ ] T001 Create SQL migration script (`specs/005-merchant-web-portal/sql/migration.sql`): CREATE `merchant_user` table, ALTER `merchant_info` ADD COLUMN `user_id`

**Checkpoint**: SQL scripts ready to run on local and VM MySQL

---

## Phase 2: Foundational / US1 — 商家登录与权限隔离 (Priority: P1) 🎯 MVP

**Goal**: Merchant can log in via dedicated endpoint, see only own data

**Independent Test**: Merchant A login → sees own store data → cannot see Merchant B's data. Unapproved store login rejected.

### Backend: Merchant User Entity & Service (share-merchant)

- [ ] T002 [P] [US1] Create `MerchantUser` entity in `share-modules/share-merchant/src/main/java/com/share/merchant/domain/MerchantUser.java` (extends BaseEntity, maps `merchant_user` table)
- [ ] T003 [P] [US1] Create `MerchantUserMapper` in `share-modules/share-merchant/src/main/java/com/share/merchant/mapper/MerchantUserMapper.java` (extends BaseMapper)
- [ ] T004 [P] [US1] Create `IMerchantUserService` interface + `MerchantUserServiceImpl` with: `login(username, password)`, `updatePassword()`, `getByUsername()` in `share-modules/share-merchant/src/main/java/com/share/merchant/service/`
- [ ] T005 [US1] Add `user_id` field to `MerchantInfo` entity in `share-modules/share-merchant/src/main/java/com/share/merchant/domain/MerchantInfo.java` and create field in `MerchantInfoMapper.xml`

### Backend: Merchant Auth (share-auth)

- [ ] T006 [US1] Create `MerchantTokenController` in `share-auth/src/main/java/com/share/auth/controller/MerchantTokenController.java` with endpoints: `POST /auth/merchant/login`, `POST /auth/merchant/logout`, `GET /auth/merchant/getInfo` (model after TokenController pattern)
- [ ] T007 [US1] Create `MerchantLoginService` in `share-auth/src/main/java/com/share/auth/service/MerchantLoginService.java` — validates merchant credentials (Feign to share-merchant for MerchantUser + MerchantInfo), checks store status, calls `TokenService.createToken()` with role "merchant"

### Backend: MerchantId Context (share-common-security)

- [ ] T008 [US1] Add `merchantId` field to `LoginUser` in `share-api/share-api-system/src/main/java/com/share/system/api/model/LoginUser.java`
- [ ] T009 [P] [US1] Add `MERCHANT_ID` constant to `SecurityConstants` in `share-common/share-common-core/src/main/java/com/share/common/core/constant/SecurityConstants.java`
- [ ] T010 [P] [US1] Add `setMerchantId()`/`getMerchantId()` to `SecurityContextHolder` in `share-common/share-common-core/src/main/java/com/share/common/core/context/SecurityContextHolder.java`
- [ ] T011 [P] [US1] Add `getMerchantId()` static method to `SecurityUtils` in `share-common/share-common-security/src/main/java/com/share/common/security/utils/SecurityUtils.java`

### Backend: Gateway Configuration

- [ ] T012 [US1] Add `/auth/merchant/**` to gateway whitelist (`security.ignore.whites`) in Nacos `share-gateway-dev.yml`
- [ ] T013 [US1] Add merchant API routes to Nacos `share-gateway-dev.yml`:
  - `share-merchant-api`: `/api/v1/merchant/product/**,/api/v1/merchant/sku/**` → lb://share-goods
  - `share-merchant-order-api`: `/api/v1/merchant/order/**,/api/v1/merchant/after-sale/**` → lb://share-order
  - `share-merchant-profile-api`: `/api/v1/merchant/profile/**,/api/v1/merchant/dashboard` → lb://share-merchant

### Frontend: Merchant Login & Foundation (share-ui)

- [ ] T014 [P] [US1] Create merchant login page at `share-ui/src/views/merchant/login.vue` (simplified login form without captcha, POST `/auth/merchant/login`)
- [ ] T015 [P] [US1] Create merchant auth API module at `share-ui/src/api/merchant/auth.js` (login/logout/getInfo functions)
- [ ] T016 [US1] Create merchant router config at `share-ui/src/router/merchant.js` with routes: login (hidden), dashboard, products, orders, after-sale, settings
- [ ] T017 [US1] Create merchant layout wrapper and empty dashboard at `share-ui/src/views/merchant/dashboard/index.vue`

**Checkpoint**: Merchant can log in at `/merchant/login`, see empty dashboard with their store name

---

## Phase 3: US2 — 商品管理 (Priority: P1)

**Goal**: Merchant manages own products — CRUD, SKU, on/off shelf

**Independent Test**: Merchant login → product list → add product with SKUs → shelf it → visible in store

### Backend: Merchant Product API (share-goods)

- [ ] T018 [P] [US2] Create `MerchantProductController` in `share-modules/share-goods/src/main/java/com/share/goods/controller/MerchantProductController.java` with endpoints: list (filtered by merchantId via SecurityUtils), detail, add (with merchantId auto-set), edit, shelf/un-shelf. `@RequestMapping("/api/v1/merchant/product")`, `@RequiresLogin`
- [ ] T019 [P] [US2] Create `MerchantSkuController` in `share-modules/share-goods/src/main/java/com/share/goods/controller/MerchantSkuController.java` with endpoints: list by product, add, edit, delete. `@RequestMapping("/api/v1/merchant/sku")`, `@RequiresLogin`

### Frontend: Product Pages (share-ui)

- [ ] T020 [P] [US2] Create product list page at `share-ui/src/views/merchant/product/index.vue` (table with name/status filter, shelf/un-shelf buttons, add/edit buttons)
- [ ] T021 [P] [US2] Create product edit page at `share-ui/src/views/merchant/product/edit.vue` (form: name, description, category selector, images, price, sort)
- [ ] T022 [P] [US2] Create SKU management component at `share-ui/src/views/merchant/product/components/SkuManager.vue` (inline editable table for spec name/price/stock)
- [ ] T023 [US2] Create product API module at `share-ui/src/api/merchant/product.js`

**Checkpoint**: Merchant can add/edit/shelf/un-shelf products with SKUs on the product management page

---

## Phase 4: US3 — 订单管理 (Priority: P1)

**Goal**: Merchant views own orders, performs delivery

**Independent Test**: Merchant login → order list → click deliver on pending order → enter delivery info → order status changes to "shipping"

### Backend: Merchant Order API (share-order)

- [ ] T024 [P] [US3] Create `MerchantOrderController` in `share-modules/share-order/src/main/java/com/share/order/controller/MerchantOrderController.java` with endpoints: list (filtered by supplierId=merchantId), detail, deliver. `@RequestMapping("/api/v1/merchant/order")`, `@RequiresLogin`
  - Deliver endpoint: POST `/api/v1/merchant/order/deliver` with body `{orderNo, deliveryName, deliveryPhone}` — calls existing `orderInfoService.deliverOrder()` with merchant ID as deliveryBy

### Frontend: Order Pages (share-ui)

- [ ] T025 [P] [US3] Create order list page at `share-ui/src/views/merchant/order/index.vue` (table with status filter, order info columns, deliver button for pending orders)
- [ ] T026 [P] [US3] Create order detail page at `share-ui/src/views/merchant/order/detail.vue` (order info, items, address, payment, delivery info, status timeline)
- [ ] T027 [US3] Create order API module at `share-ui/src/api/merchant/order.js`

**Checkpoint**: Merchant can view orders filtered by store, perform delivery with delivery person info

---

## Phase 5: US4 — 售后审核 (Priority: P2)

**Goal**: Merchant views after-sale requests, approves or rejects refunds

**Independent Test**: Merchant login → after-sale list → approve refund → system auto-refunds → order cancelled. Reject → request escalates to admin.

### Backend: Merchant After-Sale API (share-order)

- [ ] T028 [P] [US4] Create `MerchantAfterSaleController` in `share-modules/share-order/src/main/java/com/share/order/controller/MerchantAfterSaleController.java` with endpoints: list (filtered by merchantId), approve, reject. `@RequestMapping("/api/v1/merchant/after-sale")`, `@RequiresLogin`
  - Approve: calls existing refund flow (payment MQ)
  - Reject: updates after_sale_request status to "商家拒绝" → sets escalate_to_admin flag

### Frontend: After-Sale Pages (share-ui)

- [ ] T029 [P] [US4] Create after-sale list page at `share-ui/src/views/merchant/afterSale/index.vue` (table of requests with refund reason/amount, approve/reject action buttons, reject reason dialog)
- [ ] T030 [US4] Create after-sale API module at `share-ui/src/api/merchant/afterSale.js`

**Checkpoint**: Merchant can review and respond to after-sale requests

---

## Phase 6: US5 — 店铺设置与数据概览 (Priority: P3)

**Goal**: Merchant edits store info, views dashboard with stats

**Independent Test**: Merchant login → dashboard shows today's orders/sales → settings page → update store name → saved

### Backend: Merchant Profile & Dashboard API (share-merchant)

- [ ] T031 [P] [US5] Create `MerchantProfileController` in `share-modules/share-merchant/src/main/java/com/share/merchant/controller/MerchantProfileController.java` with endpoints: GET/PUT profile, PUT password. `@RequestMapping("/api/v1/merchant/profile")`, `@RequiresLogin`
- [ ] T032 [P] [US5] Create `MerchantDashboardController` in `share-modules/share-merchant/src/main/java/com/share/merchant/controller/MerchantDashboardController.java` with endpoint: GET dashboard stats (today's orders, today's sales, pending orders count, pending delivery count). `@RequestMapping("/api/v1/merchant/dashboard")`, `@RequiresLogin`

### Frontend: Settings & Dashboard Pages (share-ui)

- [ ] T033 [P] [US5] Create settings page at `share-ui/src/views/merchant/setting/index.vue` (form: store name, description, contact phone, address, logo upload, password change)
- [ ] T034 [P] [US5] Enhance dashboard page at `share-ui/src/views/merchant/dashboard/index.vue` (stat cards: today's orders count, today's sales amount, pending processing, pending delivery)
- [ ] T035 [US5] Create dashboard/settings API module at `share-ui/src/api/merchant/dashboard.js`

**Checkpoint**: Merchant can view stats dashboard and update store info

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Admin configuration, menu permissions, final validation

- [ ] T036 [P] Add merchant role (`sys_role`: role_key=`merchant`, role_name=`商家用户`) and merchant menu tree (`sys_menu`: 商家管理 → 商品管理/订单管理/售后管理/店铺设置/控制台) in share-system (via admin UI or SQL script)
- [ ] T037 [P] Ensure `MerchantUser` creation is automated when admin creates/approves a `MerchantInfo` (add logic in `MerchantInfoServiceImpl` to auto-create `MerchantUser` with generated username/password when status changes to approved)
- [ ] T038 Validate all merchant endpoints enforce merchantId data isolation (code review all new controllers)
- [ ] T039 Run `specs/005-merchant-web-portal/quickstart.md` validation

**Checkpoint**: Full feature complete, validated against quickstart

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — can start immediately
- **Phase 2 (US1/Foundational)**: Depends on Phase 1 — BLOCKS all user stories
- **Phase 3-6 (US2-US5)**: Depend on Phase 2 (need merchant login + merchantId context)
  - US2-US5 can proceed sequentially or opportunistically in parallel within each phase
- **Phase 7 (Polish)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Foundational — must be complete before any other story
- **US2 (P1)**: Depends on US1 — no dependency on US3-US5
- **US3 (P1)**: Depends on US1 — no dependency on US2/US4/US5
- **US4 (P2)**: Depends on US1 + US3 (needs order context) — can proceed after US1
- **US5 (P3)**: Depends on US1 — no dependency on US2-US4

### Parallel Opportunities Per Phase

- **Phase 2**: T002+T003+T004 (MerchantUser entity/mapper/service) parallel with T009+T010+T011 (SecurityConstants/SecurityContextHolder/SecurityUtils)
- **Phase 3**: T018+T019 (Product+Sku controllers) parallel
- **Phase 4**: All tasks sequential within phase (small scope)
- **Phase 5**: T028 backend can run in parallel with T029+T030 frontend
- **Phase 6**: T031+T032 (Profile+Dashboard controllers) parallel
- **Phase 7**: T036 (menu/role) can run in parallel with T037 (auto-create user)

---

## Parallel Example: Phase 2 (US1)

```text
# Backend entities (parallel):
Task: T002 MerchantUser entity
Task: T003 MerchantUserMapper
Task: T009 SecurityConstants constant
Task: T010 SecurityContextHolder method

# After entity layer complete:
Task: T004 MerchantUserService
Task: T006 MerchantTokenController
Task: T007 MerchantLoginService
Task: T012 Gateway whitelist

# Frontend (parallel with backend):
Task: T014 merchant login page
Task: T015 merchant auth API module
```

---

## Implementation Strategy

### MVP (Phase 1 + Phase 2 only)

1. Complete Phase 1: SQL migration
2. Complete Phase 2: Merchant login + merchantId context + empty dashboard
3. **STOP and VALIDATE**: Merchant can log in, see empty dashboard
4. Deploy/demo if ready

### Incremental Delivery

1. **MVP**: US1 → merchant can log in (Phase 1+2)
2. **Increment 1**: US2 → product management (Phase 3)
3. **Increment 2**: US3 → order management (Phase 4)
4. **Increment 3**: US4 → after-sale review (Phase 5)
5. **Increment 4**: US5 → dashboard + settings (Phase 6)
6. **Polishing**: Menu permissions, validation, auto-create user (Phase 7)

### Parallel Team Strategy

With multiple developers:

1. Phase 1 + Phase 2: 1-2 developers (backend + frontend)
2. Phase 3-6: Can parallelize across 2-3 developers:
   - Developer A: US2 (products) + US5 (settings)
   - Developer B: US3 (orders) + US4 (after-sale)
3. Phase 7: Single developer after all stories integrate

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks within same phase
- [USx] label maps task to specific user story for traceability
- Each user story is independently testable after its phase completes
- All merchant endpoints use `@RequiresLogin` + `SecurityUtils.getMerchantId()` for data isolation
- Existing `OrderInfoApiController.deliver()` already supports delivery — merchant controller reuses same service
- Classification: merchants read-only access to categories via existing `CategoryController.treeselect`
