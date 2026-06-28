# Feature Specification: 项目全面审计与基线

**Feature Branch**: `001-project-audit`

**Created**: 2026-06-23

**Status**: Draft

**Input**: User description: "扫描现有代码、理解已实现功能、写规格文档列出已完成的 + 待做功能"

## User Scenarios & Testing

### User Story 1 — 管理员后台管理电商业务 (Priority: P1)

小区物业管理员登录后台，管理商品、订单、优惠券、商家等核心电商业务。

**Why this priority**: 这是项目的核心价值——社区电商运营需要完整的后台管理能力。

**Independent Test**: 管理员成功创建商品→上架→用户下单→支付→发货→完成，全链路走通。

**Acceptance Scenarios**:

1. **Given** 管理员有登录权限，**When** 访问 /goods/product 页面，**Then** 能看到商品列表并新增/编辑商品
2. **Given** 管理员查看订单列表，**When** 点击订单详情，**Then** 能看到完整的订单信息和状态流转
3. **Given** 管理员需要管理优惠券，**When** 创建优惠券模板，**Then** 用户可领券并使用

---

### User Story 2 — C 端用户购物下单 (Priority: P1)

小区居民通过微信小程序浏览商品、领券、下单、支付、收货。

**Why this priority**: 电商平台的最终价值在于 C 端用户使用。C 端应用不在本仓库（独立小程序项目），但后端 API 需完整支撑。

**Independent Test**: 用户通过小程序登录→浏览商品→领券→下单→支付→查看订单状态。

**Acceptance Scenarios**:

1. **Given** 用户打开小程序，**When** 微信授权登录，**Then** 后端返回用户信息并自动注册
2. **Given** 用户选择商品，**When** 提交订单，**Then** 订单创建成功并锁定库存
3. **Given** 用户发起支付，**When** 调起微信支付，**Then** 支付成功更新订单状态
4. **Given** 用户确认收货，**When** 点击确认，**Then** 订单状态变为已完成

---

### User Story 3 — 商家入驻与管理 (Priority: P2)

商家在平台注册、提交资料、接受审核、管理自己的商品和订单。

**Why this priority**: 平台需要商家丰富商品供给，但首要阶段可平台自营。

**Independent Test**: 商家提交入驻申请→管理员审核通过→商家登录管理商品。

**Acceptance Scenarios**:

1. **Given** 商家提交入驻申请，**When** 填写信息并提交，**Then** 平台收到待审核申请
2. **Given** 管理员审核商家，**When** 审核通过，**Then** 商家状态变为已启用
3. **Given** 商家已入驻，**When** 管理商品，**Then** 只能操作自己的商品

---

### User Story 4 — 系统运维监控 (Priority: P3)

系统管理员监控服务状态、查看操作日志、管理用户权限。

**Why this priority**: 基础设施已在 RuoYi 框架中完成，无需额外开发。

**Independent Test**: 管理员查看操作日志、缓存管理、服务监控页面正常展示。

**Acceptance Scenarios**:

1. **Given** 管理员，**When** 访问操作日志页面，**Then** 看到操作记录
2. **Given** 系统异常，**When** 查看监控，**Then** 能定位问题

---

### Edge Cases

- 高并发下单时库存扣减是否原子？-- 乐观锁 3 次重试兜底
- 微信支付回调超时或重复通知是否幂等？-- paymentNo 唯一索引保证
- 订单超时取消与用户手动取消同时触发？-- 状态机校验+乐观锁保证
- 优惠券超发？-- TODO: 当前无并发控制，需要 Redisson 锁
- 退款时微信已扣但 DB 失败？-- 当前退款仅更新 DB，TODO: 调微信退款 API
- 分布式环境下定时任务重复执行？-- 未使用 XXL-Job，@Scheduled 需加分布式锁

## Requirements

### Functional Requirements

