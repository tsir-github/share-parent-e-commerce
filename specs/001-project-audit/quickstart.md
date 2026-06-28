# 验证指南

## 环境准备

### 前置条件

| 组件 | 状态 | 检查方式 |
|---|---|---|
| Nacos | VM 192.168.10.129:8848 | 浏览器打开管理页面 |
| Redis Sentinel | VM 26379/26380/26381 | `redis-cli -p 26379 sentinel master mymaster` |
| RocketMQ | VM 192.168.10.129:9876 | `nc -zv 192.168.10.129 9876` |
| MySQL | VM 192.168.10.129:3306 | 各业务库可连接 |
| Seata + Sentinel | Windows 本机 | `middleware/start-middleware.bat` |
| nginx | VM | `sudo systemctl status nginx` |

### 启动顺序

```
1. VM: Nacos, Redis, RocketMQ, MySQL（长期运行，通常已启动）
2. Windows: start-middleware.bat（Seata + Sentinel）
3. IDEA: share-gateway → share-auth → share-system → 各业务模块
```

---

## 修复验证步骤（按优先级）

### P0: IOrderInfoService 接口修复

**前置依赖**：无

**验证步骤**：
1. 创建 `IOrderInfoService.java` 接口（10 个方法签名）
2. 修改 `OrderInfoServiceImpl` 加 `implements IOrderInfoService`
3. `mvn compile -pl share-modules/share-order -am` 检查编译无报错
4. 启动 share-order 模块，检查以下端点可用：
   - `GET /api/v1/order/list` — 用户订单列表
   - `POST /api/v1/order/create` — 创建订单
   - `GET /api/v1/order/detail/{id}` — 订单详情

**成功标准**：
- [ ] share-order 编译无错误
- [ ] OrderInfoApiController 注入 IOrderInfoService 不抛 UnsatisfiedDependencyException
- [ ] OrderTimeoutConsumer / PaymentSuccessConsumer 注入成功
- [ ] 现有 10 个 @Override 方法全部匹配接口签名

---

### P1: 订单取消库存回滚

**前置依赖**：IOrderInfoService 接口创建完成

**验证步骤**：
1. 在 RemoteGoodsService 增加 `releaseStock` 接口
2. 在 share-goods 增加释放库存的内部端点
3. 修改 `OrderInfoServiceImpl.cancelOrder()`：取消时调 Feign 释放库存和优惠券
4. 单元测试场景：
   - 创建订单 → 扣库存成功
   - 取消订单 → 库存回滚
   - 查询库存 = 原始值

**成功标准**：
- [ ] cancelOrder 调用 RemoteGoodsService.releaseStock()
- [ ] cancelOrder 调用 RemoteCouponService.releaseCoupon()
- [ ] 回滚操作在同一个 @Transactional 内
- [ ] 多次取消幂等（第二次调用不报错）

---

### P1: 领券并发控制

**前置依赖**：
- CouponTemplate 表加 `remain_count` 和 `version` 字段
- share-common-redis 在 share-coupon pom.xml 中已引入

**验证步骤**：
1. 执行 DB 迁移脚本加字段
2. 修改 `CouponUserServiceImpl.claimCoupon()`：替换 4 个 TODO
3. 并发测试：100 个线程同时领取 10 张优惠券，实际发放 <= 10 张

**成功标准**：
- [ ] remain_count 验证 + 乐观锁 update
- [ ] 有效期校验（startTime < now < endTime）
- [ ] 每人限领校验（count < limitPerUser）
- [ ] Redisson tryLock 成功时在锁内扣减
- [ ] 超发量为 0

---

### P1: 微信支付退款

**前置依赖**：
- share-payment/pom.xml 加 wechatpay-java 依赖
- PaymentStatus 加 REFUNDING=3

**验证步骤**：
1. Mock 模式：调用退款 → payment_status 从 PAID 变为 REFUNDED
2. 真实模式（如需）：使用微信沙箱环境测试
3. 退款回调端点 POST /api/v1/payment/refund/callback 可用

**成功标准**：
- [ ] PaymentInfoServiceImpl.refund() 正确调用退款 API
- [ ] payment_status 流转：PAID → REFUNDING → REFUNDED
- [ ] 单个订单不允许重复退款
- [ ] 退款异常时状态恢复为 PAID

---

### P1: SQL 注入修复

**前置依赖**：无

**验证步骤**：
1. 检查 `OrderInfoServiceImpl.getOrderCount()` 的 SQL 拼接
2. 确认使用参数化查询替代字符串拼接

**成功标准**：
- [ ] getOrderCount 不使用 `$` 拼接 SQL 参数
- [ ] SQL 注入测试：传入 `"1' OR '1'='1"` 不返回异常数据

---

## 回归测试清单

### 核心流程

```
注册/登录 → 浏览商品 → 加入购物车 → 创建订单
                                    → 支付(模拟)
                                    → 支付回调处理
                                    → 取消订单(超时/手动)
                                    → 分配配送员
                                    → 确认收货
                                    → 申请退款
```

### 各端点健康检查

| 模块 | 端点 | 预期 |
|---|---|---|
| share-auth | POST /auth/login | 200 + token |
| share-goods | GET /api/v1/product/list | 200 + 分页数据 |
| share-order | POST /api/v1/order/create | 200 + orderNo |
| share-order | GET /api/v1/order/detail/{id} | 200 + 订单详情 |
| share-payment | POST /api/v1/payment/refund | 200 + 退款成功 |
| share-coupon | POST /api/v1/coupon/claim | 200 + 领取成功 |
| share-user | GET /api/v1/user/info | 200 + 用户信息 |
| share-merchant | GET /api/v1/merchant/list | 200 + 商家列表 |

---

## 已知未覆盖场景

1. **ORDER_AUTO_CONFIRM_TOPIC** 无消费者 — 配送后不会自动确认收货
2. **UserAddress.java** 源码丢失 — 地址管理功能不可用
3. **product_image** 无后端代码 — 商品图片管理需手动 SQL
4. **购物车 Cart** 无 Service/Controller — 前端直接操作 DB（如果有的话）
