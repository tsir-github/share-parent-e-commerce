# 平台数据报表 — 技术调研

## 模块归属

- **Decision**: 主逻辑在 share-order（9211），用户统计在 share-user（9209）
- **Rationale**: 5/6 的报表需要订单数据（order_info/order_item），share-order 可直接跨库查询 share_payment 和 share_merchant。用户统计则复用 share-user 已存在的 UserInfoController。
- **Alternatives**: 新建 share-report 模块 → 过于重量级，跨库查询已够用。统一放 share-system → share-system 目前只有 RuoYi 框架表，需要额外配置数据源。

## 角色与鉴权

- **Decision**: 平台管理员（`@RequiresPermissions` + `BaseController` + `AjaxResult`）
- **Rationale**: 用户要求"数据报表"，平台管理员查看全平台数据。遵循项目规范：后端管理接口使用无前缀路径 + BaseController。
- **Alternatives**: 商家端报表（`/api/v1/merchant/`）→ 商家已有 MerchantDashboard 控制台，但本次需求是平台级报表，不冲突。

## 数据库现状

### 已有统计相关代码

| 资产 | 状态 | 说明 |
|------|------|------|
| `OrderInfoAdminController.getDashboardStats()` | ✅ 已实现 | 返回 totalOrders/todayOrders/pendingOrders/totalRevenue/todayRevenue |
| `UserInfoController.getDashboardStats()` | ✅ 已实现 | 返回 totalUsers/todayNewUsers |
| `MerchantInfoController.getDashboardStats()` | ✅ 已实现 | 返回 totalMerchants/pendingAudit/activeMerchants |
| `MerchantDashboardMapper.xml` | ✅ 已实现 | 跨库查询 share_order.order_info（COALESCE + 子查询） |
| `OrderInfoMapper.getOrderCountByDate()` | ✅ 已实现 | GROUP BY DATE(create_time) 按天聚合 |
| `OrderReviewMapper` | ✅ 已实现 | AVG(rating) + COUNT(*) 评价统计 |
| `OrderStatistics` | 🟡 空存根 | `// TODO: 补充统计字段` |
| `OrderStatisticsQuery` | 🟡 部分实现 | 有日期范围 + 区域字段，未使用 |
| `OrderStatisticsMapper` | 🟡 空存根 | 有接口方法无 XML/Service/Controller |
| `OrderRegionStatisticsVo` | 🟡 空存根 | `// TODO: 补充区域统计字段` |

### 可用数据表

| 数据库 | 表 | 关键统计字段 |
|--------|-----|-------------|
| `share_order.order_info` | 订单主表 | `pay_amount`, `status`, `order_type`, `create_time`, `pay_time` |
| `share_order.order_item` | 订单明细 | `product_id`, `merchant_id`, `price`, `quantity`, `total_amount`, `product_name` |
| `share_order.order_log` | 操作流水 | `operate_type`, `after_status` |
| `share_payment.payment_info` | 支付记录 | `pay_way`, `amount`, `payment_status`, `create_time` |
| `share_user.user_info` | 用户 | `status`, `create_time`, `last_login_time` |
| `share_user.user_login_log` | 登录日志 | `user_id`, `login_time` |
| `share_merchant.merchant_info` | 商家 | `name`, `status`, `create_time` |
| `share_goods.product` | 商品 | `name`, `category_id`, `status` |
| `share_goods.category` | 分类 | `name`, `parent_id` |

### 跨库查询可行性

已确认可行（MerchantDashboardMapper.xml 已有生产案例）：
```sql
SELECT COUNT(*) FROM share_order.order_info WHERE supplier_id = #{merchantId}
```

所有业务数据库在同一 MySQL 实例（192.168.10.129:3306），可直接使用 `db.table` 语法跨库查询。

## 关键设计决策

### 1. 实时聚合 vs 预计算

- **Decision**: 实时聚合（v1），暂不建统计表
- **Rationale**: 当前数据量级（日订单数百级）实时 COUNT/SUM 可在秒级完成，无需定时预计算增加复杂度
- **When to change**: 日订单超 1 万或报表查询超 5 秒时，在 share-job 加定时任务写统计结果表

### 2. 返回格式

- **Decision**: 统一使用 `Map<String, Object>` 或简单 VO，避免过度设计
- **Rationale**: 报表数据字段随需求变化频繁，强类型 DTO 维护成本高。使用 Map 灵活且已在 MerchantDashboard 验证可行。
- **Note**: 商品/商家排行等列表数据使用 `List<Map<String, Object>>` 返回

### 3. 客单价计算

- **Decision**: `客单价 = 交易总额 / 已完成订单数`
- **Note**: 分母为已支付已完成订单（status='3'），分子为 pay_amount 总和

### 4. 退款处理

- **Decision**: 交易额 = SUM(pay_amount) - SUM(refund_amount)，单独返回退款金额
- **Note**: 符合 Edge Cases 中"退款订单不应计入交易额统计，退款金额单独展示"的要求

### 5. 商品排行中的退款扣除

- **Decision**: 商品排行交易额 = 该商品已支付订单的交易额 - 已退款金额
- **Note**: 需要 JOIN order_info 获取 pay_status 和 refund_amount

### 6. 商家排行中的跨库查询

- **Decision**: share-order 模块跨库查询 `share_merchant.merchant_info` 获取商家名称
- **Note**: 使用 `share_merchant.merchant_info.name` 语法，已在 MerchantDashboard 验证可行

### 7. 活跃用户定义

- **Decision**: "活跃用户"定义为**最近 30 天有登录记录的用户**
- **Note**: 从 `share_user.user_login_log` 统计，`COUNT(DISTINCT user_id) WHERE login_time > NOW() - INTERVAL 30 DAY`

### 8. 支付方式

- **Decision**: 当前仅微信支付（pay_way=0），统计接口预留多方式扩展
- **Note**: `payment_info.pay_way` 为 tinyint，已在 payment_info 表中存在

## 已存在的相关代码资产

| 资产 | 状态 | 使用方式 |
|------|------|----------|
| `OrderInfoAdminController` | ✅ 已存在 | 复用其 dashboard 模式，新增 DataReportController |
| `UserInfoController.getDashboardStats()` | ✅ 已存在 | 扩展用户统计（新增/活跃趋势） |
| `MerchantDashboardMapper` | ✅ 已存在 | 跨库查询模式参考 |
| `OrderInfoMapper.getOrderCountByDate()` | ✅ 已存在 | GROUP BY 聚合模式参考 |
| `OrderStatistics` / `OrderStatisticsQuery` / `OrderStatisticsMapper` | 🟡 存根 | 本次不直接使用（报表使用新 DataReportMapper + Map 返回） |

## 缓存策略

- **Decision**: v1 不做缓存，每次实时查询
- **Rationale**: 管理后台访问频率低，数据实时性要求高（管理员希望看到截至当前的最新数据）
- **When to change**: 当报表访问频率升高或查询变慢时，可对固定时间范围（如"今日总览"）加 Redis 缓存，TTL 60 秒
