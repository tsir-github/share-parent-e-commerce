# Feature Specification: 支付服务实现

**Feature Branch**: `002-payment-service`

**Created**: 2026-06-23

**Status**: Draft

**Input**: User description: "根据不同角色完成share-payment服务"

## User Scenarios & Testing

### User Story 1 — C 端用户下单支付 (Priority: P1)

用户在微信小程序提交订单后，调起微信支付完成付款，支付成功后订单状态自动更新。

**Why this priority**: 支付是整个电商交易闭环的核心环节，C 端用户支付流程不通则业务不可用。

**Independent Test**: 用户提交订单 → 系统创建支付单 → 用户完成支付 → 订单状态更新为已支付。

**Acceptance Scenarios**:

1. **Given** 用户提交订单成功，**When** 系统调用支付服务创建支付单，**Then** 支付单创建成功，记录订单号、金额、状态为待支付
2. **Given** 用户发起微信支付，**When** 微信返回支付参数（prepay_id），**Then** 前端可调起微信支付
3. **Given** 用户支付成功，**When** 微信异步通知支付结果，**Then** 系统验签通过、更新支付单为已支付
4. **Given** 支付单更新为已支付，**When** 系统发送 RocketMQ 支付成功消息，**Then** 订单模块消费消息并更新订单状态
5. **Given** 用户支付时中途取消，**When** 不支付离开，**Then** 支付单保持待支付状态，订单超时后自动取消

---

### User Story 2 — 管理员管理支付记录 (Priority: P2)

后台管理员查看支付记录列表、详情，处理异常支付和退款审核。

**Why this priority**: 支付涉及资金，运营人员需要查询和干预能力。RuoYi 后台管理框架已有但缺支付管理页面。

**Independent Test**: 管理员登录后台 → 进入支付管理 → 查看支付记录列表和详情。

**Acceptance Scenarios**:

1. **Given** 管理员登录后台，**When** 访问支付管理页面，**Then** 展示支付记录分页列表（订单号、金额、状态、时间）
2. **Given** 管理员点击某条支付记录，**When** 查看详情，**Then** 展示完整的支付信息（微信交易号、回调时间、回调内容等）
3. **Given** 管理员需要退款，**When** 发起退款操作，**Then** 系统调微信退款 API 并更新支付单状态

---

### User Story 3 — 系统内部服务间支付调用 (Priority: P1)

订单服务（share-order）通过 Feign 接口和 RocketMQ 与支付服务交互。

**Why this priority**: 微服务架构下，服务间通信是基础能力。share-order 已定义了 Feign 调用和 MQ 消费者的接口契约。

**Independent Test**: share-order 调用支付服务创建支付 → 支付服务返回创建结果 → 支付完成后 MQ 通知 order。

**Acceptance Scenarios**:

1. **Given** share-order 调用 createPayment Feign 接口，**When** 传入订单号和金额，**Then** 支付服务创建支付单并返回创建结果
2. **Given** share-order 查询支付结果，**When** 调用 getPaymentByOrderNo Feign 接口，**Then** 返回支付单完整信息
3. **Given** 支付单支付成功，**When** 支付服务发送 PAYMENT_SUCCESS_TOPIC 消息，**Then** share-order 的 PaymentSuccessConsumer 收到消息并处理

---

### Edge Cases

- **微信回调重复通知**：微信支付可能多次发送异步通知，需通过 transactionId 或 paymentNo 唯一索引幂等
- **支付成功但 MQ 发送失败**：支付单已更新但消息未发出，需定时任务补偿扫描待通知的支付单
- **退款金额超过实付金额**：退款时校验退款金额 ≤ 实付金额，不允许超退
- **支付单已关闭后收到回调**：订单超时取消导致支付单关闭，此时收到支付回调需区分处理（先记录回调，再通知订单模块协商处理）
- **mock 模式 vs 真实微信支付**：开发/测试环境使用 mock 模式（不调微信 API），生产环境自动切换真实模式
- **并发退款**：同一笔支付单多人同时退款，需加锁防止重复退款

## Requirements

### Functional Requirements

