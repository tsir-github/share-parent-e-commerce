# Phase 0 技术调研结果

**Branch**: 001-project-audit | **Date**: 2026-06-23 | **Spec**: specs/001-project-audit/spec.md

## 调研范围

5 个并行 research agents，覆盖：
1. IOrderInfoService 接口缺失分析与 @Override 映射
2. Order -> Goods -> Coupon Feign 调用流程
3. 无 Service/Controller 的 DB 表与实体 Gaps
4. 微信支付 v3 退款 API Java 实现
5. Redisson 分布式锁领券并发控制

## 1. IOrderInfoService 接口缺失 (P0)

### 发现

IOrderInfoService.java 在源代码中完全不存在（git 历史中从未存在过），但被 3 个生产类引用，编译会炸。

引用缺失接口的文件：
- OrderInfoApiController -- private final IOrderInfoService orderInfoService
- OrderTimeoutConsumer -- 通过 IOrderInfoService 调用 cancelOrder()
- PaymentSuccessConsumer -- 通过 IOrderInfoService 调用 processPaySuccess()

OrderInfoServiceImpl 当前声明：
  public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
缺少 implements IOrderInfoService，但有 10 个无效的 @Override 方法。

### 需要创建的接口（10 个方法）

public interface IOrderInfoService extends IService<OrderInfo> {
    OrderInfo getByOrderNo(String orderNo);
    void processPaySuccess(String orderNo, String transactionId);
    void cancelOrder(String orderNo, String closeType, String reason);
    void deliverOrder(String orderNo, Long deliveryBy, String deliveryName, String deliveryPhone);
    void confirmReceive(String orderNo);
    String createOrder(CreateOrderDTO dto);
    void endOrder(EndOrderVo endOrderVo);
    List<OrderInfo> selectOrderListByUserId(Long userId);
    OrderInfo selectOrderInfoById(Long id);
    Map<String, Object> getOrderCount(String sql);
}

### 以此类推的其他缺失接口

| 模块 | ServiceImpl | 缺少接口 | 影响 |
|---|---|---|---|
| share-goods | CategoryServiceImpl | ICategoryService | 控制器直接注入 Impl |
| share-goods | ProductServiceImpl | IProductService | 控制器直接注入 Impl |
| share-goods | ProductSkuServiceImpl | IProductSkuService | 控制器直接注入 Impl |
| share-merchant | MerchantInfoServiceImpl | IMerchantInfoService | 仅模块内使用 |
| share-user | UserInfoServiceImpl | IUserInfoService | 控制器+Feign 注入 Impl |
| share-payment | PaymentInfoServiceImpl | IPaymentInfoService | 无调用者问题 |

### MQ 消费者依赖

| 消费者 | Topic | 调用方法 |
|---|---|---|
| OrderTimeoutConsumer | ORDER_TIMEOUT_CANCEL_TOPIC | cancelOrder(orderNo, CLOSE_TYPE_TIMEOUT, msg) |
| PaymentSuccessConsumer | PAYMENT_SUCCESS_TOPIC | processPaySuccess(orderNo, transactionId) |

决策：P0 只恢复 IOrderInfoService（编译崩溃），其他模块归类为 P2。

---

## 2. Order -> Goods -> Coupon Feign 调用流程

### 现有 Feign 接口

| Feign 接口 | 所在模块 | 方法数 | 用途 |
|---|---|---|---|
| RemoteGoodsService | share-api-goods | 3 | 商品查询、库存扣减 |
| RemoteOrderInfoService | share-api-order | 3 | 订单查询 |
| RemoteCouponService | share-api-coupon | 待确认 | 优惠券查询 |
| RemoteUserService | share-api-user | 待确认 | 用户信息查询 |
| RemoteMerchantService | share-api-merchant | 待确认 | 商家信息查询 |

### 订单创建中的 Feign 链路

1. share-user: RemoteUserService.getUserById() -> 查用户
2. share-goods: RemoteGoodsService.getSkuById() -> 查 SKU
3. share-goods: RemoteGoodsService.deductStock() -> 扣库存
4. share-coupon: RemoteCouponService.lockCoupon() -> 锁定优惠券
5. 本地插入 OrderInfo + OrderItem

### 现有问题

1. cancelOrder 不同步回滚库存和优惠券
2. Feign fallback 部分返回 R.fail() 但上游未校验
3. MQ 异步解耦不完整

决策：P1 修复 cancelOrder 的回滚逻辑。

---

## 3. 无 Service/Controller 的 DB 表与实体 Gaps

### GAP 1: DB 表无任何 Java 源码

