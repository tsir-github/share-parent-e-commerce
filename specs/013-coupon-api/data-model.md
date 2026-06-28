# Data Model: 优惠券功能 API

## 现有实体

### CouponTemplate（已有）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long PK | 模板ID |
| name | String | 优惠券名称 |
| merchantId | Long | 商家ID（索引） |
| type | String(1) | 类型：0-满减 1-折扣 2-无门槛 |
| conditionAmt | BigDecimal | 满减条件金额（满减/折扣券用） |
| discountAmt | BigDecimal | 减免金额（满减券/无门槛券） |
| discountRate | BigDecimal(3,2) | 折扣率（折扣券，如0.85=85折） |
| totalCount | Integer | 发行总量 |
| remainCount | Integer | 剩余数量（-1=不限量） |
| limitPerUser | Integer | 每人限领（默认1） |
| startTime | Date | 有效期开始 |
| endTime | Date | 有效期结束 |
| status | String(1) | 0-禁用 1-启用 |
| version | Integer | 乐观锁 |
| delFlag | String(1) | 逻辑删除 |

**表**: `coupon_template` | **位置**: `share-api-coupon/domain/`

### CouponUser（已有）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long PK | 记录ID |
| userId | Long | 用户ID（索引） |
| templateId | Long | 模板ID |
| orderNo | String(32) | 使用订单号（下单锁定后写入） |
| status | String(1) | 0-未使用 1-已使用 2-已过期 |
| usedTime | Date | 使用时间（支付成功后写入） |
| delFlag | String(1) | 逻辑删除 |

**表**: `coupon_user` | **位置**: `share-api-coupon/domain/`

### OrderInfo（已有，含优惠券字段）

| 字段 | 类型 | 说明 |
|---|---|---|
| couponIds | String | 使用的优惠券ID，逗号分隔 |
| discountAmount | BigDecimal | 折扣金额 |
| amountDetail | String(JSON) | 含 discountAmount |

**位置**: `share-api-order/domain/`

## 新建 VO

### AvailableCouponVO

C端可领取优惠券列表 → `GET /api/v1/coupon/available`

| 字段 | 类型 | 来源 |
|---|---|---|
| id | Long | CouponTemplate.id |
| name | String | CouponTemplate.name |
| type | String(1) | CouponTemplate.type |
| conditionAmt | BigDecimal | CouponTemplate.conditionAmt |
| discountAmt | BigDecimal | CouponTemplate.discountAmt |
| discountRate | BigDecimal | CouponTemplate.discountRate |
| startTime | Date | CouponTemplate.startTime |
| endTime | Date | CouponTemplate.endTime |
| remainCount | Integer | CouponTemplate.remainCount |
| totalCount | Integer | CouponTemplate.totalCount |
| limitPerUser | Integer | CouponTemplate.limitPerUser |
| claimedCount | Integer | 当前用户已领数（子查询） |

### MyCouponVO

我的优惠券列表 → `GET /api/v1/coupon/my`

| 字段 | 类型 | 来源 |
|---|---|---|
| id | Long | CouponUser.id |
| templateId | Long | CouponUser.templateId |
| name | String | 模板名称（JOIN） |
| type | String(1) | 模板类型（JOIN） |
| conditionAmt | BigDecimal | 模板条件金额 |
| discountAmt | BigDecimal | 模板减免金额 |
| discountRate | BigDecimal | 模板折扣率 |
| status | String(1) | CouponUser.status |
| startTime | Date | 模板有效期开始 |
| endTime | Date | 模板有效期结束 |
| usedTime | Date | CouponUser.usedTime |
| createTime | Date | 领取时间 |

### UsableCouponVO

下单可用券 → `GET /api/v1/coupon/usable`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | CouponUser.id |
| templateId | Long | CouponUser.templateId |
| name | String | 模板名称 |
| type | String(1) | 模板类型 |
| conditionAmt | BigDecimal | 满减条件 |
| discountAmt | BigDecimal | 减免金额 |
| discountRate | BigDecimal | 折扣率 |
| endTime | Date | 有效期截止 |
| discount | BigDecimal | 实际可减免金额（计算后） |

## 状态流转

```
coupon_user.status:
  0 (UNUSED) ──┬──→ 1 (USED)     # lockForOrder (下单锁定)
               └──→ 1 (USED)     # consume (支付成功核销)
  [定时] 0 ──→ 2 (EXPIRED)      # 查询时实时判断 end_time < now

coupon_template.status:
  0 (DISABLED) ←→ 1 (ENABLED)    # 商家手动切换
```

## 领券校验流程

```
claimCoupon(userId, templateId):
  ├── template.status == 1?            → 模板已启用
  ├── now BETWEEN start_time AND end_time?  → 有效期
  ├── remain_count != 0?               → 有余量（-1=不限）
  ├── count(user_id) < limit_per_user? → 未超限领
  ├── Redisson.lock("coupon:claim:{tid}")
  │   └── 乐观锁扣减 remain_count      → 防超发
  └── INSERT coupon_user               → 领券记录
```

## 锁定/核销流程

```
lockForOrder(userId, couponUserId, orderNo):
  ├── coupon_user.status == 0?         → 未使用
  └── UPDATE status=1, order_no=orderNo

consume(couponUserId, orderNo):
  ├── coupon_user.status == 1?         → 已锁定
  ├── coupon_user.order_no == orderNo? → 订单匹配
  └── UPDATE used_time=now

releaseForOrder(couponUserId, orderNo):
  ├── coupon_user.order_no == orderNo? → 订单匹配
  └── UPDATE status=0, order_no=NULL, used_time=NULL
```
