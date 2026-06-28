# API Contracts: C端商品搜索功能

## 新增搜索端点

### GET /api/v1/product/search

**Auth**: 公开（无需登录）

**Parameters**:

| 参数 | 位置 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | query | 否 | 搜索关键词（空则返回全部在售商品） |
| `categoryId` | query | 否 | 分类ID（可选，限定分类） |
| `minPrice` | query | 否 | 最低价（可选） |
| `maxPrice` | query | 否 | 最高价（可选） |
| `sortBy` | query | 否 | 排序方式：`default`(默认/最新)、`sales`(销量)、`price_asc`(价格升)、`price_desc`(价格降) |
| `pageNum` | query | 否 | 页码（默认1） |
| `pageSize` | query | 否 | 每页条数（默认20） |

**Response**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 42,
    "rows": [
      {
        "id": 1,
        "name": "蒙牛纯牛奶",
        "mainImage": "http://...",
        "minPrice": 49.90,
        "sales": 233
      }
    ],
    "code": 0,
    "msg": "查询成功"
  }
}
```
