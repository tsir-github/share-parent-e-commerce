# 内部接口契约

## 1. Feign 接口（同步调用）

### RemoteGoodsService (share-api-goods)

```java
@FeignClient(contextId = "remoteGoodsService",
             value = ServiceNameConstants.GOODS_SERVICE,
             fallbackFactory = RemoteGoodsFallbackFactory.class)
public interface RemoteGoodsService {

    /** 根据 SKU ID 查询商品信息 */
    @GetMapping("/inner/goods/sku/{skuId}")
    R<ProductSku> getSkuById(@PathVariable("skuId") Long skuId,
                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 批量扣减库存（订单创建时调用） */
    @PostMapping("/inner/goods/stock/deduct")
    R<Boolean> deductStock(@RequestBody List<StockDeductDTO> items,
                           @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    // TODO: P1 — 增加库存释放接口（cancelOrder 时回滚）
    // @PostMapping("/inner/goods/stock/release")
    // R<Boolean> releaseStock(@RequestBody List<StockDeductDTO> items,
    //                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
```

### RemoteOrderInfoService (share-api-order)

```java
@FeignClient(contextId = "remoteOrderInfoService",
             value = ServiceNameConstants.ORDER_SERVICE,
             fallbackFactory = RemoteOrderInfoFallbackFactory.class)
public interface RemoteOrderInfoService {

    /** 查询用户未完成订单数 */
    @GetMapping("/inner/order/unfinish/count")
    R<Integer> getNoFinishOrder(@RequestParam("userId") Long userId,
                                 @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 根据订单号查询订单 */
    @GetMapping("/inner/order/getByOrderNo")
    R<OrderInfo> getByOrderNo(@RequestParam("orderNo") String orderNo,
                              @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 统计订单数量（含 SQL 注入风险，P0 修复项） */
    @PostMapping("/inner/order/count")
    R<Map<String, Object>> getOrderCount(@RequestBody OrderSqlVo orderSqlVo,
                                         @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
```

### RemoteCouponService 待重建

> 当前 share-api-coupon 仅包含 domain 实体，无 Feign 接口。
> 需要在 P1 时创建，至少需要：

```java
// share-api-coupon/src/main/java/com/share/coupon/api/RemoteCouponService.java
@FeignClient(contextId = "remoteCouponService",
             value = ServiceNameConstants.COUPON_SERVICE,
             fallbackFactory = RemoteCouponFallbackFactory.class)
public interface RemoteCouponService {

    /** 领取优惠券 */
    @PostMapping("/inner/coupon/claim")
    R<Void> claimCoupon(@RequestBody ClaimCouponDTO dto,
                        @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 下单锁定优惠券 */
    @PostMapping("/inner/coupon/lock")
    R<Void> lockCoupon(@RequestBody LockCouponDTO dto,
                       @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 取消订单释放优惠券 */
    @PostMapping("/inner/coupon/release")
    R<Void> releaseCoupon(@RequestBody ReleaseCouponDTO dto,
                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 查询用户可用优惠券数量 */
    @GetMapping("/inner/coupon/available/count")
    R<Long> countAvailable(@RequestParam("userId") Long userId,
                           @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
```

---

## 2. MQ 消息（异步）

### Topic: ORDER_TIMEOUT_CANCEL_TOPIC

| 属性 | 值 |
|---|---|
| Topic | ORDER_TIMEOUT_CANCEL_TOPIC (见 MqConstants) |
| 消息体 | String (orderNo) |
| 生产者 | OrderInfoServiceImpl.createOrder() |
| 消费者 | OrderTimeoutConsumer |
| 延迟级别 | 16 (RocketMQ, 约 30 分钟) |
| 幂等 | 消费者内通过 orderNo + status=0 查询，已取消则跳过 |
| 当前状态 | ✅ 已实现 |

### Topic: PAYMENT_SUCCESS_TOPIC

| 属性 | 值 |
|---|---|
| Topic | PAYMENT_SUCCESS_TOPIC |
| 消息体 | PaymentSuccessMessage (orderNo, transactionId, payTime) |
| 生产者 | PaymentInfoServiceImpl (支付回调处理中) |
| 消费者 | PaymentSuccessConsumer |
| 幂等 | consumer 内通过 payStatus 校验，已处理则跳过 |
| 当前状态 | ✅ 已实现（需 IOrderInfoService 接口创建后编译通过） |

### Topic: ORDER_AUTO_CONFIRM_TOPIC (待实现)

| 属性 | 值 |
|---|---|
| Topic | ORDER_AUTO_CONFIRM_TOPIC (见 MqConstants) |
| 消息体 | String (orderNo) -- 推测 |
| 生产者 | 待实现（配送后发延迟消息） |
| 消费者 | **未实现** |
| 当前状态 | ⚠️ Topic 已定义但无生产者和消费者 |

---

## 3. 内部 Controller 端点 (Inner 前缀)

被 Feign 调用的内部接口统一使用 `/inner/` 路径前缀，加 `@InnerAuth` 注解。

### share-goods 内部端点

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | /inner/goods/sku/{skuId} | 查询 SKU |
| POST | /inner/goods/stock/deduct | 扣减库存 |
| POST | /inner/goods/stock/release | 释放库存（待实现） |

### share-order 内部端点

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | /inner/order/unfinish/count | 未完成订单数 |
| GET | /inner/order/getByOrderNo | 查询订单 |
| POST | /inner/order/count | 统计订单 |

### share-payment 内部端点

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | /inner/payment/refund | 发起退款（待实现） |

### share-coupon 内部端点

| 方法 | 路径 | 用途（待实现） |
|---|---|---|
| POST | /inner/coupon/claim | 领取优惠券 |
| POST | /inner/coupon/lock | 锁定优惠券 |
| POST | /inner/coupon/release | 释放优惠券 |

---

## 4. Fallback 规范

```java
@Slf4j
@Component
public class RemoteXxxFallbackFactory implements FallbackFactory<R<T>> {

    @Override
    public R<T> create(Throwable cause) {
        log.error("Feign 调用失败", cause);
        // 禁止在 fallback 内 throw ServiceException（会导致 Sentinel 熔断异常误报）
        return R.fail("服务暂时不可用，请稍后再试");
    }
}
```

> 调用方必须校验 `result.getCode() == HttpStatus.SUCCESS`，不能假设 fallback 永远返回成功数据。