| DB 表 | 所属模块 | 缺 Service | 缺 Controller | 优先级 |
|---|---|---|---|---|
| product_image | share-goods | YES | YES | P2 |
| seckill_activity | share-goods | YES | YES | P2 |
| region | share-goods | YES | YES | P3 |
| stock_log | share-goods | YES | YES | P3 |

### GAP 2: 仅剩 .class 文件（源码丢失）

- UserAddress.java / UserLoginLog.java (share-api-user/domain/)
- RemoteMerchantService.java / RemoteMerchantFallbackFactory.java (share-api-merchant/)
- MerchantUser.java (share-api-merchant/domain/) + MerchantUserMapper.java
- RemotePaymentService.java / RemotePaymentFallbackFactory.java (share-api-payment/)

### GAP 3: Entity 存在但缺 Service/Controller/Mapper

| Entity | 位置 | 缺 Service | 缺 Controller | 缺 Mapper |
|---|---|---|---|---|
| Cart.java | share-api-order/domain/ | YES | YES | YES |

决策：GAP 2 先检查 git stash / local history 能否恢复，否则重写 (P2)。

---

## 4. 微信支付 v3 退款 API

### 依赖现状

父 pom 已声明 wechatpay-java (SDK v0.2.11)，但 share-payment/pom.xml 未引入。
现有 WxPayUtil 使用 JDK HttpClient + 手动签名。

### 方案 A: wechatpay-java SDK（推荐）

1. share-payment/pom.xml 加 wechatpay-java 依赖
2. 注册 RefundService Bean (RSAAutoCertificateConfig)
3. 调用 refundService.create(request)
4. PaymentInfo 加 REFUNDING=3 状态

### 方案 B: 延续 JDK HttpClient

现有 WxPayUtil 加 createRefund() 方法，模式与 createOrder() 一致。

### 退款 API 要点

| 项目 | 内容 |
|---|---|
| 端点 | POST /v3/refund/domestic/refunds |
| 幂等键 | out_refund_no（统一前缀 TK + 原 orderNo） |
| 异步结果 | 退款回调 event_type=REFUND.SUCCESS |
| 证书 | 与支付同一套 |

### 幂等策略

out_refund_no = "TK" + orderNo
if (payment_status == REFUNDING || payment_status == REFUNDED) -> 跳过

决策：推荐方案 A，若 SDK 太重则退而使用方案 B。

---

## 5. Redisson 锁 -- 优惠券领取并发控制

### 锁模式

- tryLock(wait, lease, unit) -- 短操作显式 leaseTime，禁用 watchdog
- lock() -- 长操作 watchdog 续期，不适合短操作

### 推荐实现

lockKey = "coupon:claim:" + templateId
RLock lock = redissonClient.getLock(lockKey);

if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
    throw new ServiceException("操作太频繁，请稍后再试");
}
try {
    // 锁内：验证 + 扣减
    // 1. 模板存在 && 领取时间内
    // 2. 库存 (remainCount > 0)
    // 3. 每人限领 (perUserLimit)
    // 4. 乐观锁扣库存 (version)
    // 5. 插入用户优惠券记录
} finally {
    if (lock.isHeldByCurrentThread()) lock.unlock();
}

### 锁粒度

| 粒度 | 优点 | 缺点 |
|---|---|---|
| templateId | 模板级串行，绝对不超发 | 不同用户领同一种券排队 |
| templateId:userId | 不同用户并发 | 需配合乐观锁防超发 |

推荐先用 per-user 粒度，等流量上来再收紧。

### 与项目现有模式一致

AGENTS.md 已定义：tryLock(5, 30, SECONDS)，仅需调整 key 和 timeout。

---

## 决策汇总

| # | 问题 | 方案 | 优先级 |
|---|---|---|---|
| R01 | IOrderInfoService 接口缺失 | 创建接口（10 方法），ServiceImpl 加 implements | P0 |
| R02 | 无接口的其他模块 | 暂不创建，归类为代码规范修复 | P2 |
| R03 | cancelOrder 不回滚 | 补 Feign 调用释放逻辑 | P1 |
| R04 | 8 个源码文件丢失 | 先检查 git stash，否则重写 | P2 |
| R05 | Cart 无 Service/Controller/Mapper | 新建购物车 CRUD | P2 |
| R06 | product_image/seckill_activity 无代码 | 按需实现 | P2 |
| R07 | 微信退款只更新 DB | SDK 或 WxPayUtil 补真实 API 调用 | P1 |
| R08 | 领券无并发控制 | Redisson tryLock + 乐观锁 version | P1 |
| R09 | refund callback 端点缺失 | 新建回调端点 | P1 |
| R10 | PaymentStatus 缺 REFUNDING | 加 REFUNDING=3 | P1 |
