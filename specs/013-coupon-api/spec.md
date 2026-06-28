# Feature Specification: 优惠券功能 API

**Feature Branch**: `013-coupon-api`

**Created**: 2026-06-28

**Status**: Draft

**Input**: User description: "优惠券功能 开发这个接口，自己判断属于哪个角色、缺不缺表/字段。请贴合此项目并摸清此接口痛点，最后解决。在写代码前读agent.md的代码开发约束。"

## 现有模块审计摘要

`share-coupon` 模块已构建完成（非骨架），但有缺口：

**已有的**：
- `coupon_template` + `coupon_user` 两张表结构完整，含乐观锁 `version`、逻辑删除 `del_flag`
- `ICouponUserService.claimCoupon()` 领券（Redisson 锁 + 乐观锁 + 限领校验）
- `ICouponUserService.lockForOrder()` 锁定（下单占用）
- `ICouponUserService.releaseForOrder()` / `releaseByOrderNo()` 释放（取消退还）
- `ICouponUserService.countAvailableByUserId()` 可用数量
- `CouponTemplateController` B端 CRUD（AjaxResult + `@RequiresPermissions`）
- `InnerCouponController` 内部 Feign（lock/release/releaseByOrderNo/countAvailable）

**缺口的**：
1. ❌ **无 C端 API** — 业主无法领取优惠券、查看我的优惠券、查看下单可用券
2. ❌ **无 商家 API** — 商家无法创建/管理自己的优惠券模板
3. ❌ **Feign 接口不完整** — `RemoteCouponService` 只有 `releaseByOrderNo`，缺 `lock`/`release`/`countAvailable`
4. ❌ **下单未集成** — `OrderInfoServiceImpl` 中锁定优惠券的调用标注了 `// ponytail: 待集成`
5. ❌ **网关路由缺失** — `/coupon/**` → `lb://share-coupon` 未在网关配置
6. ❌ **无 XML Mapper** — 虽非必需，但 `CouponTemplateServiceImpl.save()` 写死了 `merchantId=1`

**不缺表/字段** — 现有表结构已经满足本次功能需求。

## User Scenarios & Testing

### User Story 1 — C端业主：领取优惠券（Priority: P1）🎯 MVP

业主在优惠券列表页看到可领取的优惠券模板，点击领取。领取后优惠券进入"我的优惠券"中。

**痛点解决**：
- 并发抢券 → Redisson 锁 + 乐观锁 `version` 已实现
- 重复领取 → `limit_per_user` 限制已实现
- 库存不足 → `remain_count` 校验已实现
- 过期模板不展示 → 按 `start_time/end_time` + `status=1` 过滤

**Independent Test**: 登录后请求可领取列表，点击领取，再进"我的优惠券"看到新增记录。

**Acceptance Scenarios**:

1. **Given** 业主已登录，**When** 请求可领取优惠券列表，**Then** 返回所有状态=启用且在有效期内的模板
2. **Given** 业主选择某模板点击领取，**When** 领取成功，**Then** 返回成功，该优惠券进入"我的优惠券"
3. **Given** 业主已领满某模板的 `limit_per_user`，**When** 再次领取该模板，**Then** 返回业务错误提示"已达领取上限"
4. **Given** 某模板 `remain_count=0`，**When** 用户领取，**Then** 返回业务错误提示"优惠券已领完"

---

### User Story 2 — C端业主：查看我的优惠券（Priority: P1）

业主在个人中心进入"我的优惠券"，分页查看自己的优惠券，可筛选状态（未使用/已使用/已过期）。

**痛点解决**：
- 分页查询 → PageHelper 分页，按领取时间倒序
- 状态筛选 → 前端传 status 参数，0-未使用 1-已使用 2-已过期
- 显示模板信息 → JOIN `coupon_template` 展示券名称/类型/金额/有效期

**Independent Test**: 领取2张券后进入我的优惠券页，显示2条记录，含券信息。

**Acceptance Scenarios**:

