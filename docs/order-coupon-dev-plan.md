# 订单 + 优惠券 开发方案

## 设计思路：从底层往上层，不把鸡蛋放一个篮子里

开发顺序不按「角色」切，也不按「服务」切——按**依赖层级**切。

### 原则

```
依赖关系决定开发顺序：
  Controller → Service → Mapper → SQL
                    ↑
              不依赖别的服务 → 先做
              有依赖的 → 依赖做好再做
```

所以先做的是**最底层、无依赖的模块**，后做的是**组装层**。

---

## 开发路线图

### Step 0：基础设施（已经做完）

| 事项 | 状态 |
|---|---|
| DB 表结构 + 注释更新 | ✅ |
| OrderInfo.java（所有字段） | ✅ |
| OrderLog.java（新结构） | ✅ |
| 锁/幂等/并发设计方案 | ✅ doc |

### Step 1：share-payment 支付模块（独立，无业务依赖）— ✅ 已完成

支付模块不依赖订单模块，只需要：
- `order_no`（字符串，调用方传过来）
- `user_id`（谁付的钱）
- `amount`（多少钱）

所以它可以独立开发，不需要等订单模块。

```

支付模块内部依赖：
  Controller（回调/支付接口）
      ↓
  PaymentInfoServiceImpl
      ↓
  WxPayUtil（签名验签）  +  WxPayConfig（微信配置）
      ↓
  PaymentInfoMapper（CRUD payment_info 表）
```

**具体做：**
1. `WxPayConfig` — 微信支付 v3 配置读取（含 `mock-mode` 开关）✅
2. `WxPayUtil` — 签名生成、回调验签 ✅
3. `createPayment()` — 调微信统一下单 / mock，写 payment_info ✅
4. `handlePayCallback(request)` — 验签→幂等→写流水 ✅
5. `mockPaySuccess()` — mock 模式模拟支付回调 ✅
6. `refund(orderNo, amount)` — 调微信退款接口 / mock ✅
7. Controller：C端支付 + 微信/mock回调 + 退款 + 管理列表 + Feign查询 ✅

**交付物：** 支付模块可独立测试（mock 模式无需微信真实商户号，使用 `POST /api/v1/payment/mock/callback?orderNo=xxx` 模拟支付成功）

---

### Step 2：share-order 订单状态引擎（核心业务逻辑）— 🟡 状态机已完成

这是整个系统最复杂的部分——**不是写 Controller，而是写状态机**。

```  
订单引擎内部依赖：
  ┌──────────────────────────────────────┐
  │  OrderInfoServiceImpl                │
  │                                      │
  │  createOrder() ── 事务①               │
  │    ├── 扣库存（乐观锁）               │
  │    ├── 锁优惠券                       │
  │    ├── INSERT order_info              │
  │    ├── INSERT order_bill              │
  │    ├── INSERT order_item              │
  │    ├── INSERT order_log               │
  │    └── 发 MQ 延迟消息（30min超时）     │
  │                                      │
  │  handlePayCallback() ── 事务②         │
  │    ├── 幂等校验                       │
  │    ├── 乐观锁更新 status='0'→'1'      │
  │    ├── 标记优惠券已使用               │
  │    └── INSERT order_log               │
  │                                      │
  │  cancelOrder() ── 事务③               │
  │    ├── 状态校验                       │
  │    ├── 乐观锁更新 status→'4'          │
  │    ├── 归还库存                       │
  │    ├── 归还优惠券                     │
  │    ├── 已支付的 → 调用 refund         │
  │    └── INSERT order_log               │
  │                                      │
  │  shipOrder() ── 事务④                 │
  │  confirmOrder() ── 事务⑤              │
  │  acceptDelivery() ── 分布式锁+事务⑥   │
  │  markDelivered() ── 事务⑦             │
  │                                      │
  │  autoCancelTimeoutOrders() ── 定时任务 │
  │  autoConfirmDelivered() ── 定时任务   │
  │  reconcile() ── 每日对账              │
  └──────────────────────────────────────┘
```

**已实现：**
| 组件 | 状态 | 说明 |
|---|---|---|
| `IOrderStatusService` 接口 | ✅ | validateTransition / transition / getTargetStatusByOperate |
| `OrderStatusServiceImpl` 状态机 | ✅ | 8种操作类型 + 6个状态 + 乐观锁 + OrderLog流水 |
| `OrderInfoServiceImpl` 接入状态机 | ✅ | processPaySuccess / cancelOrder / deliverOrder / confirmReceive 全部走状态引擎 |
| `OrderInfoMapper.updateOrderStatus` | ✅ | 乐观锁更新 `WHERE id=? AND version=?` |
| `OrderInfoMapper.xml` | ✅ | MyBatis XML |
| `OrderInfoApiController` | ✅ | 构造注入 + 新增 `/cancel` `/deliver` `/confirm` `/inner/paySuccess` |

