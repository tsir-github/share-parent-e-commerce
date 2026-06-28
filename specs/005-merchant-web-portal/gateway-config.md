# Nacos 网关配置变更 (T012-T013)

登录 Nacos 控制台 `192.168.10.129:8848`，命名空间 `a746e297-417e-4aec-bfb1-e42df33fbe93`，编辑 `share-gateway-dev.yml`：

## 1. 白名单添加（T012）

在 `security.ignore.whites` 列表中添加：

```yaml
- /auth/merchant/**
```

## 2. 路由添加（T013）

在 `spring.cloud.gateway.routes` 列表中添加：

```yaml
# 商家商品接口 → share-goods（controller path: /api/v1/merchant/product|sku）
- id: share-merchant-products
  uri: lb://share-goods
  predicates:
    - Path=/api/v1/merchant/product/**,/api/v1/merchant/sku/**
  filters:
    - StripPrefix=0

# 商家订单+售后 → share-order（controller path: /api/v1/merchant/order|after-sale）
- id: share-merchant-orders
  uri: lb://share-order
  predicates:
    - Path=/api/v1/merchant/order/**,/api/v1/merchant/after-sale/**
  filters:
    - StripPrefix=0

# 商家资料+控制台 → share-merchant（controller path: /api/v1/merchant/profile|dashboard）
- id: share-merchant-profile
  uri: lb://share-merchant
  predicates:
    - Path=/api/v1/merchant/profile/**,/api/v1/merchant/dashboard/**
  filters:
    - StripPrefix=0
```

> ⚠️ **注意**：白名单 `/auth/merchant/**` 必须在 `security.ignore.whites` 中，否则商家登录请求会被 AuthFilter 拦截（因为还没拿到 token）。
