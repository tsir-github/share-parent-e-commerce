# Phase 0 Research: 支付服务实现

**Date**: 2026-06-24
**Spec**: [spec.md](./spec.md)

---

## 1. 现有代码审计 — 模块完整度

share-payment 模块 **不是空骨架**，已有大量代码。审计结果是代码覆盖率约 **75%**，核心业务逻辑已经实现。

### 已存在文件清单

| 路径 | 行数 | 状态 | 说明 |
|------|------|------|------|
| SharePaymentApplication.java | ~20 | ✅ 完整 | 标准 RuoYi 启动类 |
| controller/PaymentInfoController.java | ~100 | ✅ 完整 | 7 个接口 |
| service/impl/PaymentInfoServiceImpl.java | ~260 | ✅ 完整 | 核心业务逻辑齐全 |
| mapper/PaymentInfoMapper.java | ~15 | ✅ 有 | extends BaseMapper |
| config/WxPayConfig.java | ~60 | ✅ 完整 | @ConfigurationProperties + @RefreshScope |
| utils/WxPayUtil.java | ~200 | 🟡 部分实现 | 签名/HTTP 完整，证书+解密未实现 |
| ootstrap.yml | ~25 | ✅ 有 | Nacos 配置齐全，port 9213 |
| pom.xml | ~70 | ✅ 有 | 依赖齐全 |
| domain/ (本地) | — | ❌ 空 | VO/DTO 不存在 |
| esources/mapper/ | — | ❌ 空 | Mapper XML 不存在 |

### share-api-payment 已存在文件

| 路径 | 状态 | 说明 |
|------|------|------|
| domain/PaymentInfo.java | ✅ 完整 | Entity，extends BaseEntity @Builder |
| domain/dto/PaymentSuccessMessage.java | ✅ 完整 | MQ DTO |
| pi/RemotePaymentService.java | ✅ 完整 | Feign 接口 |
| actory/RemotePaymentFallbackFactory.java | ✅ 完整 | Sentinel 降级 |

### Controller 接口汇总

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| createPayment | POST /api/v1/payment/create | @RequiresLogin | C 端创建支付单 |
| payCallback | POST /api/v1/payment/callback | 无 | 微信支付异步通知 |
| refundCallback | POST /api/v1/payment/refund/callback | 无 | 微信退款异步通知 |
| mockCallback | POST /api/v1/payment/mock/callback | 无 | 模拟支付成功 |
| refund | POST /api/v1/payment/refund | @RequiresPermissions | 管理员退款 |
| list | GET /list | @RequiresPermissions | 管理端分页查询 |
| getPaymentStatus | GET /inner/payment/status/{orderNo} | @InnerAuth | Feign 查询 |
| add | POST | @RequiresPermissions | 管理端新增 |

---

## 2. WeChat Pay v3 集成分析

### WxPayUtil 功能覆盖

| 功能 | 实现状态 | 问题 |
|------|----------|------|
| 签名生成 (SHA256withRSA) | ✅ 完整 | 使用商户私钥签名 |
| 发送 HTTP POST 请求 | ✅ 完整 | java.net.http.HttpClient |
| 统一下单 (JSAPI) | ✅ 完整 | 调用 /v3/pay/transactions/jsapi |
| 构建前端调起参数 | ✅ 完整 | appId + timeStamp + nonceStr + package + paySign |
| 退款 API 调用 | ✅ 完整 | 调用 /v3/refund/domestic/refunds |
| 验签 | ✅ 完整 | 通过平台证书公钥验证签名 |
| **获取微信平台证书** | ❌ 返回 null | getWechatCertificate() 未实现 |
| **回调数据解密 (AEAD)** | ❌ 直接返回原文 | AES-256-GCM 未实现 |
| **prepay_id 提取** | ⚠️ 脆弱 | 字符串 indexOf 而非 JSON |

---

## 3. 数据库 schema

### payment_info 表字段设计

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int PK | 自增主键 |
| user_id | bigint | 用户ID |
| order_no | varchar(50) | 订单号 |
| pay_way | tinyint | 1-微信 |
| transaction_id | varchar(50) | 微信支付交易号 |
| amount | decimal(10,2) | 支付金额 |
| content | varchar(200) | 交易内容 |
| payment_status | tinyint | 0-未支付 1-已支付 2-已退款 3-退款中 |
| callback_time | datetime | 回调时间 |
| callback_content | text | 回调原始数据 |
| remark | varchar(255) | 备注 |
| create_time/update_time/del_flag | | BaseEntity 字段 |

### 支付状态机

`
UNPAID(0) ──→ PAID(1) ──→ REFUNDING(3) ──→ REFUNDED(2)
                   │              ▲
                   └──────────────┘ (直接退款)
`

所有状态转换使用 LambdaUpdateWrapper + eq(paymentStatus) 保证并发安全。

---

## 4. MQ 通信模式

`
PaymentInfoServiceImpl → RocketMQ("order-pay-success")
  → share-order.PaymentSuccessConsumer
  → orderInfoService.processPaySuccess()
  → IOrderStatusService.transition()
`

**问题**：MQ 发送在事务内但非事务消息，缺少补偿定时任务（FR-008）。

---

## 5. Feign 路径不匹配

| Feign 路径 | Controller 路径 | 需修复 |
|-----------|---------------|--------|
| POST /inner/payment/create | 不存在 | 需新增 @InnerAuth 端点 |
| GET /inner/payment/{orderNo} | GET /inner/payment/status/{orderNo} | 需新增或修正 |

---

## 6. 修改/新建文件清单

### 需修改文件
1. PaymentInfoController.java — 增加 Feign 端点，修复 list 路径
2. PaymentInfoServiceImpl.java — 改为实现接口
3. WxPayUtil.java — 实现 getWechatCertificate() + decryptBody()

### 需新建文件
1. service/IPaymentInfoService.java
2. domain/vo/PaymentInfoVO.java
3. domain/dto/CreatePaymentDTO.java
4. mq/PaymentMQProducer.java
5. esources/mapper/PaymentInfoMapper.xml
6. 	ask/PaymentCompensationTask.java

---

## 7. 开发顺序

**P0**: Feign 路径修复 → Service 接口提取 → WxPayUtil 证书+解密  
**P1**: REST 路径规范 → MQ 补偿任务 → VO/DTO  
**P2**: 退款并发锁 → JSON 解析替换