- **FR-001** [✅ 已实现]: 系统 MUST 支持微信小程序登录（code → openid → 注册/登录）
- **FR-002** [✅ 已实现]: 系统 MUST 支持商品分类管理（树形结构 CRUD）
- **FR-003** [✅ 已实现]: 系统 MUST 支持商品 SPU + SKU 管理（多规格商品）
- **FR-004** [✅ 已实现]: 系统 MUST 支持库存扣减（乐观锁 + 3 次重试）
- **FR-005** [✅ 已实现]: 系统 MUST 支持订单全生命周期（待支付→待发货→配送中→已完成→售后）
- **FR-006** [✅ 已实现]: 系统 MUST 支持订单状态机（校验转换合法性 + 乐观锁 + 日志流水）
- **FR-007** [✅ 已实现]: 系统 MUST 支持订单超时取消（RocketMQ 延迟消息 30 分钟）
- **FR-008** [✅ 已实现]: 系统 MUST 支持微信支付 v3（统一下单 + 回调验签 + mock 模式）
- **FR-009** [✅ 已实现]: 系统 MUST 支持支付成功回调通知订单模块（RocketMQ）
- **FR-010** [✅ 已实现]: 系统 MUST 支持优惠券模板 CRUD
- **FR-011** [⚠️ 部分实现]: 系统 MUST 支持领券（TODO: 并发控制、有效期校验、发行量校验、人限校验）
- **FR-012** [✅ 已实现]: 系统 MUST 支持优惠券锁定/释放（下单锁定，取消释放）
- **FR-013** [⚠️ 部分实现]: 系统 MUST 支持商家入驻（TODO: 审核流程需完善）
- **FR-014** [❌ 未实现]: 系统 MUST 支持订单取消时回滚库存（ponytail 注释标记）
- **FR-015** [❌ 未实现]: 系统 MUST 支持售后/退款流程（状态机定义但无实现）
- **FR-016** [⚠️ 部分实现]: 系统 MUST 支持退款（TODO: 调微信退款 API）
- **FR-017** [❌ 未实现]: 系统 MUST 支持 7 天自动确认收货（MQ Topic 定义但无消费者）
- **FR-018** [⚠️ 部分实现]: 系统 MUST 支持后端管理员登录 + 权限控制（RuoYi 框架，无电商页面）
- **FR-019** [❌ 待开发]: C 端 MUST 能浏览商品列表和详情
- **FR-020** [❌ 待开发]: C 端 MUST 能领券并使用

### Key Entities

#### 商品域
- **Category**: 商品分类树（parentId 递归），用于前台导航和后台管理
- **Product**: 商品 SPU（标准商品单元），关联 Category + Merchant，有上下架状态
- **ProductSku**: 商品 SKU（库存量单元），关联 Product，多规格（specs JSON），有乐观锁 version
- **ProductImage**: 商品图片（表已建，无 Service）
- **SeckillActivity**: 秒杀活动（表已建，无 Service）

#### 订单域
- **OrderInfo**: 主订单，含金额、状态、收货信息、物流信息
- **OrderItem**: 订单明细（快照商品信息、数量、单价）
- **OrderLog**: 订单操作日志（状态变更流水，由状态机自动写入）
- **Cart**: 购物车（表已建，TODO: 关联用户+SKU）
- **OrderBill**: 账单信息（配送员、预计送达等，表已建）

#### 支付域
- **PaymentInfo**: 支付记录（支付单、金额、状态、微信 transactionId）
- **状态**: 待支付 → 已支付 → 已退款 / 部分退款

#### 用户域
- **UserInfo**: C 端用户（微信 openId + 基本资料）
- **UserAddress**: 收货地址（表已建，无 Service）
- **UserLoginLog**: 登录日志

#### 优惠券域
- **CouponTemplate**: 优惠券模板（面额、门槛、有效期、发行量）
- **CouponUser**: 用户持有的优惠券（状态：未使用→已锁定→已使用/已释放/已过期）

#### 商家域
- **MerchantInfo**: 商家信息（名称、联系人、状态：待审核/已启用/已关闭）
- **MerchantUser**: 商家用户（表已建，无 Service）

#### 系统域（RuoYi 框架）
- **SysUser / SysRole / SysMenu / SysDept / SysPost**: 标准 RBAC 权限体系
- **SysConfig / SysDictData**: 系统配置 + 数据字典
- **SysOperLog / SysLogininfor**: 操作日志 + 登录日志

## Success Criteria

### Measurable Outcomes

