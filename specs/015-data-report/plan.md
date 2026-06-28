# Implementation Plan: 平台数据报表

**Branch**: `015-data-report` | **Date**: 2026-06-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-data-report/spec.md`

## Summary

平台管理员查看平台级经营数据报表：交易总览（订单数/交易额/客单价）、订单趋势（折线图）、商品销售排行、商家销售排行、用户统计、支付统计。全部从现有业务表实时聚合，无需新表。功能分布在 **share-order**（4 个报表: 交易总览/趋势/商品排行/商家排行/支付统计）和 **share-user**（用户统计）模块。

## Technical Context

**Language/Version**: Java 17, Spring Boot 2.7.x

**Primary Dependencies**: Spring Cloud Alibaba, MyBatis-Plus 3.5

**Storage**: MySQL 5.7.38 — 跨库查询使用 `db.table` 语法（已在 `MerchantDashboardMapper.xml` 中验证可行）

**Testing**: 本项目中不使用自动化测试框架，验证方式为编译检查 + 手动 curl/页面验证

**Target Platform**: CentOS 7 VM（微服务在 Windows IDEA 启动，nginx 反代）

**Project Type**: Web 微服务（share-order port 9211 + share-user port 9209）

**Performance Goals**: 
- 交易总览/趋势: 30 天内数据 < 2 秒返回
- 商品/商家排行: Top 100 < 3 秒返回
- 所有接口支持 5 个以内管理员同时访问

**Constraints**: 遵循 `AGENTS.md` 编码规范：
- Controller 继承 `BaseController`，薄层收口
- 返回类型: `AjaxResult`/`TableDataInfo`（管理员端），不使用 `R<T>`
- 鉴权: `@RequiresPermissions`，不使用 `@RequiresLogin`
- 写操作追加 `@Log` 注解（本功能全部为 GET 只读查询，无写操作）
- 禁止 `@Autowired` 字段注入（使用 `@RequiredArgsConstructor` + `final`）

**Scale/Scope**: 小区电商平台，日订单量预估在数百级别，报表数据实时聚合即可满足

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 为模板占位符（未配置具体原则），无约束规则。Gate: ✅ Pass

## Project Structure

### Documentation (this feature)

```text
specs/015-data-report/
├── plan.md              # 本文件
├── spec.md              # 功能规格说明
├── research.md          # 技术调研记录
├── data-model.md        # 数据模型（无新表，仅 VO 定义）
├── quickstart.md        # 验证指南
├── contracts/           # API 接口契约
│   └── api.md
└── checklists/          # 质量检查
    └── requirements.md
```

### Source Code

```text
# share-order 模块（负责 5 个报表）
share-modules/share-order/src/main/java/com/share/order/
├── controller/
│   └── DataReportController.java          # 平台数据报表（交易总览/趋势/排行/支付统计）
├── service/
│   └── IDataReportService.java            # 报表服务接口
├── service/impl/
│   └── DataReportServiceImpl.java         # 报表服务实现（聚合 SQL 查询）
└── mapper/
    └── DataReportMapper.java              # 报表 Mapper

share-modules/share-order/src/main/resources/mapper/
└── DataReportMapper.xml                   # 聚合 SQL（跨库查询）

# share-user 模块（负责用户统计）
share-modules/share-user/src/main/java/com/share/user/
├── controller/
│   └── UserInfoController.java            # 已有，扩展 getDashboardStats()
└── service/impl/
    └── UserInfoServiceImpl.java           # 已有，扩展统计方法

# Feign DTO（如需跨模块返回结构体）
share-api/share-api-order/src/main/java/com/share/order/domain/vo/
└── 无需新建（所有报表返回 Map<String, Object>）
```

**Structure Decision**: 报表功能按数据归属放在 share-order（订单/支付）和 share-user（用户）模块，复用现有的跨库查询模式（`db.table`）。

## Complexity Tracking

无违规项，Constitution Check 通过。