- **FR-001**: 系统 MUST 支持创建支付单（orderNo, amount, userId）并返回支付参数
- **FR-002**: 系统 MUST 支持微信支付 v3 统一下单 API 调用（获取 prepay_id）
- **FR-003**: 系统 MUST 支持微信支付异步通知验签（验证签名、解密数据、更新支付单）
- **FR-004**: 系统 MUST 支持支付成功后发送 RocketMQ 消息通知订单模块
- **FR-005**: 系统 MUST 支持通过 paymentNo 或 orderNo 查询支付单
- **FR-006**: 系统 MUST 支持退款操作（调微信退款 API + 更新支付单状态）
- **FR-007**: 系统 MUST 支持 mock 模式（不调微信 API，模拟支付成功流程）
- **FR-008**: 系统 MUST 支持定时任务补偿：扫描已支付但 MQ 未发出的支付单
- **FR-009**: 管理员 MUST 能分页查询支付记录列表和详情
- **FR-010**: 管理员 MUST 能发起退款操作
- **FR-011**: 所有对外接口 MUST 校验权限（C 端接口需登录，内部 Feign 接口加 @InnerAuth）

### Key Entities

- **PaymentInfo**: 支付记录。关联字段：orderNo（订单号）、userId（用户ID）、transactionId（微信交易号）、amount（金额）、paymentStatus（支付状态：0-待支付 1-已支付 2-已退款）。继承 BaseEntity（含 createTime, updateTime, delFlag）。
- **PaymentInfoVO**: 管理端展示视图，金额转字符串、状态码转中文说明。
- **PaymentSuccessMessage**: MQ 消息 DTO，含 orderNo + transactionId，已定义在 share-api-payment。

## Success Criteria

### Measurable Outcomes

- **SC-001**: C 端用户下单支付全流程（创建支付单 → 调起微信支付 → 异步通知 → 订单更新）可在 30 秒内完成
- **SC-002**: 微信支付回调处理响应时间 < 200ms（含验签 + 更新 DB + 发 MQ）
- **SC-003**: 退款发起后，支付单状态 100% 正确更新（已支付 → 已退款），不会出现状态跳跃
- **SC-004**: MQ 消息补偿定时任务扫描周期 ≤ 1 分钟，确保支付成功消息可靠投递
- **SC-005**: mock 模式下支付流程可在无微信 API 环境下完整调试

## Assumptions

- **微信支付 v3 是唯一支付方式**：当前仅集成微信支付 v3，不接入支付宝或其他渠道
- **mock 模式用于非生产环境**：Nacos 配置 `payment.mock-mode=true` 时，所有微信 API 调用走模拟路径
- **支付服务和订单服务通过 MQ 解耦**：支付成功消息通过 RocketMQ Topic `PAYMENT_SUCCESS_TOPIC` 投递
- **微信商户号已在 Nacos 配置**：商户号 1631833859、API v3 密钥、证书等敏感配置在 Nacos `share-payment-dev.yml` 中
- **RuoYi `@RequiresPermissions` 用于管理端鉴权**：后台接口遵循 RuoYi 权限体系
- **C 端接口通过 `@RequiresLogin` 鉴权**：用户需登录后访问支付相关接口
- **定时任务使用 `@Scheduled` + 分布式锁**：弥补补偿任务通过 Redisson 锁防止重复执行

## 模块实现计划

### share-payment 模块需创建的文件

```
share-modules/share-payment/src/main/java/com/share/payment/
├── controller/
│   └── PaymentController.java          # C 端支付接口（创建支付、查询支付）
├── controller/
│   └── PaymentManageController.java    # 管理端接口（列表、详情、退款）
├── service/
│   ├── IPaymentInfoService.java        # 服务接口
│   └── impl/
│       └── PaymentInfoServiceImpl.java # 服务实现（核心业务逻辑）
├── mapper/
│   └── PaymentInfoMapper.java          # MyBatis-Plus Mapper
├── domain/
│   ├── vo/
│   │   └── PaymentInfoVO.java          # 管理端展示 VO
│   └── dto/
│       └── CreatePaymentDTO.java       # 创建支付入参 DTO
├── mq/
│   └── PaymentMQProducer.java          # MQ 消息发送（支付成功通知）
├── config/
│   └── WechatPayConfig.java            # 微信支付配置（从 Nacos 读取）
├── service/
│   └── WechatPayService.java           # 微信支付 API 封装（统一下单、回调验签、退款）
└── resources/
    └── mapper/
        └── PaymentInfoMapper.xml       # MyBatis XML（复杂查询）
```

### share-payment 启动配置需更新

- Nacos `share-payment-dev.yml`：已推送基础配置，需补充微信支付 v3 专项配置（商户号、API v3 密钥、证书路径、回调地址、mock 模式开关）
- `bootstrap.yml`：已就绪
- 网关路由 `/payment/**` → StripPrefix=1 → lb://share-payment：已就绪
