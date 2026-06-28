# Research: C端商品搜索功能

**Phase**: 0 — Research

## Decisions

### 1. 搜索匹配方式

**Decision**: MySQL LIKE %keyword%，匹配 name + subtitle
**Rationale**: 商品量 1000 以内，LIKE 足够。不用 MySQL FULLTEXT 或 ES，避免引入复杂性
**Alternatives**: Elasticsearch（过度设计），MySQL FULLTEXT（效果提升有限，维护成本高）

### 2. 排序实现

**Decision**: MyBatis XML 动态 ORDER BY，根据参数拼接
**Rationale**: 简单直接，PageHelper 分页可用

### 3. 分类+搜索组合

**Decision**: 同一 SQL 中拼接 `category_id = ?` 条件，由前端传参控制
**Rationale**: 不需要拆分接口，一个 search 方法处理所有组合