**待实现：**
| 组件 | 说明 |
|---|---|
| `createOrder()` 完整链路 | 扣库存 + 锁优惠券 + 写 order_info/order_bill/order_item |
| `acceptDelivery()` 配送员接单 | 分布式锁防重复接单 |
| 超时自动取消定时任务 | MQ 延迟消息 / @Scheduled |
| 自动确认收货 | 配送送达7天后自动完成 |

**为什么先写 Service 层而不是 Controller？**
- Controller 只有一行 `orderService.xxx()`，没有业务逻辑
- 所有锁、事务、幂等都在 Service 里
- Service 写完可以用单元测试验证，不用启动 HTTP 服务

**依赖的外部服务（都有 Feign 接口）：**
- `share-api-goods.RemoteGoodsService` — 扣库存
- `share-api-coupon.RemoteCouponService` — 锁券/用券/还券
- `share-payment` — 支付回调后自己写 MQ 通知订单

---

### Step 3：消息队列集成（支付→订单的桥梁）

```
支付回调成功
    ↓
PaymentInfoServiceImpl
    ↓ 发 RocketMQ 消息（topic: order-pay-success）
    ↓
OrderPaySuccessConsumer（在 share-order 模块）
    ↓ 幂等 + 乐观锁
orderService.handlePayCallback(orderNo)
```

**额外消息：**
- `order-timeout-cancel`（30min 延迟）— 待支付超时取消
- `order-auto-confirm`（7天延迟）— 送达后自动确认

---

### Step 4：角色 Controller（薄层收口）

```
CustomerOrderController         @RequiresLogin
  ├── POST /api/v1/order/create    → orderService.createOrder()
  ├── POST /api/v1/order/cancel    → orderService.cancelOrder()
  ├── POST /api/v1/order/confirm   → orderService.confirmOrder()
  ├── POST /api/v1/order/refund    → orderService.applyRefund()
  └── GET  /api/v1/order/list      → orderService.listByUserId()

SupplierOrderController           @RequiresPermissions
  ├── GET  /api/v1/supplier/order/list   → orderService.listBySupplier()
  └── POST /api/v1/supplier/order/ship   → orderService.shipOrder()

DeliveryOrderController           @RequiresLogin
  ├── GET  /api/v1/delivery/order/list    → orderService.listByDelivery()
  ├── POST /api/v1/delivery/order/accept  → orderService.acceptDelivery()
  └── POST /api/v1/delivery/order/deliver → orderService.markDelivered()

AdminOrderController              @RequiresPermissions
  ├── GET  /api/v1/admin/order/list      → orderService.listAll()
  └── POST /api/v1/admin/order/force     → orderService.forceUpdateStatus()
```

每个 Controller 方法不超过 5 行，只做：
1. 参数提取
2. 调用 Service
3. 返回 R<T>

---

### Step 5：定时任务 + 对账（兜底）

| Cron | 任务 | 做什么 |
|---|---|---|
| `0/30 * * * * ?` | 超时取消扫描 | 查 status='0' 且超过 30min → 取消 |
| `0 0 2 * * ?` | 每日对账 | payment_info vs order_info 不一致 → 补单 |
| `0 0 3 * * ?` | 自动确认收货 | delivery_status='2' 超过 7天 → 完成 |

---

## 实际执行顺序（进度追踪）

```
第1步：share-payment Service 层                             ✅ 已完成
  ├── WxPayConfig + WxPayUtil                               ✅
  ├── createPayment()                                        ✅（mock + 真实双模式）
  ├── handlePayCallback()                                    ✅
  └── refund()                                               ✅

第2步：share-order 状态引擎（最核心）                         🟡 状态机已完成
  ├── IOrderStatusService 接口 + 实现                        ✅
  ├── processPaySuccess() + 幂等 + 乐观锁                    ✅
  ├── cancelOrder() + 状态校验                               ✅
  ├── deliverOrder() / confirmReceive()                      ✅
  ├── createOrder() + 扣库存 + 锁券 + 事务                   ⏳
  ├── acceptDelivery() + 分布式锁                            ⏳
  └── 超时 + 对账定时任务                                    ⏳

第3步：MQ 集成                                                   ⏳
  ├── 支付成功发消息
  ├── 订单消费者
  └── 延迟消息（超时取消/自动确认）

第4步：角色 Controller                                           ⏳
  ├── CustomerOrderController
  ├── SupplierOrderController
  ├── DeliveryOrderController
  └── AdminOrderController
```

---

## 为什么这样安排？

**不是按「角色」切，而是按「依赖层级」切：**

```
❌ 按角色切（会出现的问题）：
    第1周：客户控制器（依赖 Service、依赖支付、依赖库存）
    第2周：供货商控制器（依赖 Service、依赖配送）
    → 每写一个 Controller 都要先把底层 Service 堆完
    → 前两周看不到可工作的东西

✅ 按依赖层级切（我们的方案）：
    第1步：支付 Service（独立可测）
    第2步：订单 Service（核心引擎，单元测试覆盖）
    第3步：MQ 集成（支付→订单桥接）
    第4步：4 个 Controller（一周全搞定）
    → 每一步都有可验证的产出
    → 底层先做好，上层只是组装
```
