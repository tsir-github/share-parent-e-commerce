# API 接口契约：平台数据报表

**角色**: 平台管理员 | **基路径**: 见各接口 | **返回类型**: `AjaxResult` | **鉴权**: `@RequiresPermissions`

**注意**: 全部为 GET 只读查询，无写操作，无需 `@Log` 注解

---

## 1. 交易总览

```
GET /data-report/overview
```

**权限**: `order:report:overview`

**归属**: share-order（网关 `/order/data-report/overview`）

**请求参数**（Query String）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `startDate` | String | 否 | 自定义范围起（yyyy-MM-dd） |
| `endDate` | String | 否 | 自定义范围止（yyyy-MM-dd） |

不传日期参数时返回今日/本周/本月/全部四个维度的汇总。

**响应**: `AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "todayOrderCount": 15,
    "todayRevenue": 2899.00,
    "todayAvgOrderAmount": 193.27,
    "todayRefundAmount": 0,
    "weekOrderCount": 98,
    "weekRevenue": 18500.00,
    "monthOrderCount": 450,
    "monthRevenue": 85600.00,
    "totalOrders": 12580,
    "totalRevenue": 2350000.00
  }
}
```

---

## 2. 订单趋势

```
GET /data-report/trend
```

**权限**: `order:report:trend`

**归属**: share-order（网关 `/order/data-report/trend`）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `startDate` | String | 否 | 开始日期（默认近 30 天） |
| `endDate` | String | 否 | 结束日期（默认今天） |
| `granularity` | String | 否 | 粒度：`day`（默认）/ `week` / `month` |

**响应**: `AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    { "date": "2026-06-22", "orderCount": 12, "revenue": 2300.00 },
    { "date": "2026-06-23", "orderCount": 18, "revenue": 3500.00 }
  ]
}
```

---

## 3. 商品销售排行

```
GET /data-report/product-ranking
```

**权限**: `order:report:productRanking`

**归属**: share-order（网关 `/order/data-report/product-ranking`）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `startDate` | String | 否 | 开始日期 |
| `endDate` | String | 否 | 结束日期 |
| `sortBy` | String | 否 | 排序：`salesCount`（默认，按销量）/ `revenue`（按交易额） |
| `topN` | Integer | 否 | 返回条数（默认 20，最大 100） |

**响应**: `AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "rank": 1,
      "productId": 100,
      "productName": "iPhone 手机壳",
      "salesCount": 85,
      "revenue": 2975.00,
      "categoryName": "手机配件"
    }
  ]
}
```

---

## 4. 商家销售排行

```
GET /data-report/merchant-ranking
```

**权限**: `order:report:merchantRanking`

**归属**: share-order（网关 `/order/data-report/merchant-ranking`）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `startDate` | String | 否 | 开始日期 |
| `endDate` | String | 否 | 结束日期 |
| `topN` | Integer | 否 | 返回条数（默认 20，最大 100） |

**响应**: `AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "rank": 1,
      "merchantId": 1,
      "merchantName": "XX 旗舰店",
      "orderCount": 320,
      "revenue": 65000.00
    }
  ]
}
```

---

## 5. 支付统计

```
GET /data-report/payment-stats
```

**权限**: `order:report:paymentStats`

**归属**: share-order（网关 `/order/data-report/payment-stats`）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `startDate` | String | 否 | 开始日期 |
| `endDate` | String | 否 | 结束日期 |

**响应**: `AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "payWay": 0,
      "payWayName": "微信支付",
      "orderCount": 420,
      "amount": 85600.00,
      "ratio": "100.00%"
    }
  ]
}
```

---

## 6. 用户统计

```
GET /userInfo/statistics
```

**权限**: `user:report:statistics`

**归属**: share-user（网关 `/user/userInfo/statistics`）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `startDate` | String | 否 | 新增用户趋势开始日期 |

**响应**: `AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "totalUsers": 5600,
    "todayNewUsers": 12,
    "weekNewUsers": 85,
    "monthNewUsers": 350,
    "activeUsers": 1200,
    "dailyTrend": [
      { "date": "2026-06-22", "newUserCount": 8 },
      { "date": "2026-06-23", "newUserCount": 15 }
    ]
  }
}
```
