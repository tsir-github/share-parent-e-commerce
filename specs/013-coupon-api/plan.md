# Implementation Plan: 优惠券功能 API

**Branch**: `013-coupon-api` | **Date**: 2026-06-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/013-coupon-api/spec.md`

## Summary

为 C端业主和商家角色补齐优惠券 API 层，扩展 Feign 接口供订单模块调用，配置网关路由。现有 `share-coupon` 模块后端逻辑（领券、锁定、释放）已实现，缺少面向角色的 HTTP 入口和 Feign 契约。

## Technical Context

**Language/Version**: Java 8, Spring Boot 2.7.18, Spring Cloud Alibaba 2021.0.5

**Primary Dependencies**: 
- `share-coupon` 模块：Spring Boot, MyBatis-Plus, Redisson, RocketMQ
- `share-api-coupon`：Spring Cloud OpenFeign
- 外部：Nacos (注册/配置), Sentinel (熔断), Redis Sentinel (分布式锁)

**Storage**: MySQL 5.7 (share-coupon 库) — `coupon_template` + `coupon_user` 表，无需新增表/字段

**Testing**: 编译验证 + LSP diagnostics，后端无单元测试框架

**Target Platform**: Windows (IDEA 开发) → CentOS 7 VM (生产部署)

**Project Type**: 微服务模块（Spring Cloud Alibaba），RESTful API

**Performance Goals**: 领券操作含 Redis 锁 + DB 乐观锁 < 1s；列表查询 < 1s

**Constraints**: 
- 遵循 AGENTS.md 编码规范（Controller 薄层、R<T> vs AjaxResult、路径约定）
- 不新增表/字段
- 下单锁定延用 `lockForOrder()`（status=0→1 覆盖"锁定"语义，支付成功后再更新 used_time）

**Scale/Scope**: 初期 1000 用户量级

## Constitution Check

*GATE: 无自定义约束（constitution.md 为未填写模板），跳过。*

## Project Structure

### Documentation (this feature)

```text
specs/013-coupon-api/
├── spec.md              # Feature specification
├── plan.md              # 本文件
├── research.md          # Phase 0 research
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Phase 1 validation guide
├── checklists/
│   └── api.md           # Requirements checklist
└── tasks.md             # Implementation tasks（由 /speckit.tasks 生成）
```

### Source Code

```text
share-modules/share-coupon/src/main/java/com/share/coupon/
├── controller/
│   ├── CouponApiController.java         # [新建] C端业主 API (/api/v1/coupon/*)
│   └── MerchantCouponController.java    # [新建] 商家 API (/api/v1/merchant/coupon/template/*)
├── domain/
│   └── vo/
│       ├── AvailableCouponVO.java       # [新建] 可领取模板 VO
│       └── MyCouponVO.java             # [新建] 我的优惠券 VO (含模板 JOIN 信息)
├── mapper/                              # 无需修改
├── service/                             # 无需修改
└── constant/                            # 无需修改

share-api/share-api-coupon/src/main/java/com/share/coupon/
├── api/
│   └── RemoteCouponService.java         # [扩展] 追加 lock/release/countAvailable
└── factory/
    └── RemoteCouponFallbackFactory.java  # [扩展] 追加降级方法

share-gateway/ (Nacos config)
└── share-gateway-dev.yml                # [修改] 追加 coupon 路由
```

**Structure Decision**: 遵循项目现有模块结构—Feign 接口在 `share-api-coupon`, 业务实现在 `share-modules/share-coupon`。

## Complexity Tracking

> 无 — 常规 CRUD + Feign 集成，无过度设计风险。
