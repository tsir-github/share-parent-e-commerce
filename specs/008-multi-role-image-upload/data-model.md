# Data Model: 多角色图片上传能力

**Phase**: 1 — Design & Contracts
**Date**: 2026-06-27

## 实体总览

| 实体 | 数据库 | 表名 | 状态 |
|---|---|---|---|
| UserInfo.avatarUrl | share-user | user_info | 已有字段 |
| MerchantInfo.logo | share-merchant | merchant_info | 已有字段 |
| Product.mainImage | share-goods | product | 已有字段 |
| ProductImage | share-goods | product_image | 表已存在 |
| ReviewImage | share-order | review_image | 需新建 |

---

## 1. UserInfo — avatarUrl 字段

**位置**: `share-user.user_info.avatar_url`

**类型**: `varchar(200)`

**当前行为**: wxLogin 时硬编码为阿里云 OSS 默认头像

**变更**: 提供上传接口后，用户可上传新头像，`avatar_url` 更新为 MinIO URL

## 2. MerchantInfo — logo 字段

**位置**: `share-merchant.merchant_info.logo`

**类型**: `varchar(255)`

**当前行为**: 字段存在但无上传入口

**变更**: 商家后台店铺资料页提供Logo上传，更新 logo 字段

## 3. Product — mainImage 字段

**位置**: `share-goods.product.main_image`

**类型**: `varchar(255)`

**变更**: 商品新增/编辑时支持上传主图

## 4. ProductImage（已存在）

**位置**: `share-goods.product_image`

| 字段 | 类型 | 约束 |
|---|---|---|
| id | bigint | PK, auto_increment |
| product_id | bigint | NOT NULL, FK → product.id |
| image_url | varchar(500) | NOT NULL |
| sort_order | int | DEFAULT 0 |
| create_time | datetime | |

**说明**: 关联商品的多张图片（主图之外的详情图、轮播图）。上传后直接插入该表。

## 5. ReviewImage（需新建）

**位置**: `share-order.review_image`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK, auto_increment | 主键 |
| order_item_id | bigint | NOT NULL, FK → order_item.id | 关联订单项 |
| image_url | varchar(500) | NOT NULL | 图片访问URL |
| sort_order | int | DEFAULT 0 | 排序序号 |

**建表 SQL**:

```sql
CREATE TABLE `review_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_item_id` bigint NOT NULL COMMENT '关联订单项ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_order_item_id` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价晒图';
```
