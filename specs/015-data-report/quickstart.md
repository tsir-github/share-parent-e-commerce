# 验证指南：平台数据报表

## 前置条件

1. share-order 和 share-user 服务已启动
2. share-gateway 网关已启动
3. Nacos 中相关模块配置正确
4. 数据库中有至少一笔已完成订单（用于验证数据）

## 验证场景

### 1. 交易总览

```bash
# 今日/本周/本月/全部汇总
GET http://localhost:8080/order/data-report/overview
Authorization: Bearer <admin-token>

# 指定日期范围
GET http://localhost:8080/order/data-report/overview?startDate=2026-06-01&endDate=2026-06-28
```

**预期**：返回 JSON 包含 todayOrderCount / monthRevenue 等字段。无订单时数值为 0。

### 2. 订单趋势

```bash
# 默认近 30 天
GET http://localhost:8080/order/data-report/trend

# 按周粒度
GET http://localhost:8080/order/data-report/trend?startDate=2026-01-01&endDate=2026-06-28&granularity=week
```

**预期**：返回按日期排序的数组。日期连续（无订单日返回 0）。

### 3. 商品销售排行

```bash
# 按销量排行
GET http://localhost:8080/order/data-report/product-ranking

# 按交易额排行 + Top 5
GET http://localhost:8080/order/data-report/product-ranking?sortBy=revenue&topN=5
```

**预期**：返回含 rank/productName/salesCount/revenue 的数组，按所选排序方式降序。

### 4. 商家销售排行

```bash
# 默认 Top 20
GET http://localhost:8080/order/data-report/merchant-ranking

# 指定范围 + Top 10
GET http://localhost:8080/order/data-report/merchant-ranking?startDate=2026-06-01&endDate=2026-06-28&topN=10
```

**预期**：返回含 merchantName/orderCount/revenue 的数组，无订单商家不在排行中。

### 5. 支付统计

```bash
GET http://localhost:8080/order/data-report/payment-stats
```

**预期**：返回各支付方式的订单数/金额/占比。

### 6. 用户统计

```bash
GET http://localhost:8080/user/userInfo/statistics
```

**预期**：返回 totalUsers/todayNewUsers/activeUsers/dailyTrend。

## 无权限验证

```bash
# 不带 token
GET http://localhost:8080/order/data-report/overview
```

**预期**：返回 401 未授权。

## 空数据验证

在开发/测试数据库无订单时：
- 所有数值接口应返回 0 而非 null
- 排行接口应返回空数组 `[]` 而非 null
- 趋势接口应返回空数组 `[]` 而非 null

## 边界条件验证

| 场景 | 操作 | 预期 |
|------|------|------|
| 跨年查询 | `startDate=2025-01-01&endDate=2026-06-28` | 正常返回，响应 < 5 秒 |
| 超大 topN | `topN=9999` | 返回全部数据，或限制最大 100 |
| 无效日期 | `startDate=invalid` | 返回参数校验错误 |
| 日期颠倒 | `startDate=2026-06-28&endDate=2026-01-01` | 返回友好错误提示 |
