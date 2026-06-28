# Tasks: 多角色图片上传能力

**Input**: Design documents from `specs/008-multi-role-image-upload/`

**Prerequisites**: [spec.md](spec.md), [plan.md](plan.md), [data-model.md](data-model.md), [contracts/](contracts/), [research.md](research.md)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel
- **[Story]**: User story this task belongs to
- Include exact file paths

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 项目已存在，无需额外初始化。确认现有基础设施可用。

- [ ] T001 Verify share-file `POST /upload` 接口可用且 MinIO 配置正常
- [ ] T002 [P] 确认网关路由已配置: share-user(/user/**), share-merchant(/merchant/**), share-goods(/goods/**), share-order(/order/**)
- [ ] T003 [P] 确认 RemoteFileService Feign 接口可用（`share-api/share-api-system/.../api/RemoteFileService.java`）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 核心基础设施，所有用户故事依赖

**⚠️ CRITICAL**: 必须先完成本阶段

- [ ] T004 评估 share-file 是否需要扩展多文件上传（`share-modules/share-file/.../controller/SysFileController.java`），当前只支持单文件
- [ ] T005 确认各模块 pom.xml 依赖了 `share-api-system`（为使用 RemoteFileService Feign）
- [ ] T006 评估是否需要 `share-api-file` 模块（若当前 share-system api 中的 RemoteFileService 位置不合适则新建）

**Checkpoint**: 基础设施就绪，可以开始用户故事实现

---

## Phase 3: User Story 1 — C端业主更换头像 (Priority: P1) 🎯 MVP

**Goal**: C端业主可通过微信小程序更换个人头像

**Independent Test**: 登录小程序 → 个人中心 → 点击头像 → 选择图片上传 → 页面显示新头像

### Implementation

- [ ] T007 [P] [US1] 创建 `UserAvatarController` 在 `share-modules/share-user/.../controller/UserAvatarController.java`，提供 `POST /api/v1/user/avatar`
- [ ] T008 [US1] 实现头像上传服务: 接收 MultipartFile → 调用 RemoteFileService 上传 → 更新 `user_info.avatar_url` → 返回新URL
- [ ] T009 [US1] 更新 `UserInfoServiceImpl.wxLogin()`，首次注册时从微信获取真实头像而非硬编码默认值（`share-modules/share-user/.../service/impl/UserInfoServiceImpl.java`）

**Checkpoint**: US1 可独立测试验证

---

## Phase 4: User Story 2 — 商家商品图片上传 (Priority: P1)

**Goal**: 商家管理员可为商品上传主图和详情多图

**Independent Test**: 商家登录后台 → 新增/编辑商品 → 上传主图+多图 → 商品详情页展示

### Implementation

- [ ] T010 [P] [US2] 在 `MerchantProductController` 中新增图片上传端点（`share-modules/share-goods/.../controller/MerchantProductController.java`），支持 `POST /api/v1/merchant/product/{id}/images`
- [ ] T011 [US2] 实现商品图片上传服务: 接收 MultipartFile[] → 逐张调 RemoteFileService → type=main 时更新 `product.main_image` → 其余写入 `product_image` 表
- [ ] T012 [P] [US2] 确认 `ProductImage` 实体已存在且 MyBatis-Plus Mapper 可用（`share-goods` 模块内，表 `product_image` 已存在）

**Checkpoint**: US2 可独立测试验证

---

## Phase 5: User Story 3 — 商家上传店铺Logo (Priority: P2)

**Goal**: 商家可在店铺资料页更新Logo

**Independent Test**: 商家登录后台 → 店铺资料页 → 上传新Logo → 保存后店铺首页展示新Logo

### Implementation

- [ ] T013 [US3] 在 `MerchantProfileController` 中新增Logo上传端点（`share-modules/share-merchant/.../controller/MerchantProfileController.java`），`POST /api/v1/merchant/profile/logo`
- [ ] T014 [US3] 实现Logo上传: 接收 MultipartFile → 调 RemoteFileService → 更新 `merchant_info.logo`

**Checkpoint**: US3 可独立测试验证

---

## Phase 6: User Story 4 — C端评价晒图 (Priority: P2)

**Goal**: C端业主在确认收货后可对订单进行评价并上传晒图

**Independent Test**: 用户登录小程序 → 已完成订单 → 评价 → 上传最多9张晒图 → 提交后商品评价区展示

### Implementation

- [ ] T015 [P] [US4] 在 `share-order` 数据库执行建表 SQL: `review_image`（[建表语句见 data-model.md](data-model.md#5-reviewimage需新建)）
- [ ] T016 [P] [US4] 创建 `ReviewImage` 实体在 `share-modules/share-order/.../domain/ReviewImage.java`，`@TableName("review_image")`
- [ ] T017 [P] [US4] 创建 `ReviewImageMapper` 在 `share-modules/share-order/.../mapper/ReviewImageMapper.java`
- [ ] T018 [US4] 创建评价+晒图端点: `POST /api/v1/order/review/{orderItemId}/images`，接收 MultipartFile[] → 调 RemoteFileService → 插入 `review_image` 表

**Checkpoint**: US4 可独立测试验证

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 全局质量提升

- [ ] T019 [P] 复查所有新增接口的错误处理和校验（文件类型、大小、数量限制）
- [ ] T020 [P] 确认所有新增接口都加了 `@Operation(summary="...")` 和 `@RequiresLogin`
- [ ] T021 按 quickstart.md 逐条验证所有场景
- [ ] T022 检查网关路由配置是否需要更新（`share-gateway-dev.yml` 在 Nacos）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 快速检查，无依赖
- **Phase 2 (Foundational)**: 依赖 Phase 1 — 阻塞所有用户故事
- **Phase 3-6 (User Stories)**: 依赖 Phase 2 完成
  - Phase 3 (US1), 4 (US2), 5 (US3), 6 (US4) 可并行开发
- **Phase 7 (Polish)**: 依赖所有需要的用户故事完成

### User Story Dependencies

- **US1 (头像)**: 独立，无其他故事依赖
- **US2 (商品图片)**: 独立，无其他故事依赖
- **US3 (店铺Logo)**: 独立，无其他故事依赖
- **US4 (评价晒图)**: 独立，依赖评价功能存在

### Parallel Opportunities

- Phase 2 的 T004/T005/T006 可并行
- Phase 3-6 的所有用户故事可**完全并行**（不同模块，互不依赖）
- 同一故事内的 [P] 任务可并行

---

## Implementation Strategy

### MVP Scope (Phase 3 仅 US1)

1. Phase 1 (检查) → Phase 2 (评估) → Phase 3 (头像上传)
2. **停**：验证 US1 独立可用
3. 这是最小的 MVP：C端能换头像了

### 增量交付

1. **MVP**: US1 头像上传 → 验收
2. **Iteration 2**: US2 商品图片 → 验收
3. **Iteration 3**: US3 店铺Logo + US4 评价晒图 → 验收

### 并行策略

```bash
# Phase 2 可并行:
Task: T004 评估多文件上传
Task: T005 检查 pom.xml 依赖
Task: T006 评估 api-file 模块

# Us1, US2, US3, US4 完全并行（不同模块，无依赖冲突）:
Task: "US1 头像上传 → share-user"
Task: "US2 商品图片 → share-goods"
Task: "US3 店铺Logo → share-merchant"
Task: "US4 评价晒图 → share-order"
```

## Notes

- 无需新建微服务模块，所有代码在现有模块中新增 controller/service
- 上传底层统一走 share-file（MinIO），业务模块只存 URL
- `product_image` 表已存在，只需确认 Mapper 可用
- `review_image` 表需要新建