1. **Given** 业主有若干优惠券，**When** 进入"我的优惠券"，**Then** 分页展示优惠券列表（模板名称、类型、金额、有效期、状态）
2. **Given** 业主筛选"未使用"，**When** 查看列表，**Then** 只返回 status=0 的记录
3. **Given** 业主的优惠券已过期，**When** 查看列表，**Then** 状态标记为"已过期"

---

### User Story 3 — C端业主：查看下单可用优惠券（Priority: P2）

业主在结算页面选择优惠券，接口根据订单金额返回当前可用的优惠券列表。

**痛点解决**：
- 满减券校验 → `condition_amt` 条件过滤（订单金额 >= condition_amt）
- 折扣券校验 → 同样需要满足 condition_amt
- 无门槛券 → type=2 没有 condition_amt 限制
- 仅展示已领取且未使用的 → 自己的 `coupon_user` 记录中 status=0
- 按优惠力度排序 → 减免金额/折扣率降序

**Independent Test**: 有一张满100减10的券，结算100元商品时显示可用；结算50元时不显示。

**Acceptance Scenarios**:

1. **Given** 业主有未使用的满100-10优惠券，**When** 结算101元订单查询可用券，**Then** 该券出现在可用列表
2. **Given** 业主有未使用的满100-10优惠券，**When** 结算50元订单查询可用券，**Then** 该券不出现
3. **Given** 业主有未使用的折扣券，**When** 结算满足条件时，**Then** 展示折扣率和最高减免金额

---

### User Story 4 — 商家：管理优惠券模板（Priority: P2）

商家在后台创建优惠券模板，包括名称、类型、金额、总量、有效期等，支持查看/编辑/删除自己的模板。

**痛点解决**：
- 只能管理自己的 → 按 `merchant_id` = 当前商家用户过滤
- 已发放的模板不可删除 → 先检查 `coupon_user` 是否有记录
- 剩余量不足的模板不可减量 → 已发放 > 新总量时拒绝
- 状态缺省为禁用 → 创建后 `status=0`，手动启用

**Independent Test**: 商家创建一张满200减20的券，在列表看到自己的模板。

**Acceptance Scenarios**:

1. **Given** 商家用户登录，**When** 创建优惠券模板，**Then** 创建成功，状态为禁用
2. **Given** 商家查看模板列表，**When** 请求列表，**Then** 只返回自己（merchant_id）的模板
3. **Given** 商家修改自己的模板，**When** 减少总量到低于已发放数量，**Then** 拒绝修改并提示
4. **Given** 商家删除模板，**When** 模板已有用户领取，**Then** 拒绝删除（逻辑删除置 del_flag=2）

---

### User Story 5 — 下单集成：锁定优惠券（Priority: P3）

用户在创建订单时，传入使用的优惠券ID，订单服务通过 Feign 调用 coupon 服务锁定优惠券。

**痛点解决**：
- 防并发 → Redisson 锁 `coupon:lock:{couponUserId}`
- 幂等 → status=0 时才允许锁定，重复锁定失败
- Feign 熔断 → `RemoteCouponFallbackFactory` 降级处理

**Independent Test**: 下单时传入优惠券ID，订单创建后优惠券变为"已使用"状态。

**Acceptance Scenarios**:

1. **Given** 用户下单时传入优惠券ID，**When** 订单创建成功，**Then** 优惠券状态变为"已锁定"（status=1 + orderNo）
2. **Given** 用户取消订单，**When** 取消操作完成，**Then** 优惠券状态回退为"未使用"（调用 releaseByOrderNo）

### Edge Cases

- 优惠券在领取瞬间被抢光 → `remain_count` 乐观锁失败 → 返回"已领完"提示
- 用户领取时优惠券模板刚过期 → 校验有效期 + `status=1` + 乐观锁三重保障
- 下单时优惠券已被其他订单锁定 → `lockForOrder` 的 status=0 条件防止重复锁定
- 优惠券过期后仍在"未使用"列表 → 查询时自动过滤已过期的（end_time < now），并在领券时校验
- 商家删除模板不影响已领取用户 → 只做逻辑删除，已有 `coupon_user` 可正常使用
- 商家禁用模板后已领取的券 → 已领取的不受影响，但新领取时校验 `status=1`

