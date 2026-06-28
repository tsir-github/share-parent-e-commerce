# Feature Specification: 商品评价系统

**Feature Branch**: `012-product-review`

**Created**: 2026-06-28

**Status**: Draft

## User Scenarios

### User Story 1 — 提交评价（Priority: P1）🎯 MVP

订单完成后，用户可对每个订单项提交评价（评分 + 文字 + 图片）。支持先上传图片再提交评价，也可一次性完成。

**痛点解决**：
- 评价后不能重复提交 → 每个订单项仅限一条评价（order_item_id 唯一索引）
- 未完成的订单不能评价 → 仅 status=3（已完成）+ delivery_status=3（已确认）可评价
- 图片上传失败不影响评价提交 → 评价文本和评分先提交，图片可单独上传

**Acceptance Scenarios**:

1. **Given** 用户已确认收货（订单已完成），**When** 提交评价（评分+文字），**Then** 评价成功，订单项标记已评价
2. **Given** 用户已提交评价，**When** 再次提交相同订单项，**Then** 返回重复提交错误
3. **Given** 订单未完成，**When** 尝试评价，**Then** 提示订单状态不允许

---

### User Story 2 — 待评价列表（Priority: P1）

用户进入"待评价"页面，查看所有可评价的订单项（已完成订单中未评价的项）。

**痛点解决**：
- 一个订单多个商品 → 按订单项展示，每个项独立评价
- 已评价和未评价分开 → query 过滤：order_review 中不存在的 order_item_id 即为待评价

**Acceptance Scenarios**:

1. **Given** 用户有2个已完成订单共3个未评价项，**When** 点击待评价，**Then** 展示3条待评价项（含商品信息）
2. **Given** 用户评价其中1个后，**When** 刷新待评价列表，**Then** 剩余2个待评价项

---

### User Story 3 — 商品评价列表（Priority: P1）

商品详情页展示所有用户评价，含评分、内容、图片、用户信息、时间。支持分页。

**Acceptance Scenarios**:

1. **Given** 商品有10条评价，**When** 进入商品详情页，**Then** 展示评价列表（每条含评分、内容、图片、用户昵称、时间）
2. **Given** 评价超过分页大小，**When** 翻页，**Then** 加载下一页评价

---

### User Story 4 — 商品详情显示评分（Priority: P2）

商品列表和详情页展示平均评分和评价总数。评分数据从 order_review 表实时计算。

**Acceptance Scenarios**:

1. **Given** 商品有3条评价（5分、4分、4分），**When** 查看商品详情，**Then** 显示平均评分 4.3 和评价数 3

---

### User Story 5 — 商家回复评价（Priority: P3）📌 后续迭代

商家可回复用户评价，回复显示在原评价下方。

> 当前不实现，标记为 P3。

---

## Requirements

### Functional Requirements

- **FR-001**: 系统必须支持用户对已完成订单的每个订单项提交评价（评分 1-5 + 文字内容）
- **FR-002**: 每个订单项仅限一条评价（order_item_id 唯一约束）
- **FR-003**: 评价提交时需校验订单状态（仅已完成+已确认）
- **FR-004**: 系统必须支持评价晒图（复用现有 review_image 表，关联 review_id）
- **FR-005**: 系统必须支持分页查看待评价的订单项列表
- **FR-006**: 系统必须支持分页查看某商品的所有评价（公开，无需登录）
- **FR-007**: 商品列表/详情必须展示平均评分和评价总数（实时计算）

### Key Entities

- **OrderReview（新建）**: `share-order.order_review`，一条记录对应一个订单项的评价
- **ReviewImage（已有）**: 新增 `review_id` 字段关联 OrderReview，保留 `order_item_id` 向后兼容

## Database

```sql
-- 评价主表
CREATE TABLE `order_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_item_id` bigint NOT NULL COMMENT '订单项ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `rating` tinyint NOT NULL COMMENT '评分（1-5）',
  `content` varchar(2000) DEFAULT '' COMMENT '评价内容',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1隐藏）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0正常 2删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_order_item` (`order_item_id`),
  KEY `idx_product` (`order_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单评价';

-- 已有 review_image 表新增 review_id 列
ALTER TABLE `review_image` ADD COLUMN `review_id` bigint DEFAULT NULL COMMENT '评价ID' AFTER `order_item_id`;
ALTER TABLE `review_image` ADD INDEX `idx_review_id` (`review_id`);
```

## Success Criteria

- **SC-001**: 评价提交在 1 秒内响应
- **SC-002**: 商品评价列表分页在 1 秒内返回
- **SC-003**: 不允许同一订单项重复评价（唯一索引保障）
- **SC-004**: 不允许未完成订单提交评价（状态机校验）

## Assumptions

- 评价系统针对 C 端业主，商家和平台管理员不直接评价
- 平均评分实时从 order_review 表 AVG 计算，不反写 product 表（数据量小，性能可接受）
- 评价晒图复用已有的 `review_image` 表和文件上传逻辑
- 评价提交后不可修改（如需修改走售后流程或后续迭代支持）
- 商品详情中 `avgRating` 和 `reviewCount` 作为 `@TableField(exist = false)` 字段返回，不存储

## API 设计

### C端评价接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/order/review` | 提交评价 |
| GET | `/api/v1/order/review/item/{orderItemId}` | 查指定订单项评价 |
| GET | `/api/v1/order/review/pending` | 待评价列表 |
| POST | `/api/v1/order/review/{orderItemId}/images` | 上传晒图（已有） |

### 商品公开接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/product/{productId}/reviews` | 商品评价列表（公开） |
