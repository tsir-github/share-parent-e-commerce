# Tasks: 秒杀活动管理后台

**Input**: Design documents from `specs/014-flash-sale-admin/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**Tests**: 本项目不使用自动测试框架，验证方式为编译检查 + 自测

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1-US5)
- Include exact file paths

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: share-goods 模块已就绪，无需初始化。需要执行数据库 DDL 改造现有 `seckill_activity` 表。

- [ ] T001 在 share-goods 数据库执行 `seckill_activity` 表 DDL 改造（新增字段+索引）
       DDL 参考: `specs/014-flash-sale-admin/data-model.md`
       操作: 连接 share-goods 库，ALTER TABLE 补充 limit_per_user/merchant_id/version/sort/create_by/update_by/update_time/del_flag/remark 字段，新增 5 个索引

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entity/Mapper/Service 骨架，必须在用户故事开始前完成

- [ ] T002 [P] 创建 `SeckillActivity` Entity
       文件: `share-api/share-api-goods/src/main/java/com/share/goods/domain/SeckillActivity.java`
       继承 `BaseEntity`，@TableName("seckill_activity")，包含所有字段 + 非数据库关联字段（productName/skuSpecs/mainImage）
       参照 `Product.java` 的 Entity 风格

- [ ] T003 [P] 创建 `SeckillActivityMapper`
       文件: `share-modules/share-goods/src/main/java/com/share/goods/mapper/SeckillActivityMapper.java`
       继承 `BaseMapper<SeckillActivity>`，标注 `@Mapper`

- [ ] T004 [P] 创建 Mapper XML
       文件: `share-modules/share-goods/src/main/resources/mapper/goods/SeckillActivityMapper.xml`
       只声明 `resultMap` 和基础字段映射，关联查询语句放在 US2 实现

- [ ] T005 [P] 创建 `ISeckillActivityService` 接口
       文件: `share-modules/share-goods/src/main/java/com/share/goods/service/ISeckillActivityService.java`
       继承 `IService<SeckillActivity>`，声明标准 CRUD + 自定义方法签名

- [ ] T006 [P] 创建 `SeckillActivityServiceImpl`
       文件: `share-modules/share-goods/src/main/java/com/share/goods/service/impl/SeckillActivityServiceImpl.java`
       继承 `ServiceImpl<SeckillActivityMapper, SeckillActivity>` 实现 `ISeckillActivityService`

**Checkpoint**: 秒杀活动 Entity/Mapper/Service 骨架搭建完成，可编译通过

---

## Phase 3: User Story 1 — 管理员创建秒杀活动 (Priority: P1) 🎯 MVP

**Goal**: 管理员选择商品/SKU，设置秒杀参数，创建新活动

**Independent Test**: 调用 `POST /seckillActivity` 传入完整活动参数，返回成功且列表中出现该活动

### Implementation for User Story 1

- [ ] T007 [P] [US1] 创建 `SeckillActivityController`
       文件: `share-modules/share-goods/src/main/java/com/share/goods/controller/SeckillActivityController.java`
       继承 `BaseController`，`@RequestMapping("/seckillActivity")`，注入 `ISeckillActivityService`
       参照 `CouponTemplateController.java` 的 admin 风格

- [ ] T008 [US1] 实现 `POST /seckillActivity` 新增接口
       方法: `add(@RequestBody SeckillActivity activity)`
       权限: `@RequiresPermissions("goods:seckill:add")` + `@Log`
       校验: 秒杀价>0, endTime>startTime, 商品/SKU 存在且已上架
       时间冲突检测: 同一 SKU 在起止时间范围内不能有冲突的进行中/未开始活动
       自动填充: `merchantId` 从 `product` 表查询 `merchant_id` 回填
       返回: `toAjax(...)`

- [ ] T009 [US1] 在 `ISeckillActivityService` 和 `ServiceImpl` 中实现新增校验逻辑
       方法: `checkAndCreate(SeckillActivity activity)`
       包括: 商品/SKU 存在性校验、秒杀价>0 校验、时间冲突检测（SQL 查询时间重叠的活动）

- [ ] T010 [P] [US1] 实现 `GET /seckillActivity/product/list` 辅助接口（创建活动时选择商品）
       方法: `listProducts(...)`，支持按名称搜索商品
        返回商品列表含 SKU 信息（id/name/image/minPrice/maxPrice + skus）
       参照 `ProductController` 的查询模式

**Checkpoint**: 管理员可通过 API 创建秒杀活动，创建时校验通过

---

## Phase 4: User Story 2 — 管理员查看活动列表 (Priority: P1)

**Goal**: 管理员分页查看活动列表，按名称/状态/时间筛选，查看详情

**Independent Test**: 调用 `GET /seckillActivity/list?pageNum=1&pageSize=10` 返回活动分页数据

### Implementation for User Story 2

- [ ] T011 [US2] 实现 `GET /seckillActivity/list` 分页列表接口
       方法: `list(SeckillActivity activity)`
       权限: `@RequiresPermissions("goods:seckill:list")`
       支持分页: `startPage()` + `getDataTable(list)`
       筛选条件: `name` 模糊搜索, `status` 精确匹配, `params[beginTime]/params[endTime]` 时间范围
       JOIN 查询: 关联 `product` 表取出 `product_name`、`main_image`，关联 `product_sku` 取出 `sku_specs`
       排序: 按 `sort` 降序、`create_time` 降序
       状态自动判断: 查询时 `status='1'` 但 `end_time < now` 自动转为已结束

- [ ] T012 [US2] 在 ServiceImpl 中实现列表查询 SQL（Mapper XML 中写 JOIN）
       SQL: `SELECT a.*, p.name AS product_name, p.main_image, ps.specs AS sku_specs FROM seckill_activity a LEFT JOIN product p ON a.product_id = p.id LEFT JOIN product_sku ps ON a.sku_id = ps.id WHERE a.del_flag = '0' ...`
       在 `SeckillActivityMapper.xml` 中定义

- [ ] T013 [US2] 实现 `GET /seckillActivity/{id}` 详情接口
       方法: `getInfo(@PathVariable Long id)`
       权限: `@RequiresPermissions("goods:seckill:query")`
       返回: `success(service.getById(id))`

**Checkpoint**: 管理员可查看活动列表和详情，支持筛选分页

---

## Phase 5: User Story 4 — 管理员启用/禁用活动 (Priority: P1)

**Goal**: 管理员手动控制活动上下架状态

**Independent Test**: 创建一个活动后调用 `PUT /seckillActivity/status` 切换状态，再次查询发现状态已变更

### Implementation for User Story 4

- [ ] T014 [P] [US4] 实现 `PUT /seckillActivity/status` 启用/禁用接口
       方法: `updateStatus(@RequestBody Map<String, Object> params)`
       权限: `@RequiresPermissions("goods:seckill:edit")` + `@Log`
       参数: `{id: Long, status: String}`
       状态流转校验:
         - 未开始(0) → 进行中(1) 或 已禁用(3)
         - 进行中(1) → 已禁用(3)（禁用后需清除C端缓存，后续实现）
         - 已禁用(3) → 进行中(1)（需校验当前时间在活动时间范围内）
         - 已结束(2) 不可变更
       使用 `LambdaUpdateWrapper` 更新（禁止 `updateById`）
       返回: `toAjax(updated)`

- [ ] T015 [US4] 在 ServiceImpl 中实现状态流转校验逻辑
       方法: `updateStatus(Long id, String newStatus)`
       查询当前活动状态和时间，判断是否允许流转，抛出 `ServiceException` 或执行更新

**Checkpoint**: 管理员可手动启用/禁用秒杀活动

---

## Phase 6: User Story 3 — 管理员编辑和删除活动 (Priority: P2)

**Goal**: 管理员修改活动参数或删除不再需要的活动

**Independent Test**: 修改活动秒杀价后查询确认变更，删除无记录的活动确认不可见

### Implementation for User Story 3

- [ ] T016 [P] [US3] 实现 `PUT /seckillActivity` 编辑接口
       方法: `edit(@RequestBody SeckillActivity activity)`
       权限: `@RequiresPermissions("goods:seckill:edit")` + `@Log`
       限制:
         - 进行中的活动不允许修改秒杀价和起止时间，只允许增加库存
         - 未开始的活动可修改全部参数（重新校验时间冲突）
       使用 `LambdaUpdateWrapper` 按需更新字段（禁止 `updateById`）

- [ ] T017 [P] [US3] 实现 `DELETE /seckillActivity/{ids}` 删除接口
       方法: `remove(@PathVariable Long[] ids)`
       权限: `@RequiresPermissions("goods:seckill:remove")` + `@Log`
       限制:
         - 检查是否有配合记录（预留后续 `seckill_record` 表关联）
         - 逻辑删除 `del_flag=2`
       批量处理

**Checkpoint**: 管理员可编辑/删除秒杀活动

---

## Phase 7: User Story 5 — 管理员查看活动统计 (Priority: P3)

**Goal**: 管理员查看每个秒杀活动的参与数据

**Independent Test**: 调用 `GET /seckillActivity/stats/{id}` 返回统计 JSON

### Implementation for User Story 5

- [ ] T018 [US5] 实现 `GET /seckillActivity/stats/{id}` 统计接口
       方法: `stats(@PathVariable Long id)`
       权限: `@RequiresPermissions("goods:seckill:list")`
       返回统计: 总售出数量、总订单数、已支付数、支付率、总销售额
       ⚠️ 当前返回占位数据（`{"totalSales":0,"totalOrders":0,"paidOrders":0,"paymentRate":"0.00%","totalRevenue":0.00}`）
       **完整实现依赖**: 后续 C 端秒杀下单 + `seckill_record` 表 + 订单数据写入
       返回: `success(statsMap)`

**Checkpoint**: 统计接口返回占位数据，接口结构可用

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 编译验证、日志完善、校验补充

- [ ] T019 [P] 编译验证 share-goods 模块：`mvn compile -pl share-modules/share-goods -am -q`
- [ ] T020 [P] 编译验证 share-api-goods 模块：`mvn compile -pl share-api/share-api-goods -am -q`
- [ ] T021 检查所有 Controller 是否遵循 admin 规范（`BaseController`, `AjaxResult`/`TableDataInfo`, `@RequiresPermissions`, `@Log`）
- [ ] T022 检查所有 `LambdaUpdateWrapper` 更新（禁止 `updateById` 更新状态字段）
- [ ] T023 按 `contracts/api.md` 验证所有接口行为与契约一致

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: DDL 改造，必须先于 Foundational
- **Foundational (Phase 2)**: 依赖 Setup，BLOCKS 所有民故事
- **US1 (Phase 3)**: 依赖 Foundational，创建活动的 Service 方法被 US3/US4 复用
- **US2 (Phase 4)**: 依赖 Foundational，可独立于 US1 并行
- **US4 (Phase 5)**: 依赖 Foundational + US1 Service，需活动已创建才能启用/禁用
- **US3 (Phase 6)**: 依赖 Foundational + US1 Service，需活动已创建才能编辑/删除
- **US5 (Phase 7)**: 依赖 Foundational + US1 Entity，仅需 Entity 存在
- **Polish (Phase 8)**: 所有 Phase 完成后执行

### User Story Dependencies

- **User Story 1 (P1)**: 可独立启动（MVP）
- **User Story 2 (P1)**: 依赖 Foundational，独立于 US1（同一个 Entity/Service）
- **User Story 4 (P1)**: 依赖 US1 的 Entity 和 Service
- **User Story 3 (P2)**: 依赖 US1 的 Entity 和 Service
- **User Story 5 (P3)**: 依赖 Foundational 即可

### Parallel Opportunities

- T002, T003, T004, T005, T006 可并行（Entity/Mapper/XML/Service 接口/Service 实现 — 不同文件）
- T007, T010 可并行（Controller 骨架 + 商品列表辅助接口）
- US1 (Phase 3) 和 US2 (Phase 4) 可部分并行（US1 的 POST 与 US2 的 GET 列表可同时开发）
- US3 和 US4 可并行（编辑删除 与 状态切换，不同方法）
- T019, T020 可并行（两个模块独立编译）

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1: DDL 改造 seckill_activity 表
2. Phase 2: Entity/Mapper/Service 骨架
3. Phase 3: US1 创建活动（含商品列表辅助接口）
4. Phase 4: US2 列表+详情
5. **VALIDATE**: 完整的活动创建→查看流程可跑通

### Incremental Delivery

1. Phase 1+2 → 数据层就绪
2. Phase 3+4 → 创建+查看（MVP）
3. Phase 5 → 启用/禁用
4. Phase 6 → 编辑/删除
5. Phase 7 → 统计（占位）
6. Phase 8 → 验证

### Parallel Execution Example: Phase 2 Foundational

```bash
# 同时启动 5 个任务（不同文件，无依赖）
Task: "创建 SeckillActivity Entity"
Task: "创建 SeckillActivityMapper"
Task: "创建 Mapper XML"
Task: "创建 ISeckillActivityService 接口"
Task: "创建 SeckillActivityServiceImpl"
```

### Parallel Execution Example: Phase 3 (US1)

```bash
# 同时启动（不同文件，无依赖）
Task: "创建 SeckillActivityController 骨架"
Task: "实现 GET /seckillActivity/product/list 辅助接口"
```
