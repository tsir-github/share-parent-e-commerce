# Tasks: 支付服务实现

**Input**: Design documents from specs/002-payment-service/

## User Stories

- **US1** (P1): C 端用户下单支付 — 创建支付单 → 调起微信支付 → 异步通知 → 订单更新
- **US2** (P2): 管理员管理支付记录 — 列表查询、退款操作
- **US3** (P1): 系统内部服务间支付调用 — Feign + MQ 通信

---

## Phase 1: Foundation (P0 修复)

**Purpose**: 修复现有代码中阻塞功能的问题

- [ ] T001 [P] [FOUNDATION] 修复 Feign 路径不匹配：在 PaymentInfoController 中增加 /inner/payment/create 和 /inner/payment/{orderNo} 两个 @InnerAuth 端点，使 RemotePaymentService 的 Feign 调用有对应实现

- [ ] T002 [P] [FOUNDATION] 提取 IPaymentInfoService 接口：创建 service/IPaymentInfoService.java，将 PaymentInfoServiceImpl 改为实现该接口，Controller 注入接口而非 Impl 类

- [ ] T003 [P] [FOUNDATION] 修复 WxPayUtil 证书获取：实现 getWechatCertificate() 方法，调用微信 /v3/certificates API 获取平台证书并缓存，支持定期刷新

- [ ] T004 [P] [FOUNDATION] 修复 WxPayUtil 回调解密：实现 decryptBody() 方法，使用 AES-256-GCM 解密微信回调中的加密数据（resource.ciphertext），使用 WxPayConfig.apiKey 派生密钥

- [ ] T005 [P] [FOUNDATION] 创建 Mapper XML：新建 esources/mapper/PaymentInfoMapper.xml，提供基础 CRUD 和复杂查询映射

---

## Phase 2: User Story 1 — C 端下单支付 (P1)

**Goal**: C 端用户可创建支付单、调起微信支付（或 mock）、收到支付成功的异步通知

**Independent Test**: POST create 创建支付单 → POST mock/callback 模拟回调 → 查状态确认

- [ ] T006 [US1] 创建支付入参 DTO：新建 domain/dto/CreatePaymentDTO.java，含 orderNo、amount、description、openid，使用 @NotBlank/@NotNull 校验

- [ ] T007 [US1] 优化 PaymentInfoServiceImpl：注入 IPaymentInfoService 接口，完善 createPayment 方法（Redisson 锁防重、订单号幂等、金额校验）

- [ ] T008 [US1] 完善微信回调处理：handlePayCallback 增加重复通知幂等（transactionId 去重）、支付金额比对校验

- [ ] T009 [US1] 完善 mock 回调：mockPaySuccess 增加 Redisson 锁防并发调用

**Checkpoint**: C 端下单支付全流程可通过 mock 模式验证

---

## Phase 3: User Story 3 — 服务间通信 (P1)

**Goal**: share-order 可通过 Feign 调用支付服务，支付成功后 MQ 通知 order 更新订单

**Independent Test**: 启动 share-order，调用 createPayment Feign → 确认服务注册

- [ ] T010 [US3] 创建 MQ 封装类：新建 mq/PaymentMQProducer.java，封装 sendPaySuccessMessage() 方法，统一 MQ 发送逻辑

- [ ] T011 [US3] 改造 ServiceImpl 使用 MQ Producer：将 PaymentInfoServiceImpl 中的直接 rocketMQTemplate 调用改为注入 PaymentMQProducer

- [ ] T012 [US3] 验证 Feign 通信：确认 RemotePaymentService 的 createPayment 和 getPaymentByOrderNo 与 Controller 新增的 @InnerAuth 端点匹配

**Checkpoint**: share-order 可在下单后调用支付服务，支付成功后收到 MQ 通知

---

## Phase 4: User Story 2 — 管理端支付记录 (P2)

**Goal**: 管理员可在 RuoYi 后台查看支付列表、发起退款

**Independent Test**: GET /list 查看分页 → POST /api/v1/payment/refund 退款

- [ ] T013 [US2] 创建管理端 VO：新建 domain/vo/PaymentInfoVO.java，金额转字符串、状态码转中文说明

- [ ] T014 [US2] 修复 list 接口路径：将 list 接口改为 GET /api/v1/payment/list，添加 @Log 注解，使用 IPaymentInfoService.page() 分页

- [ ] T015 [US2] 优化退款逻辑：refund 方法增加 Redisson 锁防并发、退款金额 ≤ 实付金额校验、退款原因必填校验

**Checkpoint**: 管理员可查询支付列表并发起退款

---

## Phase 5: 补偿与健壮性 (P1/P2)

**Purpose**: 确保消息可靠性、数据一致性

- [ ] T016 [P] [CROSS] 创建 MQ 补偿定时任务：新建 	ask/PaymentCompensationTask.java，使用 @Scheduled + Redisson 锁，每分钟扫描 payment_info 中 status=1 且 callback_time 超过 5 分钟但 MQ 未成功投递的记录，重新发送 PAYMENT_SUCCESS_TOPIC

- [ ] T017 [P] [CROSS] 使用 Jackson 替换字符串解析：将 PaymentInfoServiceImpl 中的 extractValue() 字符串操作改为使用 Jackson ObjectMapper 解析回调 JSON

**Checkpoint**: 所有用户故事均可独立验证，支付流程完整闭环

---

## 执行顺序

`
Phase 1 (Foundation) ─── 必须先完成，阻塞所有 US
  ├── T001 Feign 路径修复     [US1/US3 需要]
  ├── T002 Service 接口提取    [US1 需要]
  ├── T003 WxPayUtil 证书      [US1 生产需要]
  ├── T004 WxPayUtil 解密      [US1 生产需要]
  └── T005 Mapper XML          [可并行]
  ↓
Phase 2 (US1) ─── C 端支付
Phase 3 (US3) ─── Feign+MQ   [可与 US1 并行]
  ↓
Phase 4 (US2) ─── 管理端     [依赖 Phase 1]
Phase 5 (补偿)  ─── 健壮性   [依赖 Phase 2/3]
`

建议顺序：T001→T002→T005 (Phase 1) → T006→T007→T010→T011 (US1+US3 并行) → T013→T014→T015 (US2) → T016→T017 (补偿)
