# Quickstart: 优惠券功能 API

## 前置条件

- 已启动 `share-coupon` 模块（端口 9214）
- 已启动 `share-gateway`（端口 8080），已配置 coupon 路由
- MySQL `share-coupon` 库有初始数据

## 验证场景

### 1. 商家创建优惠券模板

```bash
curl -X POST http://localhost:9214/api/v1/merchant/coupon/template \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {merchant_token}" \
  -d '{
    "name": "满100减10券",
    "type": "0",
    "conditionAmt": 100.00,
    "discountAmt": 10.00,
    "totalCount": 100,
    "limitPerUser": 1,
    "startTime": "2026-06-28 00:00:00",
    "endTime": "2026-07-28 23:59:59"
  }'
```

期望：返回 `R.ok(data)`，data 为模板ID

### 2. 商家查看模板列表

```bash
curl http://localhost:9214/api/v1/merchant/coupon/template/list \
  -H "Authorization: Bearer {merchant_token}"
```

期望：返回 `R.ok(data)`，data 为分页模板列表（仅当前商家的）

### 3. C端业主查看可领取优惠券

```bash
curl http://localhost:9214/api/v1/coupon/available \
  -H "Authorization: Bearer {user_token}"
```

期望：返回 `R.ok(data)`，只包含 status=1 且在有效期内的模板

### 4. C端业主领取优惠券

```bash
curl -X POST http://localhost:9214/api/v1/coupon/claim/1 \
  -H "Authorization: Bearer {user_token}"
```

期望：返回 `R.ok()`

### 5. C端业主查看我的优惠券

```bash
curl "http://localhost:9214/api/v1/coupon/my?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer {user_token}"
```

期望：返回 `R.ok(data)`，含模板名称/类型/金额/有效期

### 6. C端业主查询下单可用券

```bash
curl "http://localhost:9214/api/v1/coupon/usable?amount=150" \
  -H "Authorization: Bearer {user_token}"
```

期望：返回 `R.ok(data)`，只返回满减条件 amount>=condition_amt 的券

### 7. Feign 接口测试（内部调用）

```bash
curl -X POST http://localhost:9214/inner/coupon/lock \
  -H "Content-Type: application/json" \
  -H "From: internal" \
  -d '{"userId": 1, "couponUserId": 1, "orderNo": "ORDER20260001"}'
```

期望：返回 `R.ok()`，优惠券锁定成功

## 编译验证

```bash
# 编译 coupon 模块
mvn compile -pl share-modules/share-coupon -am -q
# 编译 API 模块
mvn compile -pl share-api/share-api-coupon -am -q
```

期望：无输出（编译成功）

## 验收清单

- [ ] 商家可创建/查看/编辑/删除自己的模板
- [ ] C端可查看可领取列表
- [ ] C端可领取优惠券（含限领/余量校验）
- [ ] C端可查看我的优惠券（分页+状态筛选）
- [ ] C端可查询下单可用券（按金额过滤）
- [ ] Feign lock/release/countAvailable 可调用
- [ ] 网关 `/coupon/**` 路由正常转发
