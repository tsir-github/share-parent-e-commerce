# Research: 优惠券功能 API

**Phase 0 output** — 所有 NEEDS CLARIFICATION 已在审计中解决。

## 研究范围

通过背景 agent 对 coupon 模块的完整审计（代码结构 + DB schema + Feign 接口 + 跨模块引用）+ 订单模块集成点检查。

## 发现汇总

### 1. 模块状态：✅ 已构建（非骨架）

- `share-coupon` 端口 9214，有完整启动类、Mapper、Service、Controller
- DB `share-coupon` 库 3 张表：`coupon_template`, `coupon_user`, `undo_log`
- RocketMQ producer 已配置，Seata 已配置

### 2. 后端能力已实现

| Service 方法 | 并发控制 | 用途 |
|---|---|---|
| `claimCoupon()` | Redisson `coupon:claim:{tid}` + DB 乐观锁 `version` | 领券 |
| `lockForOrder()` | Redisson `coupon:lock:{cuid}` | 下单锁定 |
| `releaseForOrder()` | 无锁（幂等） | 释放单张 |
| `releaseByOrderNo()` | 无锁 | 批量释放 |
| `countAvailableByUserId()` | 无 | 可用数量 |

### 3. 缺口

- ❌ 无 C端 Controller（领取/我的券/可用券）
- ❌ 无 商家 Controller（模板 CRUD）
- ❌ `RemoteCouponService` 只暴露了 `releaseByOrderNo`
- ❌ 网关无 coupon 路由
- ❌ 下单锁定代码中标记了 `// ponytail: 待集成`
- ❌ `CouponTemplateServiceImpl.save()` 硬编码 `merchantId=1`

### 4. 不需要做的

- **不新增表/字段** — 现有 schema 满足
- **不修改 Mapper XML** — MyBatis-Plus 自动 SQL 足够
- **不修改现有 Service 实现** — `ICouponUserService` 已覆盖所有业务
- **不做定时过期处理** — 查询时实时过滤 `end_time < now`
- **不改 B端 CouponTemplateController** — 现有 B端 CRUD 保持不变

### 5. 订单集成路径

下单流程（已有 `couponId` 入参 + `couponIds` 存储字段 + 折扣金额字段）：
当前集成缺失（ZERO 硬编码 + 注释标明待实现），本次实现补齐 Feign 调用 + 折扣计算。

### 决策汇总

| 决策 | 选择 | 理由 |
|---|---|---|
| 过期券处理 | 实时查询过滤 `end_time < now` | 量小无需定时任务 |
| 领券并发 | 复用现有 Redisson + 乐观锁 | 已验证可用 |
| 下单锁定语义 | status=0→1（标记 USED + 设 orderNo） | 现有 `lockForOrder()` 实现，无需新端点 |
| 支付成功消耗 | 另建 `consume(couponUserId)` 端点更新 `used_time` | 区分锁定和核销 |
| C端 API 路径 | `/api/v1/coupon/*` | 遵循 AGENTS.md §3.6 |
