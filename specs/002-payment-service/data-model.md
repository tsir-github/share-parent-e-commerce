# Data Model: 支付服务数据模型

## 1. 实体 (Entity)

### PaymentInfo（支付记录）

**表名**: payment_info  
**数据库**: share-payment  
**位置**: share-api/share-api-payment/domain/PaymentInfo.java

| 字段 | 类型 | 说明 | 验证规则 |
|------|------|------|----------|
| id | Long | 主键 | 自增 |
| userId | Long | 用户ID | 必填 |
| orderNo | String | 订单号 | 必填，唯一 |
| payWay | Integer | 付款方式 1-微信 | 必填 |
| transactionId | String | 微信支付交易号 | 支付成功后回填 |
| amount | BigDecimal | 支付金额 | > 0 |
| content | String | 交易描述 | 可选 |
| paymentStatus | Integer | 0-未支付 1-已支付 2-已退款 3-退款中 | 必填 |
| callbackTime | Date | 微信回调时间 | 回填 |
| callbackContent | String | 回调原始报文(TEXT) | 回填 |
| remark | String | 备注 | 可选 |
| createTime | Date | 创建时间 | BaseEntity |
| createBy | String | 创建人 | BaseEntity |
| updateTime | Date | 更新时间 | BaseEntity |
| updateBy | String | 更新人 | BaseEntity |
| delFlag | String | 逻辑删除 0-正常 2-删除 | BaseEntity |

### 状态常量

`java
// share-common-core PaymentStatus.java（已存在）
public class PaymentStatus {
    public static final Integer UNPAID = 0;     // 未支付
    public static final Integer PAID = 1;       // 已支付
    public static final Integer REFUNDED = 2;   // 已退款
    public static final Integer REFUNDING = 3;  // 退款中
}
`

`java
// share-common-core MqConstants.java（已存在）
public interface MqConstants {
    String PAYMENT_SUCCESS_TOPIC = "order-pay-success";
}
`

## 2. 值对象 (VO)

### PaymentInfoVO — 管理端展示

`java
// share-payment/domain/vo/PaymentInfoVO.java（需新建）
public class PaymentInfoVO {
    private Long id;
    private Long userId;
    private String orderNo;
    private Integer payWay;
    private String payWayStr;       // "微信支付"
    private String transactionId;
    private String amountStr;       // "99.00"
    private String content;
    private Integer paymentStatus;
    private String paymentStatusStr; // "未支付/已支付/已退款"
    private Date callbackTime;
    private String remark;
    private Date createTime;
}
`

## 3. 数据传输对象 (DTO)

### CreatePaymentDTO — 创建支付入参

`java
// share-payment/domain/dto/CreatePaymentDTO.java（需新建）
public class CreatePaymentDTO {
    @NotBlank private String orderNo;
    @NotNull private BigDecimal amount;
    @NotBlank private String description;
    private String openid;           // 微信 openid，JSAPI 需要
}
`

### PaymentSuccessMessage — MQ 消息（已存在）

`java
// share-api-payment/domain/dto/PaymentSuccessMessage.java
public class PaymentSuccessMessage implements Serializable {
    private String orderNo;
    private String transactionId;
}
`

## 4. 状态机定义

`
[UNPAID:0] ──支付成功──→ [PAID:1]
[PAID:1]   ──发起退款──→ [REFUNDING:3]
[PAID:1]   ──mock退款──→ [REFUNDED:2]
[REFUNDING:3] ─退款回调─→ [REFUNDED:2]
`

所有状态变更条件：
- 必须使用 LambdaUpdateWrapper.set() + eq(当前状态)
- 不允许跨状态跳跃（如 UNPAID → REFUNDED）
- 幂等：PAID 状态再次收到回调直接跳过
