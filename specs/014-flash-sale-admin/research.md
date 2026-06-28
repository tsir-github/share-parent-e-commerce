# 秒杀活动管理后台 — 技术调研

## 模块归属

- **Decision**: share-goods（端口 9210）
- **Rationale**: `seckill_activity` 表已存在于 `share-goods` 数据库，商品/商品 SKU 实体也在 `share-api-goods` 中。新增秒杀活动功能放入现有模块，不新建模块。
- **Alternatives**: 新建 `share-seckill` 模块 → 过于重量级，且需额外配置 Nacos/网关/数据库。当前活动量级（数百级别）无需独立模块。

## 角色与鉴权

- **Decision**: 平台管理员（`@RequiresPermissions` + `BaseController` + `AjaxResult`）
- **Rationale**: 用户明确要求"管理后台"，且项目规范规定无前缀路径 + BaseController 为管理员模式
- **Alternatives**: 商家端管理（`/api/v1/merchant/`）→ 秒杀活动通常由平台统一运营而非单个商家，不适合

## 数据库表现状

- **Decision**: 改造现有 `seckill_activity` 表而非新建表
- **Rationale**: 表已存在且命名合理，仅需补充字段
- **需新增字段**:
  - `limit_per_user` int — 每人限购数量（防黄牛）
  - `merchant_id` bigint — 关联商家（从 product 表推导）
  - `version` int — 乐观锁（高并发扣库存用）
  - `sort` int — 排序权重
  - 审计字段: `create_by`, `update_by`, `update_time`, `del_flag`, `remark`
- **需新增索引**: `idx_product_id`, `idx_sku_id`, `idx_start_time`, `idx_status`, `idx_merchant_id`
- **状态设计**: status 字段规范化为项目通用的 char(1)，`0`=未开始 `1`=进行中 `2`=已结束 `3`=已禁用

## 活动状态自动流转

- **Decision**: 状态由 `status` 字段 + 系统时间共同决定
- **Rationale**: 查询时 `WHERE status = '1' AND start_time <= NOW() AND end_time >= NOW()` 判断是否为进行中。无需定时任务扫描。
- **Alternatives**: 定时任务每分钟扫描更新状态 → 增加复杂度，没有必要

## 秒杀扣库存方案

- **Decision**: 管理后台不涉及扣库存逻辑（C 端下单时才扣减）
- **Rationale**: 本功能仅开发管理后台，不包含 C 端秒杀抢购。库存扣减使用 `product_sku.version` 乐观锁 + Redis Lua 脚本，留待后续 C 端功能实现。
- **注意**: 已经预留 `version` 字段到 `seckill_activity` 表

## 已存在的相关代码资产

| 资产 | 状态 | 使用方式 |
|------|------|----------|
| `seckill_activity` 表 | ✅ 已存在，需改造 | 新增字段+索引 |
| `product`/`product_sku` 实体 | ✅ 已存在 | 活动关联商品/SKU |
| `product_sku.version` 乐观锁 | ✅ 已存在 | 后续秒杀扣库存 |
| `order_info.order_type` | ✅ 已支持 `'1'`=秒杀 | 后续秒杀订单标识 |
| `OrderStatus.ORDER_TYPE_FLASH` | ✅ 已定义 | 直接复用常量 |
| 秒杀 Entity/Mapper/Service | ❌ 不存在 | 需新建 |

## 缓存策略

- **Decision**: 活动列表不缓存（管理后台数据量小），但启用/禁用操作需清除 C 端缓存
- **Rationale**: 管理员操作频率远低于 C 端查询。启用/禁用时要保证 C 端尽快感知，后续实现 C 端 api 时需在 Redis 中缓存活动数据，并在 admin 操作时失效对应缓存。
