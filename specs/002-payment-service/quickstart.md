# Quickstart: 支付服务开发指南

## 环境要求

- JDK 21
- Nacos 192.168.10.129:8848 (命名空间 a746e297-417e-4aec-bfb1-e42df33fbe93)
- MySQL share-payment 库（payment_info 表已建）
- RocketMQ 192.168.10.129:9876 (autoCreateTopicEnable=true)
- Redis Sentinel 192.168.10.129:26379/26380/26381

## 启动步骤

1. 确保中间件运行：Nacos / MySQL / RocketMQ / Redis Sentinel
2. 启动 Seata + Sentinel Dashboard（双击 middleware/start-middleware.bat）
3. 在 IDEA 中启动 SharePaymentApplication（port 9213）
4. 验证：Nacos 服务列表 → 命名空间 a746e297 → 看到 share-payment

## Nacos 配置

已推送 share-payment-dev.yml。如需修改微信支付参数：
- 登录 Nacos 控制台 → 配置管理 → share-payment-dev.yml
- 修改 wx.pay.v3 下的 app-id / api-key / cert-serial / private-key-path

## 开发模式

默认 mock-mode=true，不调微信 API。
- 创建支付后 → POST /api/v1/payment/mock/callback?orderNo=xxx 模拟成功
- 退款直接在 mock-mode 下立即完成

生产环境前：
1. 关闭 mock-mode（设为 false）
2. 补充 wx.pay.v3 真实配置
3. 确保 WxPayUtil.getWechatCertificate() 和 decryptBody() 已实现
4. 确认 Feign 路径 /inner/payment/* 与 Controller 一致

## 验证方法

### 单元
- 创建支付: POST /api/v1/payment/create
- 模拟回调: POST /api/v1/payment/mock/callback?orderNo=xxx
- 查询状态: GET /inner/payment/status/{orderNo} (需 InnerAuth)
- 退款: POST /api/v1/payment/refund

### 集成
- 启动 share-order，下单 → 确认 Feign 调用成功
- 启动 share-goods（如需完整链路）
