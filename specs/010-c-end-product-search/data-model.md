# Data Model: C端商品搜索功能

**Phase**: 1 — Design

无新增实体。复用已有 `Product` 实体及其字段。

## Product（已有）

搜索涉及字段：

| 字段 | 类型 | 用途 |
|---|---|---|
| `name` | varchar(200) | LIKE 匹配目标 |
| `subtitle` | varchar(500) | LIKE 匹配目标 |
| `category_id` | bigint | 分类过滤 |
| `min_price` | decimal(10,2) | 价格区间下限（用于排序参考） |
| `max_price` | decimal(10,2) | 价格区间上限 |
| `sales` | int | 销量排序 |
| `status` | char(1) | 只搜上架商品 |
| `create_time` | datetime | 最新上架排序 |
