# 数据模型：平台数据报表

## 说明

本功能**不新建任何数据库表**，所有数据从现有业务表实时聚合计算。以下仅定义 VO 和查询结果结构。

## 已有表使用清单

| 表 | 数据库 | 用途 | 关键查询字段 |
|----|--------|------|-------------|
| `order_info` | share_order | 核心交易统计 | `create_time`, `pay_amount`, `refund_amount`, `status`, `pay_status`, `order_type` |
| `order_item` | share_order | 商品/商家排行 | `product_id`, `merchant_id`, `product_name`, `price`, `quantity`, `total_amount` |
| `payment_info` | share_payment | 支付统计 | `pay_way`, `amount`, `payment_status`, `create_time` |
| `user_info` | share_user | 用户统计 | `create_time`, `status`, `last_login_time` |
| `user_login_log` | share_user | 活跃用户 | `user_id`, `login_time` |
| `merchant_info` | share_merchant | 商家排行（仅名称） | `id`, `name` |

## 返回结构定义（VO）

### 交易总览

```json
{
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
```

### 订单趋势（按天）

```json
[
  {
    "date": "2026-06-22",
    "orderCount": 12,
    "revenue": 2300.00
  },
  {
    "date": "2026-06-23",
    "orderCount": 18,
    "revenue": 3500.00
  }
]
```

### 商品销售排行

```json
[
  {
    "rank": 1,
    "productId": 100,
    "productName": "iPhone 手机壳",
    "salesCount": 85,
    "revenue": 2975.00,
    "categoryName": "手机配件"
  }
]
```

### 商家销售排行

```json
[
  {
    "rank": 1,
    "merchantId": 1,
    "merchantName": "XX 旗舰店",
    "orderCount": 320,
    "revenue": 65000.00
  }
]
```

### 用户统计

```json
{
  "totalUsers": 5600,
  "todayNewUsers": 12,
  "weekNewUsers": 85,
  "monthNewUsers": 350,
  "activeUsers": 1200
}
```

### 新增用户趋势

```json
[
  {
    "date": "2026-06-22",
    "newUserCount": 8
  },
  {
    "date": "2026-06-23",
    "newUserCount": 15
  }
]
```

### 支付统计

```json
[
  {
    "payWay": 0,
    "payWayName": "微信支付",
    "orderCount": 420,
    "amount": 85600.00,
    "ratio": "100.00%"
  }
]
```

## 实体间关系（仅查询路径）

```
DataReport
├── OrderInfo (share_order.order_info)
│   ├── pay_amount → 交易额汇总
│   ├── refund_amount → 退款汇总
│   ├── status → 已完成/已取消过滤
│   └── create_time → 按日期分组趋势
│
├── OrderItem (share_order.order_item)
│   ├── product_id → 商品排行分组
│   ├── merchant_id → 商家排行分组
│   ├── product_name → 商品名快照
│   ├── quantity → 销量
│   └── total_amount → 交易额
│
├── PaymentInfo (share_payment.payment_info)
│   ├── pay_way → 支付方式分组
│   └── amount → 支付金额汇总
│
├── UserInfo (share_user.user_info)
│   └── create_time → 新增用户趋势
│
├── UserLoginLog (share_user.user_login_log)
│   └── login_time → 活跃用户统计
│
└── MerchantInfo (share_merchant.merchant_info)
    └── name → 商家名称（跨库关联）
```
