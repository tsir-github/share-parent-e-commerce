# Implementation Tasks

**Branch**: 001-project-audit | **Generated**: 2026-06-23 | **Plan**: specs/001-project-audit/plan.md

## Dependency Graph

```
Phase 0 — 编译修复 (P0)
  T1: 创建 IOrderInfoService 接口
  T2: OrderInfoServiceImpl 加 implements
  └── 验证: 编译通过

Phase 1 — 核心数据安全 (P1, 并行)
  T3: SQL 注入修复 — getOrderCount
  T4: PaymentStatus 加 REFUNDING=3
  T5: CouponTemplate 加 remain_count + version 字段
  T6: 领券并发控制 — claimCoupon (依赖 T5)
  └── 验证: 并发测试

Phase 2 — 退款闭环 (P1)
  T7: 微信退款真实 API 调用 (依赖 T4)
  T8: refund callback 端点
  └── 验证: 退款全链路

Phase 3 — 订单一致性 (P1)
  T9: cancelOrder 库存回滚
  T10: cancelOrder 优惠券释放
  └── 验证: 取消订单后库存/优惠券恢复

Phase 4 — 源码恢复 (P2)
  T11: 恢复 8 个丢失的源码文件
  T12: Cart Service/Controller/Mapper
  └── 验证: 编译通过

Phase 5 — 按需补全 (P2)
  T13: product_image 后端代码
  T14: seckill_activity 后端代码
  └── 验证: 按需
```

---

## Phase 0 — 编译修复 (P0)

### T1: 创建 IOrderInfoService 接口

| 属性 | 值 |
|---|---|
| 优先级 | P0 |
| 依赖 | 无 |
| 文件 | `share-order/service/IOrderInfoService.java` |
| 预估 | 15 min |

**操作**：
1. 在 `share-order/service/` 创建 `IOrderInfoService.java`
2. 定义 10 个方法签名（从 OrderInfoServiceImpl 的 @Override 方法提取）：

```java
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
```

**验证**：`mvn compile -pl share-modules/share-order -am` 无报错

### T2: OrderInfoServiceImpl 加 implements

| 属性 | 值 |
|---|---|
| 优先级 | P0 |
| 依赖 | T1 |
| 文件 | `share-order/service/impl/OrderInfoServiceImpl.java` |
| 预估 | 5 min |

**操作**：
1. 类声明改为 `public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService`
2. 确认 10 个 @Override 已存在，无需额外修改

**验证**：同 T1 编译验证

---

## Phase 1 — 核心数据安全 (P1)

### T3: SQL 注入修复 — getOrderCount

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | 无 |
| 文件 | `OrderInfoServiceImpl.java` 中 `getOrderCount()` 方法；`OrderInfoMapper.java`；`OrderInfoMapper.xml` |
| 预估 | 30 min |

**操作**：
1. 检查 `OrderInfoServiceImpl.getOrderCount(String sql)` 的当前实现 — 推测是直接拼接 SQL 字符串
2. 改为参数化查询：使用 MyBatis-Plus `QueryWrapper` 或 XML 中的 `${}` 改为 `#{}`
3. 如果使用 Mapper XML 中 `SELECT COUNT(*) FROM order_info WHERE ${sql}`，改为构造安全的 `LambdaQueryWrapper`
4. 确保 `OrderSqlVo` 中的 SQL 参数通过预编译传入

**验证**：
- [ ] 传入 `"1' OR '1'='1"` 不返回异常数据
- [ ] 正常日期范围查询结果正确

### T4: PaymentStatus 加 REFUNDING=3

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | 无 |
| 文件 | `PaymentStatus.java`，`PaymentInfoServiceImpl.java`，`PaymentInfo.java` 注释 |
| 预估 | 15 min |

**操作**：
1. `PaymentStatus.java` 新增 `public static final Integer REFUNDING = 3;`
2. `PaymentInfo.paymentStatus` 的 Swagger 注释改为 `支付状态：0-未支付 1-已支付 2-已退款 3-退款中`
3. 检查 `PaymentInfoServiceImpl` 中所有 `paymentStatus` 相关的判断，确保兼容新值

### T5: CouponTemplate 加 remain_count + version 字段

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | 无 |
| 文件 | `CouponTemplate.java`、`coupon_template` 表 DDL |
| 预估 | 30 min |

**操作**：
1. `CouponTemplate.java` 新增字段：
```java
/** 剩余数量（-1表示不限） */
@Schema(description = "剩余数量")
private Integer remainCount;

/** 乐观锁版本号 */
@Schema(description = "乐观锁版本号")
@Version
private Integer version;
```
2. 生成 DB 迁移 SQL：
```sql
ALTER TABLE coupon_template
  ADD COLUMN `remain_count` int DEFAULT 0 COMMENT '剩余数量',
  ADD COLUMN `version` int DEFAULT 0 COMMENT '乐观锁版本号';
```
3. 已有数据的 `remain_count` 初始化为 `total_count`（`total_count = -1` 不限量则 `remain_count = -1`）

### T6: 领券并发控制 — claimCoupon

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | T5 (remain_count + version) |
| 文件 | `CouponUserServiceImpl.java` |
| 预估 | 45 min |

**操作**：
1. 替换 `claimCoupon()` 中的 4 个 TODO：

```java
String lockKey = "coupon:claim:" + templateId;
RLock lock = redissonClient.getLock(lockKey);

if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
    throw new ServiceException("操作太频繁，请稍后再试");
}
try {
    // 锁内验证
    // 1. 有效期校验（startTime < now < endTime）
    // 2. 发行量校验（remainCount > 0 或 == -1）
    // 3. 每人限领校验（count < limitPerUser）
    // 4. 乐观锁扣减 remainCount（version）
    // 5. 插入 coupon_user 记录
} finally {
    if (lock.isHeldByCurrentThread()) lock.unlock();
}
```