## Requirements

### Functional Requirements

- **FR-001**: 系统必须支持 C端业主分页查询可领取的优惠券模板列表（仅返回 status=1 且在有效期内的）
- **FR-002**: 系统必须支持 C端业主领取优惠券（含并发控制、限领校验、余量扣减）
- **FR-003**: 系统必须支持 C端业主分页查询我的优惠券（支持按 status 筛选，JOIN 模板信息）
- **FR-004**: 系统必须支持 C端业主查询下单可用优惠券（按订单金额过滤满减条件）
- **FR-005**: 系统必须支持商家用户创建/查看/编辑/删除自己的优惠券模板
- **FR-006**: 系统必须支持订单服务通过 Feign 锁定/释放优惠券
- **FR-007**: 网关必须配置 `/coupon/**` 路由到 `share-coupon`

### Key Entities

- **CouponTemplate（已有）**: 优惠券模板，含类型（满减/折扣/无门槛）、金额条件、发行量、有效期、乐观锁
- **CouponUser（已有）**: 用户领取记录，关联模板ID、订单号、状态（未使用/已使用/已过期）
- **OrderInfo.couponIds（已有字段）**: 下单时存储使用的优惠券ID（逗号分隔）

## Database

现有表结构已满足需求，无需新增表或字段。

需注意 `coupon_template.remain_count` 默认 -1（不限量），`coupon_user` 中过期券由业务逻辑定时判断（end_time < now）而非单独字段标记。

```sql
-- 已存在，无需变更
CREATE TABLE `coupon_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '优惠券名称',
  `merchant_id` bigint NOT NULL DEFAULT 1 COMMENT '商家ID',
  `type` char(1) NOT NULL COMMENT '类型：0-满减 1-折扣 2-无门槛',
  `condition_amt` decimal(10,2) DEFAULT NULL COMMENT '满减条件金额',
  `discount_amt` decimal(10,2) DEFAULT NULL COMMENT '减免金额（满减/无门槛）',
  `discount_rate` decimal(3,2) DEFAULT NULL COMMENT '折扣率（折扣券）',
  `total_count` int NOT NULL COMMENT '发行总量',
  `limit_per_user` int DEFAULT 1 COMMENT '每人限领数量',
  `start_time` datetime NOT NULL COMMENT '有效期开始',
  `end_time` datetime NOT NULL COMMENT '有效期结束',
  `status` char(1) DEFAULT '0' COMMENT '状态：0-禁用 1-启用',
  `remain_count` int DEFAULT -1 COMMENT '剩余数量（-1不限量）',
  `version` int DEFAULT 0 COMMENT '乐观锁',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_time` (`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE `coupon_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `template_id` bigint NOT NULL COMMENT '优惠券模板ID',
  `order_no` varchar(32) DEFAULT NULL COMMENT '使用订单号',
  `status` char(1) DEFAULT '0' COMMENT '状态：0-未使用 1-已使用 2-已过期',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '领取时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_order` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';
```

## Success Criteria

- **SC-001**: 领取操作在 1 秒内响应（含 Redis 锁 + DB 乐观锁）
- **SC-002**: 我的优惠券列表分页查询在 1 秒内返回
- **SC-003**: 高并发下不会出现超发（乐观锁保障）
- **SC-004**: 下单锁定优惠券后，取消订单自动释放，状态回退正确
- **SC-005**: 所有新加接口编译通过，无 LSP 错误

## Assumptions

- 优惠券领取面向 C端业主（`@RequiresLogin`），商家和平台管理员不直接领券
- 商家只能管理自己 `merchant_id` 的优惠券模板
- 过期券不做定时批量处理，查询时实时过滤 `end_time < now`
- 下单锁定优惠券时，每个订单最多使用一张优惠券（`couponIds` 逗号分隔支持多张但当前业务仅用一张）
- `CouponTemplateServiceImpl.save()` 中 `merchantId=1` 的硬编码本次不修改（B端 Controller 会走正常流程）
