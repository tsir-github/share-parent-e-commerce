# Interface Contracts: 商家端 Web 后台

## 认证接口（share-auth）

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/auth/merchant/login` | 商家登录 | 无 |
| POST | `/auth/merchant/logout` | 商家登出 | Token |
| GET | `/auth/merchant/getInfo` | 获取当前商家信息 | Token |

**登录请求**：
```json
{ "username": "shop001", "password": "xxx" }
```

**登录响应**：
```json
{ "code": 200, "msg": "ok", "data": { "access_token": "xxx", "merchant": { "id": 1, "name": "XX小店" } } }
```

## 商品管理接口（share-goods）

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/merchant/product/list` | 商家商品列表 | @RequiresLogin + merchantId |
| POST | `/api/v1/merchant/product/add` | 新增商品 | 同上 |
| PUT | `/api/v1/merchant/product/edit` | 编辑商品 | 同上 |
| GET | `/api/v1/merchant/product/{id}` | 商品详情 | 同上 |
| PUT | `/api/v1/merchant/product/status` | 上架/下架 | 同上 |
| GET | `/api/v1/merchant/sku/product/{productId}` | 商品SKU列表 | 同上 |
| POST | `/api/v1/merchant/sku/add` | 新增SKU | 同上 |
| PUT | `/api/v1/merchant/sku/edit` | 编辑SKU | 同上 |
| DELETE | `/api/v1/merchant/sku/{id}` | 删除SKU | 同上 |

**查询参数**：`pageNum` `pageSize` `name` `status`

## 订单管理接口（share-order）

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/merchant/order/list` | 商家订单列表 | @RequiresLogin + merchantId |
| GET | `/api/v1/merchant/order/{id}` | 订单详情 | 同上 |
| POST | `/api/v1/merchant/order/deliver` | 发货 | 同上 |

**发货请求**：
```json
{ "orderNo": "xxx", "deliveryName": "张三", "deliveryPhone": "138xxxx" }
```

## 售后管理接口（share-order）

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/merchant/after-sale/list` | 售后申请列表 | @RequiresLogin + merchantId |
| PUT | `/api/v1/merchant/after-sale/approve` | 同意退款 | 同上 |
| PUT | `/api/v1/merchant/after-sale/reject` | 拒绝退款 | 同上 |

**审核请求**：
```json
{ "id": 1, "remark": "同意退款" }
```

## 店铺设置接口（share-merchant）

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/merchant/profile` | 店铺信息 | @RequiresLogin |
| PUT | `/api/v1/merchant/profile` | 更新店铺信息 | 同上 |
| PUT | `/api/v1/merchant/password` | 修改密码 | 同上 |
| GET | `/api/v1/merchant/dashboard` | 数据概览 | @RequiresLogin |

## 网关路由新增

```yaml
# share-gateway-dev.yml
- id: share-merchant-api
  uri: lb://share-goods
  predicates:
    - Path=/api/v1/merchant/product/**,/api/v1/merchant/sku/**
  filters:
    - StripPrefix=1

- id: share-merchant-order-api
  uri: lb://share-order
  predicates:
    - Path=/api/v1/merchant/order/**,/api/v1/merchant/after-sale/**
  filters:
    - StripPrefix=1

- id: share-merchant-profile-api
  uri: lb://share-merchant
  predicates:
    - Path=/api/v1/merchant/profile/**,/api/v1/merchant/dashboard/**
  filters:
    - StripPrefix=1
```