**注意**：
- 使用 `share-common-redis` 的 RedissonClient（确保 `pom.xml` 已引入）
- 扣库存用 `LambdaUpdateWrapper.set()` + `version` 乐观锁，禁止 `updateById()`
- `lockForOrder()` 中的 TODO 也同步修复

---

## Phase 2 — 退款闭环 (P1)

### T7: 微信退款真实 API 调用

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | T4 (REFUNDING 状态) |
| 文件 | `PaymentInfoServiceImpl.java`、`share-payment/pom.xml`、`WxPayAutoConfig.java`(新建) |
| 预估 | 60 min |

**操作**：
1. `share-payment/pom.xml` 加 `wechatpay-java` 依赖
2. 新建 `WxPayAutoConfig.java` 注册 `RefundService` Bean
3. 修改 `PaymentInfoServiceImpl.refund()`：替换 TODO 为真实 API 调用
4. 幂等逻辑：`out_refund_no = "TK" + orderNo`，状态为 REFUNDING/REFUNDED 则跳过

### T8: refund callback 端点

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | T7 |
| 文件 | `PaymentInfoController.java`、`PaymentInfoServiceImpl.java` |
| 预估 | 30 min |

**操作**：
1. Controller 新增 `POST /api/v1/payment/refund/callback`
2. Service 新增 `handleRefundCallback(request)` 方法
3. 解析微信异步通知，按 `event_type` 处理状态：
   - `REFUND.SUCCESS` → paymentStatus = REFUNDED
   - `REFUND.ABNORMAL` → paymentStatus = PAID（恢复）
   - `REFUND.CLOSED` → paymentStatus = PAID（恢复）

---

## Phase 3 — 订单一致性 (P1)

### T9: cancelOrder 库存回滚

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | T2 |
| 文件 | `OrderInfoServiceImpl.java`、`RemoteGoodsService.java`、`InnerSkuController.java`、`ProductSkuService`/`ProductSkuMapper.xml` |
| 预估 | 45 min |

**操作**：
1. `RemoteGoodsService` 新增 `releaseStock(List<StockDeductDTO> items, String source)` 方法
2. share-goods 内部端点 `POST /inner/goods/stock/release`
3. `ProductSkuServiceImpl` 实现库存释放逻辑（`stock = stock + quantity`）
4. `cancelOrder()` 中通过 Feign 调用释放库存

### T10: cancelOrder 优惠券释放

| 属性 | 值 |
|---|---|
| 优先级 | P1 |
| 依赖 | T2 |
| 文件 | `OrderInfoServiceImpl.java`、`RemoteCouponService.java`(新建)、`ICouponUserService.java` |
| 预估 | 30 min |

**操作**：
1. 确认 `ICouponUserService.releaseForOrder()` 已存在
2. `cancelOrder()` 中：如果 `order.couponIds` 不为空，调用 `releaseForOrder()`

---

## Phase 4 — 源码恢复 (P2)

### T11: 恢复 8 个丢失的源码文件

| 属性 | 值 |
|---|---|
| 优先级 | P2 |
| 依赖 | 无 |
| 预估 | 60 min |

**操作**：
1. 优先尝试恢复：
   - `git stash list` 检查是否有 stash
   - `git log --all --diff-filter=D --name-only` 查找被删除的文件
   - IDE Local History
2. 如果无法恢复，从 `.class` 文件反编译（使用 `javap -c` 或 CFR 反编译器）重建

**待恢复文件清单**：
- `share-api-user/domain/UserAddress.java`
- `share-api-user/domain/UserLoginLog.java`
- `share-api-merchant/api/RemoteMerchantService.java`
- `share-api-merchant/factory/RemoteMerchantFallbackFactory.java`
- `share-api-merchant/domain/MerchantUser.java`
- `share-modules/share-merchant/mapper/MerchantUserMapper.java`
- `share-api-payment/api/RemotePaymentService.java`
- `share-api-payment/factory/RemotePaymentFallbackFactory.java`

### T12: Cart Service/Controller/Mapper

| 属性 | 值 |
|---|---|
| 优先级 | P2 |
| 依赖 | 无 |
| 文件 | `CartMapper.java`(新建)、`CartServiceImpl.java`(新建)、`CartController.java`(新建) |
| 预估 | 30 min |

**操作**：
1. 创建 `CartMapper`（MyBatis-Plus BaseMapper）
2. 创建 `ICartService` + `CartServiceImpl`（基本 CRUD）
3. 创建 `CartController`（`@RequestMapping("/cart")`，RESTful 端点）
4. Entity 已在 `share-api-order/domain/Cart.java` 存在

---

## Phase 5 — 按需补全 (P2)

### T13: product_image 后端

| 属性 | 值 |
|---|---|
| 优先级 | P2 |
| 依赖 | 无 |
| 预估 | 30 min |

**操作**：
1. 在 `share-api-goods` 创建 `ProductImage` Entity
2. 在 `share-goods` 创建 Mapper/Service/Controller

### T14: seckill_activity 后端

| 属性 | 值 |
|---|---|
| 优先级 | P2 |
| 依赖 | 无 |
| 预估 | 30 min |

**操作**：
1. 在 `share-api-goods` 创建 `SeckillActivity` Entity
2. 在 `share-goods` 创建 Mapper/Service/Controller
