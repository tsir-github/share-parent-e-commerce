# Implementation Plan: 秒杀活动管理后台

**Branch**: `014-flash-sale-admin` | **Date**: 2026-06-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-flash-sale-admin/spec.md`

## Summary

平台管理员创建/管理秒杀活动（选择商品 SKU、设置秒杀价/库存/限购/时间），支持分页查询、状态筛选、启用/禁用、删除、查看统计数据。功能归属于 **share-goods** 模块（`seckill_activity` 表及商品 SKU 实体已在该模块）。已有 `seckill_activity` 表需改造补充字段。

## Technical Context

**Language/Version**: Java 17, Spring Boot 2.7.x

**Primary Dependencies**: Spring Cloud Alibaba, MyBatis-Plus 3.5, Redisson, RocketMQ 4.9.7

**Storage**: MySQL 5.7.38 (share-goods 数据库), Redis Sentinel（缓存/分布式锁/计数器）

**Testing**: 本项目中不使用自动化测试框架，验证方式为编译检查 + 自测

**Target Platform**: CentOS 7 VM（微服务在 Windows IDEA 启动，通过 nginx 反代）

**Project Type**: Web 微服务（Spring Cloud Alibaba, share-goods module, port 9210）

**Performance Goals**: 秒杀活动管理后台为低频操作，无特殊性能要求。需注意启用/禁用操作的实时性（禁用后 C 端 5 秒内不可见）

**Constraints**: 遵循 `AGENTS.md` 编码规范：
- Controller 继承 `BaseController`，薄层收口
- 返回类型: `AjaxResult`/`TableDataInfo`（管理员端），不使用 `R<T>`
- 鉴权: `@RequiresPermissions`，不使用 `@RequiresLogin`
- 写操作追加 `@Log` 注解
- 禁止 `updateById` 更新状态字段（用 `LambdaUpdateWrapper`）
- 禁止 `@Autowired` 字段注入（使用 `@RequiredArgsConstructor` + `final`）

**Scale/Scope**: 小区电商平台，秒杀活动数量预估在数百级别，管理后台接口 QPS 低

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 为模板占位符（未配置具体原则），无约束规则。Gate: ✅ Pass

## Project Structure

### Documentation (this feature)

```text
specs/014-flash-sale-admin/
├── plan.md              # 本文件
├── spec.md              # 功能规格说明
├── research.md          # 技术调研记录
├── data-model.md        # 数据模型设计
├── quickstart.md        # 验证指南
├── contracts/           # API 接口契约
└── tasks.md             # 实施任务（由 /speckit.tasks 生成）
```

### Source Code (share-goods module)

```text
share-modules/share-goods/src/main/java/com/share/goods/
├── controller/
│   └── SeckillActivityController.java     # 管理后台秒杀活动 CRUD
├── service/
│   ├── ISeckillActivityService.java        # 接口
│   └── impl/
│       └── SeckillActivityServiceImpl.java # 实现
└── mapper/
    ├── SeckillActivityMapper.java          # Mapper
    └── xml/
        └── SeckillActivityMapper.xml       # SQL（如需）

share-api/share-api-goods/src/main/java/com/share/goods/domain/
└── SeckillActivity.java                    # Entity（需新建）
```

**Structure Decision**: 单模块（share-goods）内新增标准 Controller/Service/Mapper 三层，遵循项目现有分层架构。Entity 放在 `share-api-goods` 供 Feign 复用。

## Complexity Tracking

无违规项，Constitution Check 通过。
