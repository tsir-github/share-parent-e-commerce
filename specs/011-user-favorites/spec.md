# Feature Specification: 用户商品收藏

**Feature Branch**: `011-user-favorites`

**Created**: 2026-06-27

**Status**: Draft

## User Scenarios

### User Story 1 — 收藏/取消收藏商品（Priority: P1）🎯 MVP

用户在商品详情页点击心形图标收藏商品，再次点击取消收藏。操作即时生效。

**痛点解决**：
- 重复收藏 → 数据库唯一索引 `(user_id, product_id)` + 前端防抖
- 操作不生效 → 幂等设计：收藏已存在时返回成功不报错，取消不存在时也返回成功
- 并发问题 → 用户不可能同时打开两个页面操作同个商品的收藏，单品操作不需要分布式锁

**Independent Test**: 登录后打开商品详情，点击收藏图标，收藏数+1；再次点击，收藏数-1。

**Acceptance Scenarios**:

1. **Given** 用户已登录且未收藏该商品，**When** 点击收藏，**Then** 收藏成功，心形图标变为实心
2. **Given** 用户已收藏该商品，**When** 再次点击收藏，**Then** 取消收藏成功，心形图标变为空心
3. **Given** 用户未登录，**When** 点击收藏，**Then** 提示登录

---

### User Story 2 — 收藏列表（Priority: P1）

用户在个人中心进入收藏夹，分页查看已收藏的商品列表，包含商品图片、名称、价格、销量。

**痛点解决**：
- 下架商品也展示 → 收藏列表不清除已下架商品，但标注"已下架"，用户可手动移除
- 商品被软删除 → JOIN 时 `product.del_flag = 0` 过滤，收藏列表跳过已物理删除的商品
- 分页性能 → 一次查询 JOIN product 表，不分页查全部

**Independent Test**: 收藏3个商品后进入收藏夹，显示3条记录，含商品信息。

**Acceptance Scenarios**:

1. **Given** 用户有多个收藏，**When** 进入收藏夹，**Then** 分页展示收藏商品列表（图片、名称、价格、销量）
2. **Given** 用户收藏的商品已下架，**When** 查看收藏列表，**Then** 该商品仍展示但标注"已下架"
3. **Given** 用户收藏的商品已被商家删除，**When** 查看收藏列表，**Then** 该商品不显示

---

### User Story 3 — 商品详情页显示收藏状态（Priority: P2）

用户进入商品详情页时，接口返回当前用户是否已收藏该商品，前端据此展示心形实心/空心。

**痛点解决**：
- 避免额外请求 → 详情接口直接返回 `isFavorited` 字段，不用再单独调查询接口
- 大数据量查询 → 单商品查询收藏状态走索引，毫秒级

**Independent Test**: 收藏某商品后刷新详情页，心形图标为实心；取消后刷新，变为空心。

**Acceptance Scenarios**:

1. **Given** 用户已登录进入商品详情，**When** 页面加载，**Then** 接口返回 isFavorited=true/false

### Edge Cases

- 商品被商家删除后，收藏列表中自动跳过（JOIN过滤），用户不会看到"已删除"的条目
- 商品下架但未被删除，收藏列表展示并标"已下架"
- 同一用户快速重复点击收藏，数据库唯一索引防止重复插入，幂等处理
- 收藏列表翻页过程中，商品被取消收藏，刷新后列表正常

## Requirements

### Functional Requirements

- **FR-001**: 系统必须支持用户收藏/取消收藏商品（幂等操作）
- **FR-002**: 系统必须防止同一用户重复收藏同一商品（唯一索引）
- **FR-003**: 系统必须支持分页查询用户收藏列表，返回商品基本信息（名称、图片、价格、销量）
- **FR-004**: 收藏列表中已下架商品仍展示但需标注下架状态
- **FR-005**: 商品详情接口需返回当前用户是否已收藏
- **FR-006**: 所有收藏操作要求用户已登录

### Key Entities

- **UserFavorite**: 新建表 `share-user.user_favorite`，字段：`id`, `user_id`, `product_id`, `create_time`。唯一索引 `(user_id, product_id)`
- **Product（已有）**: JOIN 查询商品信息

## Database

```sql
CREATE TABLE `user_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `create_time` datetime DEFAULT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户商品收藏';
```

## Success Criteria

- **SC-001**: 收藏/取消操作在 1 秒内响应
- **SC-002**: 收藏列表分页查询在 2 秒内返回
- **SC-003**: 不会出现同一用户重复收藏同一商品的情况（唯一索引保障）

## Assumptions

- 用户只能收藏上架或下架商品，不能收藏已被删除的商品（JOIN过滤）
- 收藏列表排序按收藏时间倒序（最新的在前）
- 收藏功能面向 C端业主，商家和平台管理员不需要此功能
- 商品详情返回 isFavorited 字段需修改 ProductApiController.detail() 接口，新增该字段到返回体
