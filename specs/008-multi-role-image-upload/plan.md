# Implementation Plan: 多角色图片上传能力

**Branch**: `008-multi-role-image-upload` | **Date**: 2026-06-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-multi-role-image-upload/spec.md`

## Summary

为 C端业主（微信小程序）和商家后台用户（商家本人+管理员）提供图片上传能力。核心思路：复用已有 share-file 服务的文件上传能力，在各业务模块封装面向具体场景的上传接口。不需要改现有数据库字段，product_image 表已存在，仅需新建 review_image 表。

## Technical Context

**Language/Version**: Java 21, Spring Boot 2.7

**Primary Dependencies**: Spring Cloud Alibaba, MyBatis-Plus, MinIO (通过 share-file), WxMaService

**Storage**: MySQL 5.7 + MinIO (对象存储) + Redis Sentinel

**Testing**: JUnit 5 + Postman 联调

**Target Platform**: 后端微服务 (CentOS 7 VM 部署)

**Project Type**: 微服务后端 (Spring Cloud)，已有 share-file 做底层文件存储

**Performance Goals**: 上传成功率 ≥99.5%，用户从选图到完成 ≤10 秒

**Constraints**: 单张 ≤10MB，单次最多9张，仅允许 JPEG/PNG/WebP/GIF

**Scale/Scope**: 小区级电商，初期用户量较小

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

宪法为模板未填写实质性约束，无违规项。 ✅ PASS（Phase 1 后复检通过）

## Project Structure

### Documentation (this feature)

```text
specs/008-multi-role-image-upload/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (existing modules)

```text
share-modules/
├── share-user/
│   └── controller/UserAvatarController.java       # 新增: C端头像上传
│
├── share-merchant/
│   └── controller/MerchantUploadController.java   # 新增: 店铺Logo上传
│
├── share-goods/
│   ├── controller/MerchantProductController.java  # 复用: 已有商品CRUD + 图片
│   └── domain/ProductImage.java                   # 已有: 商品多图实体
│
├── share-order/
│   ├── domain/ReviewImage.java                    # 新增: 评价晒图实体
│   └── service/                                   # 新增: 评价+晒图服务
│
└── share-file/
    └── controller/SysFileController.java          # 已有: POST /upload
```

**Structure Decision**: 在现有微服务模块中新增 controller/service，不新建模块。各模块通过 Feign 调用 share-file 上传获取URL后写入自身数据库字段。

## Complexity Tracking

无违规项，无需填写。
