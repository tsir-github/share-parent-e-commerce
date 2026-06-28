# Implementation Plan: 项目全面审计与基线 — 技术方案

**Branch**: `001-project-audit` | **Date**: 2026-06-23 | **Spec**: specs/001-project-audit/spec.md

**Input**: Feature specification from `/specs/001-project-audit/spec.md`

## Summary

确认项目现有技术栈，规划 16 个已知缺陷的修复方案。项目基于 RuoYi-Cloud v3.6.3，采用 Spring Boot 2.7 + Spring Cloud Alibaba + MyBatis-Plus 微服务架构，前端 Vue 3 + Element Plus。核心业务模块（order/goods/coupon/user/merchant/payment）已构建但存在 P0 级缺陷（IOrderInfoService 接口缺失、领券无并发控制、SQL 注入等），需按 P0→P1→P2 优先级分阶段修复。

## Technical Context

**Language/Version**: Java 8 (OpenJDK), Spring Boot 2.7.x, Spring Cloud Alibaba 2021.x, MyBatis-Plus 3.5.x

**Primary Dependencies**: 
- 注册配置: Nacos 2.x (192.168.10.129:8848)
- 网关: Spring Cloud Gateway (8080)
- 鉴权: JWT + Redis Sentinel (26379/26380/26381)
- 消息队列: RocketMQ 4.9.7 (192.168.10.129:9876)
- 分布式事务: Seata AT 模式 (已集成未使用)
- 熔断: Sentinel (已集成未配置规则)
- 分布式锁: Redisson (已用于订单创建)
- 文件: MinIO / 本地存储
- 前端: Vue 3 + Element Plus (Vite 构建)
- 微信支付: WeChat Pay v3 SDK

**Storage**: MySQL 5.7.38 (192.168.10.129:3306) — 8 个业务库; Redis 7.2.5 Sentinel 三节点; RocketMQ 单节点

**Testing**: JUnit (RuoYi 标准), 当前业务模块无单元测试

**Target Platform**: CentOS 7 VM (1.8GB RAM), nginx 反代; 开发机 Windows 10 + IDEA

**Project Type**: Web service (Spring Cloud 微服务) + Admin frontend (Vue 3 SPA)

**Performance Goals**: 社区电商规模 (小区级)，无严格性能要求；订单创建 < 500ms; 库存扣减乐观锁 3 次重试

**Constraints**: VM 仅 1.8GB RAM; MySQL 5.7 (无 ES/全文检索); 现有表结构冻结不改

**Scale/Scope**: 7 个业务模块 + 6 个 Feign API + 31 个 Controller; 约 146 个 HTTP 端点; 16 个已知缺陷 (P0×4, P1×6, P2×6)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- GATE-001 (Constitution Template): 无自定义约束 — ✅ PASS
- GATE-002 (Project Conventions - AGENTS.md): 需遵循现有编码规范 (分层架构、状态变更规范、异常处理等) — 已知

## Project Structure

### Documentation (this feature)

```text
specs/001-project-audit/
├── plan.md              # This file
├── research.md          # Phase 0: 技术调研结果
├── data-model.md        # Phase 1: 数据模型设计
├── quickstart.md        # Phase 1: 验证指南
├── contracts/           # Phase 1: 接口契约
└── tasks.md             # Phase 2: 实施任务 (由 /speckit.tasks 生成)
```

### Source Code (repository root)

```text
share-parent-e-commerce/
├── share-ui/                 # 前端 Vue 3 + Element Plus (RuoYi)
├── share-gateway/            # 网关 Spring Cloud Gateway
├── share-auth/               # 认证中心 JWT + Redis
├── share-api/                # Feign 接口模块
│   ├── share-api-system/
│   ├── share-api-goods/
│   ├── share-api-order/
│   ├── share-api-user/
│   ├── share-api-payment/    # 只有 domain (无 Feign 接口)
│   ├── share-api-coupon/     # 只有 domain (无 Feign 接口)
│   └── share-api-merchant/   # 只有 domain (无 Feign 接口)
├── share-common/             # 通用工具模块
├── share-modules/            # 业务模块
│   ├── share-system/         # RuoYi 框架管理
│   ├── share-goods/          # 商品服务 [9210]
│   ├── share-order/          # 订单服务 [9211] — 含 MQ 消费者
│   ├── share-payment/        # 支付服务 [9213] — 含 WxPay v3
│   ├── share-coupon/         # 优惠券服务 [9214]
│   ├── share-user/           # 用户服务 [9209] — 含 WxMa 登录
│   ├── share-merchant/       # 商家服务 [9215]
│   ├── share-file/           # 文件服务 [9300]
│   ├── share-gen/            # 代码生成
│   └── share-job/            # 定时任务
└── middleware/               # 本地中间件 (start-middleware.bat)
```

## Complexity Tracking

无 — Constitution Check 无违规。

## Resolved Problems & Decisions

<!-- 由 Phase 0 调研后填充 -->

| # | 问题 | 方案 | 依据 |
|---|------|------|------|
| R01 | IOrderInfoService 接口缺失 | 创建接口（10 方法签名），OrderInfoServiceImpl 加 `implements` | 3 个生产类引用，编译崩溃 (P0) |
| R02 | 商品/用户/支付模块无接口 | 暂不创建（控制器直接注入 Impl 仍可运行），归类为代码规范修复 | 不影响编译运行 (P2) |
| R03 | cancelOrder 不回滚库存/优惠券 | 补 Feign 调用释放逻辑（`RemoteGoodsService.releaseStock` + `RemoteCouponService.releaseCoupon`） | 订单取消后数据不一致 (P1) |
| R04 | 8 个源码文件丢失（仅剩 .class） | 先检查 git stash / IDE local history，否则重写 | target/classes 有编译产物但源码不存在 (P2) |
| R05 | Cart 实体无 Service/Controller/Mapper | 新建购物车基础 CRUD | Entity 存在但无后端代码 (P2) |
| R06 | product_image / seckill_activity 无 Java 代码 | 按需实现 | DB 表存在但无对应源码 (P2) |
| R07 | 微信退款只更新 DB | 用 wechatpay-java SDK 或 WxPayUtil 补真实 API 调用 | 当前 refund() 仅有 mock 分支 (P1) |
| R08 | 领券 4 TODO 无并发控制 | Redisson `tryLock(3,10,SECONDS)` + 乐观锁 `version` | 高并发下超发风险 (P1) |
| R09 | refund callback 端点缺失 | 新建 `POST /api/v1/payment/refund/callback` | 退款是异步的，需回调更新状态 (P1) |
| R10 | PaymentStatus 缺 REFUNDING 状态 | 加 `REFUNDING=3` | 区分"受理中"和"已退款" (P1) |
