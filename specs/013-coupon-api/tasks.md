# Tasks: 优惠券功能 API

**Input**: Design documents from `/specs/013-coupon-api/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: 本项目不使用自动测试框架，验证方式为编译检查 + LSP diagnostics

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1-US5)
- Include exact file paths

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 项目已有完整模块和依赖，无需初始化。

- [ ] T001 确认 share-coupon 模块 pom.xml 已包含 share-api-coupon 依赖（已验证存在），确认 share-order pom.xml 已包含 share-api-coupon 依赖（已验证存在）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Feign 接口扩展 + 网关路由配置，必须在用户故事开始前完成

- [ ] T002 [P] 扩展 `RemoteCouponService` 追加 `lockForOrder`/`releaseForOrder`/`countAvailable` 方法
      文件: `share-api/share-api-coupon/src/main/java/com/share/coupon/api/RemoteCouponService.java`
      参照现有 `releaseByOrderNo` 方法风格，保持 `@PostMapping` + `Map<String,Object>` 参数 + `@RequestHeader(FROM_SOURCE)` 模式

- [ ] T003 [P] 扩展 `RemoteCouponFallbackFactory` 追加新方法的降级实现
      文件: `share-api/share-api-coupon/src/main/java/com/share/coupon/factory/RemoteCouponFallbackFactory.java`
      lock 降级返回 `R.fail("优惠券锁定失败")`，release/countAvailable 降级返回空值

- [ ] T004 [P] 配置网关路由 `/coupon/**` → `lb://share-coupon`
      文件: Nacos `share-gateway-dev.yml` / 本地 `middleware/nacos-configs/share-gateway-dev.yml`
      追加路由条目（StripPrefix=1），参照现有 goods/order 路由格式

**Checkpoint**: Feign 接口可用，网关可转发 coupon 请求

---

## Phase 3: User Story 1 - C端领取优惠券 (Priority: P1) 🎯 MVP

**Goal**: 业主可查看可领取模板列表并领取优惠券

**Independent Test**: 登录后请求 `/api/v1/coupon/available` 看到模板列表，调用 `POST /api/v1/coupon/claim/{id}` 领取成功

- [ ] T005 [P] [US1] 创建 `AvailableCouponVO` 响应 VO（含模板名称/类型/金额/有效期/余量/已领数/限领数）
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/domain/vo/AvailableCouponVO.java`

- [ ] T006 [US1] 创建 `CouponApiController`，实现 `GET /api/v1/coupon/available`
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/controller/CouponApiController.java`
      - 继承 `R<T>` 返回风格（不是 AjaxResult）
      - `@RequiresLogin` 鉴权
      - 查询 `coupon_template`：status=1, del_flag=0, start_time<=now, end_time>=now, remain_count!=0
      - 返回 AvailableCouponVO 列表，含当前用户已领数子查询

- [ ] T007 [US1] 在 `CouponApiController` 实现 `POST /api/v1/coupon/claim/{templateId}`
      - 校验模板存在且可领取
      - 调用 `ICouponUserService.claimCoupon()`（已有实现）
      - 捕获 `ServiceException` 返回友好提示

**Checkpoint**: 业主可领取优惠券

---

## Phase 4: User Story 2 - C端查看我的优惠券 (Priority: P2) 🎯 MVP

**Goal**: 业主分页查看自己的优惠券，支持状态筛选

**Independent Test**: 领取后调用 `GET /api/v1/coupon/my?status=0` 看到记录

- [ ] T008 [P] [US2] 创建 `MyCouponVO` 响应 VO（含模板名称/类型/条件金额/减免金额/折扣率/状态/有效期/领取时间）
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/domain/vo/MyCouponVO.java`

- [ ] T009 [US2] 在 `CouponApiController` 实现 `GET /api/v1/coupon/my`
      - 分页查询 `coupon_user` JOIN `coupon_template`
      - 支持 `status` 参数筛选（0=未使用 1=已使用 2=已过期）
      - 自动过滤已过期（end_time < now 的未使用券标记 status=2）
      - 按领取时间倒序

**Checkpoint**: 业主可查看自己的优惠券

---

## Phase 5: User Story 3 - C端查询下单可用券 (Priority: P2)

**Goal**: 业主在结算页面可查看下单可用的优惠券

**Independent Test**: 有满100-10券，传 amount=150 时返回该券，传 amount=50 时不返回

- [ ] T010 [P] [US3] 创建 `UsableCouponDTO` 入参 DTO（含 `@NotNull BigDecimal amount` 订单金额）
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/domain/dto/UsableCouponDTO.java`

- [ ] T011 [P] [US3] 创建 `UsableCouponVO` 响应 VO（含优惠券ID/模板ID/名称/类型/条件金额/减免金额/折扣率/实际可减免金额/有效期）
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/domain/vo/UsableCouponVO.java`

- [ ] T012 [US3] 在 `CouponApiController` 实现 `GET /api/v1/coupon/usable?amount={amount}`
      - 查询当前用户 status=0 且模板 status=1 且在有效期内的优惠券
      - 按 type 过滤：type=0（满减）需 amount>=condition_amt；type=1（折扣）需 amount>=condition_amt；type=2（无门槛）直接可用
      - 计算实际可减免金额（discount 字段）
      - 按减免金额降序排列

**Checkpoint**: 下单时可根据金额筛选可用券

---

## Phase 6: User Story 4 - 商家管理优惠券模板 (Priority: P2)

**Goal**: 商家可创建/查看/编辑/删除自己的优惠券模板

**Independent Test**: 商家创建模板后在列表看到自己的模板

- [ ] T013 [P] [US4] 创建 `CouponTemplateDTO` 入参 DTO（含名称/类型/条件金额/减免金额/折扣率/发行总量/限领数/有效期起止/备注）
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/domain/dto/CouponTemplateDTO.java`
      使用 `@NotNull`/`@NotBlank`/`@Min` 等校验注解

- [ ] T014 [P] [US4] 创建 `MerchantCouponController`
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/controller/MerchantCouponController.java`
      - `@RestController @RequestMapping("/api/v1/merchant/coupon/template")`
      - 返回 `R<T>`，`@RequiresLogin`
      - 获取当前商家用户 ID：`SecurityUtils.getUserId()`（从商家用户表查询 merchant_id）
      - 所有操作按 `merchant_id` 过滤

- [ ] T015 [US4] 实现 `GET /api/v1/merchant/coupon/template/list`（分页查询）
      - 只返回当前商家（merchant_id）的模板
      - 创建时间倒序

- [ ] T016 [US4] 实现 `GET /api/v1/merchant/coupon/template/{id}`（查询详情）
      - 校验模板属于当前商家

- [ ] T017 [US4] 实现 `POST /api/v1/merchant/coupon/template`（创建）
      - `merchant_id` 从当前登录用户获取（通过商家用户信息查询）
      - 新模板 status 默认 0（禁用）

- [ ] T018 [US4] 实现 `PUT /api/v1/merchant/coupon/template`（编辑）
      - 校验模板属于当前商家
      - 若减少 total_count，检查是否低于已发放数量
      - 使用 `LambdaUpdateWrapper` 更新（禁止 `updateById`）

- [ ] T019 [US4] 实现 `DELETE /api/v1/merchant/coupon/template/{id}`（删除）
      - 校验模板属于当前商家
      - 检查 `coupon_user` 表是否有领取记录，有则拒绝删除
      - 逻辑删除 `del_flag=2`

- [ ] T020 [US4] 实现 `PUT /api/v1/merchant/coupon/template/status`（切换启用/禁用）
      - 校验模板属于当前商家
      - 切换 `status` 0↔1

**Checkpoint**: 商家可完整管理自己的优惠券模板

---

## Phase 7: User Story 5 - 下单集成锁定/核销/释放优惠券 (Priority: P3)

**Goal**: 订单服务通过 Feign 调用 coupon 服务完成锁定、核销、释放

**Independent Test**: 调用 `POST /inner/coupon/lock` 锁定 → 调用 `POST /inner/coupon/release` 释放 → 状态回退正确

- [ ] T021 [US5] 扩展 `InnerCouponController` 新增 `consume()` 核销端点
      文件: `share-modules/share-coupon/src/main/java/com/share/coupon/controller/InnerCouponController.java`
      - `@InnerAuth @PostMapping("/consume")`
      - 参数：`{couponUserId, orderNo}`
      - 校验 status=1 AND order_no=orderNo，更新 `used_time=now`

- [ ] T022 [US5] 将 `consume` 方法加入 `RemoteCouponService` Feign 接口
      文件: `share-api/share-api-coupon/src/main/java/com/share/coupon/api/RemoteCouponService.java`
      - `@PostMapping("/inner/coupon/consume")`

- [ ] T023 [US5] 将 `consume` 降级加入 `RemoteCouponFallbackFactory`
      文件: `share-api/share-api-coupon/src/main/java/com/share/coupon/factory/RemoteCouponFallbackFactory.java`
      - 降级返回 `R.fail("优惠券核销失败")`

**Checkpoint**: 所有优惠券状态变更接口完整可用

---

## Phase 8: Polish & 收尾

**Purpose**: 编译验证、代码检查

- [ ] T024 [P] 编译验证 coupon 模块：`mvn compile -pl share-modules/share-coupon -am -q`
- [ ] T025 [P] 编译验证 share-api-coupon 模块：`mvn compile -pl share-api/share-api-coupon -am -q`
- [ ] T026 [P] LSP diagnostics 检查所有新建/修改文件

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 直接跳过
- **Foundational (Phase 2)**: T002-T004 可并行，必须在 US1-US5 之前完成
- **US1-3 (Phase 3-5)**: 共用 `CouponApiController`，建议顺序实现
- **US4 (Phase 6)**: 独立于 US1-3，可与 Phase 3-5 并行
- **US5 (Phase 7)**: 依赖 T002 Feign 扩展，独立于 US1-4
- **Polish (Phase 8)**: 所有 Phase 完成后执行

### Parallel Opportunities

- T002, T003, T004 可并行（不同文件）
- T005, T008, T010, T011, T013, T014 可并行（VO/DTO/Controller 骨架）
- US4 (Phase 6) 完全独立于 US1-3
- US5 (Phase 7) 独立于 US1-4
- T024, T025 可并行

---

## Implementation Strategy

### MVP First (Phase 3+4: US1+US2)

1. Phase 2: Feign 扩展 + 网关路由
2. Phase 3: US1 可领取列表 + 领取
3. Phase 4: US2 我的优惠券
4. **VALIDATE**: 业主完整领券流程可跑通

### Incremental Delivery

1. Phase 2 → Feign 可用
2. Phase 3+4 → 业主领券 + 查看（MVP）
3. Phase 5 → 下单可用券筛选
4. Phase 6 → 商家模板管理
5. Phase 7 → 订单集成
6. Phase 8 → 验证