- **SC-001**: 90% 的 P0 功能在现有代码基础上可直接运行（编译通过、单元测试覆盖核心链路）
- **SC-002**: 订单创建响应时间 < 500ms（含 Feign 调用扣库存 + 发 MQ 延迟消息）
- **SC-003**: 优惠券并发领取无超发（领券接口集成 Redisson 锁后验证）
- **SC-004**: 订单取消后 100% 库存回滚 + 优惠券释放（当前为 0%）
- **SC-005**: 所有业务模块 Controller/Service/Mapper XML 齐全无缺失

## Assumptions

- **C 端应用在独立项目**：本仓库仅含管理后台 + 后端 API。微信小程序等 C 端前端在另一个仓库（或使用 uniapp 构建）
- **数据库表结构已冻结**：不再修改现有表结构，除非有重大理由
- **现有乐观锁方案足够**：version 乐观锁 + 3 次重试是默认并发策略，除非压测显示不足
- **Mock 模式用于开发**：在 mockMode=true 时，支付/退款走模拟流程，不调用微信真实 API
- **RocketMQ 作为异步消息骨干**：订单超时、支付回调等异步场景通过 RocketMQ 解耦
- **Seata 可用但未使用**：分布式事务框架已集成，但当前业务未使用 @GlobalTransactional（单模块事务为主）
- **Sentinel 已集成但未配置规则**：Flow/Rule 在 Nacos 中配置，当前无熔断降级规则

## 模块成熟度总览

| 模块 | 成熟度 | 状态标签 | 核心缺口 |
|------|--------|----------|----------|
| share-order | 🟢 最完整 | 核心链路可用 | 取消回滚+MISSING接口+SQL注入 |
| share-payment | 🟡 主干完整 | 可用(mock) | 退款未调微信API+无Mapper XML |
| share-goods | 🟡 CRUD完整 | 基础可用 | 缺业务逻辑（秒杀/搜索/图片管理） |
| share-coupon | 🟡 流程定义好 | 核心不可用 | 4个TODO导致领券不可用 |
| share-user | 🟡 登录可用 | 核心可用 | 地址管理缺失 |
| share-merchant | 🔴 骨架 | 不可用 | 审核+TODO |
| share-file | 🟢 完整 | 可用 | FastDFS 已注释 |
| 前端(share-ui) | 🟢 RuoYi框架 | 可用 | 零电商页面，仅后台管理框架 |

## 已知缺陷清单

### P0 —— 必须修复
1. **IOrderInfoService 接口缺失**：`OrderTimeoutConsumer` 和 `PaymentSuccessConsumer` 引用了 `com.share.order.service.IOrderInfoService`，但此接口不存在（编译错误）
2. **领券无并发控制**：`CouponUserServiceImpl.claimCoupon` 有 4 个 TODO，高并发下必然超发
3. **优惠券 lockForOrder 使用 updateById 改状态**：违反编码规范，并发下状态回退
4. **OrderInfoServiceImpl.getOrderCount SQL 注入**：直接拼接 SQL 参数

### P1 —— 应修复
5. **取消订单无库存回滚**：`cancelOrder` 仅改状态，不调 goods 服务恢复库存
6. **取消订单无优惠券释放**：`cancelOrder` 不调 coupon 服务释放优惠券
7. **下单时未锁定优惠券**：`createOrder` 传入 couponId 但不校验/锁定
8. **退款未调微信 API**：`PaymentInfoServiceImpl.refund` 仅更新 DB 状态
9. **order-auto-confirm 无消费者**：Topic 定义了，30min 超时取消的相反方向（7天自动收货）未实现
10. **商家审核是 TODO**：`MerchantInfoController.audit` 只是简单 updateById

### P2 —— 应该补
11. **售后/退款流程未实现**：状态机支持 AFTER_SALE→REFUND 但无代码
12. **秒杀活动无逻辑**：`seckill_activity` 表存在，无 Service
13. **商品图片无管理**：`product_image` 表存在，无 Service
14. **用户地址无管理**：`user_address` 表存在，无 Service
15. **商家用户无管理**：`merchant_user` 表存在，无 Service
16. **前端零电商页面**：share-ui 没有商品/订单/商家等管理页面
