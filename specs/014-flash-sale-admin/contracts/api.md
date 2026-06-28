# API 接口契约：秒杀活动管理后台

**角色**: 平台管理员 | **基路径**: `/seckillActivity` | **返回类型**: `AjaxResult`/`TableDataInfo`

**鉴权**: 所有接口需 `@RequiresPermissions` | **日志**: 写操作需 `@Log`

---

## 1. 分页查询活动列表

```
GET /seckillActivity/list
```

**权限**: `goods:seckill:list`

**请求参数**（Query String）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 否 | 活动名称（模糊搜索） |
| `status` | String | 否 | 状态筛选（0/1/2/3） |
| `merchantId` | Long | 否 | 商家ID筛选 |
| `pageNum` | Integer | 否 | 页码（默认 1） |
| `pageSize` | Integer | 否 | 每页条数（默认 10） |
| `params[beginTime]` | String | 否 | 开始时间范围（起） |
| `params[endTime]` | String | 否 | 开始时间范围（止） |

**响应**：`TableDataInfo`

```json
{
  "code": 0,
  "msg": "查询成功",
  "total": 50,
  "rows": [
    {
      "id": 1,
      "name": "国庆秒杀",
      "merchantId": 1,
      "productId": 100,
      "skuId": 200,
      "seckillPrice": 99.00,
      "stock": 100,
      "limitPerUser": 1,
      "sort": 0,
      "startTime": "2026-10-01 00:00:00",
      "endTime": "2026-10-03 23:59:59",
      "status": "0",
      "productName": "iPhone 手机壳",
      "skuSpecs": "[{\"key\":\"颜色\",\"value\":\"黑色\"}]",
      "mainImage": "https://...",
      "createTime": "2026-09-20 10:00:00"
    }
  ]
}
```

---

## 2. 查询活动详情

```
GET /seckillActivity/{id}
```

**权限**: `goods:seckill:query`

**响应**：`AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": { /* SeckillActivity 完整字段 */ }
}
```

---

## 3. 新增活动

```
POST /seckillActivity
```

**权限**: `goods:seckill:add` | **日志**: `@Log(title="秒杀活动", businessType=INSERT)`

**请求体**：

```json
{
  "name": "国庆秒杀",
  "productId": 100,
  "skuId": 200,
  "seckillPrice": 99.00,
  "stock": 100,
  "limitPerUser": 1,
  "sort": 0,
  "startTime": "2026-10-01 00:00:00",
  "endTime": "2026-10-03 23:59:59",
  "remark": "国庆促销活动"
}
```

**校验规则**：
- `name` 非空且不超过 100 字符
- `productId`/`skuId` 非空，且商品/SKU 存在且已上架
- `seckillPrice > 0` 且不得高于 `product_sku.price`
- `stock >= 0`，`limit_per_user >= 0`
- `endTime > startTime`
- 同一 SKU 在起止时间范围内不能有冲突的进行中/未开始活动

**响应**：`AjaxResult`（`toAjax(result)`）

```json
{ "code": 0, "msg": "操作成功" }
```

---

## 4. 修改活动

```
PUT /seckillActivity
```

**权限**: `goods:seckill:edit` | **日志**: `@Log(title="秒杀活动", businessType=UPDATE)`

**请求体**：同新增，包含 `id` 字段。

**限制**：
- 状态为"进行中"的活动不允许修改秒杀价和起止时间
- 进行中的活动只允许增加 `stock`，不允许减少

**响应**：`AjaxResult`（`toAjax(result)`）

---

## 5. 删除活动

```
DELETE /seckillActivity/{id}
```

**权限**: `goods:seckill:remove` | **日志**: `@Log(title="秒杀活动", businessType=DELETE)`

**限制**：
- 如果活动已有用户参与记录（后续 `seckill_record` 表），拒绝删除
- 逻辑删除（`del_flag=2`）

**响应**：`AjaxResult`（`toAjax(result)`）

---

## 6. 启用/禁用活动

```
PUT /seckillActivity/status
```

**权限**: `goods:seckill:edit` | **日志**: `@Log(title="秒杀活动", businessType=UPDATE)`

**请求体**：

```json
{
  "id": 1,
  "status": "1"
}
```

**状态流转规则**：
- `0`(未开始) → `1`(进行中) 或 `3`(已禁用)
- `1`(进行中) → `3`(已禁用)
- `3`(已禁用) → `1`(进行中)（需校验当前时间在活动时间范围内）
- `2`(已结束) 不可变更

**响应**：`AjaxResult`

```json
{ "code": 0, "msg": "操作成功" }
```

---

## 7. 获取商品列表（辅助接口）

```
GET /seckillActivity/product/list
```

**用途**: 创建活动时选择商品的下拉列表

**权限**: `goods:seckill:add`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 否 | 商品名称搜索 |
| `merchantId` | Long | 否 | 商家筛选 |

**响应**：`AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 100,
      "name": "iPhone 手机壳",
      "mainImage": "https://...",
      "minPrice": 29.00,
      "maxPrice": 39.00,
      "merchantId": 1,
      "skus": [
        {
          "id": 200,
          "specs": "[{\"key\":\"颜色\",\"value\":\"黑色\"}]",
          "price": 39.00,
          "stock": 500
        }
      ]
    }
  ]
}
```

---

## 8. 获取活动统计

```
GET /seckillActivity/stats/{id}
```

**权限**: `goods:seckill:list`

**响应**：`AjaxResult`

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "totalSales": 85,
    "totalOrders": 80,
    "paidOrders": 60,
    "paymentRate": "75.00%",
    "totalRevenue": 5940.00
  }
}
```

> 注：统计数据依赖后续 `seckill_record` 表和订单数据，当前可先返回占位结构，完整实现需等 C 端秒杀下单功能上线。
