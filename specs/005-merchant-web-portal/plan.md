# Implementation Plan: 商家端 Web 后台

**Branch**: `005-merchant-web-portal` | **Date**: 2026-06-25 | **Spec**: specs/005-merchant-web-portal/spec.md

## Summary

在现有 share-ui（Vue 3 + Element Plus）中扩展商家角色路由，在 share-auth 中增加商家登录支持，在各业务模块增加商家视角的 API（商品 CRUD/订单发货/售后审核），实现商家数据隔离。商家拒绝售后后转客服处理，配送员手动录入无独立账号。

## Technical Context

**Language/Version**: Java 8 / Spring Boot 2.7 / Vue 3 / Element Plus

**Primary Dependencies**: RuoYi-Cloud v3.6.3 / MyBatis-Plus / Nacos / Redis / RocketMQ

**Storage**: MySQL (本地 localhost:3306 + VM 192.168.10.129:3306 需同步迁移)

**Testing**: 手动集成测试

**Target Platform**: Web (PC browser)

**Project Type**: Web application (前端 + 后端微服务)

**Performance Goals**: 商家页面加载 < 3s，数据隔离 100%

**Constraints**: 复用现有 RuoYi RBAC 体系，不新建前端工程

**Scale/Scope**: 预计 50-200 个商家，每个商家管理 10-200 个商品

## Constitution Check

GATE PASSED — 项目无 constitution 约束，遵循 AGENTS.md 编码规范。

## Research Summary

### 现有代码分析

| 查询 | 结果 |
|------|------|
| Product 实体 | 已有 `merchantId` 字段 ✅ |
| OrderInfo 实体 | 有 `supplierId`（实为商家ID），无 `merchantId` |
| MerchantInfo 实体 | 无 `userId`，无法关联登录账号 |
| 商家模块 share-merchant | Controller/Service/Mapper 存在，权限基于 `@RequiresPermissions("merchant:merchant:*")` |
| 商品模块 share-goods | 现有 Controller 带 `@RequiresPermissions("goods:*")`，是管理员视角 |
| 订单模块 share-order | OrderInfoApiController 已有 deliver 接口 |
| share-auth | TokenController + H5TokenController，暂无私商家登录 |
| share-ui 路由 | 从后端返回（`/system/menu/getRouters`），views 目录自动发现 |
| share-ui views | 仅有 system/monitor/tool，无任何业务页面 |

### 关键设计决策

1. **商家登录**：新建 `merchant_user` 表（独立于 admin 的 sys_user），share-auth 新增 `/auth/merchant/login` 端点，返回携带 `merchantId` 和角色 `merchant` 的 token
2. **数据隔离**：在查询时根据 token 中的 `merchantId` 过滤数据，不依赖 RuoYi 的 DataScope 注解
3. **商家 API**：在各模块新增 `MerchantXxxController`，路径 `/api/v1/merchant/xxx`，通过网关转发
4. **订单 merchant_id**：OrderInfo 的 `supplierId` 字段就是商家 ID，查询时使用 `supplierId` 过滤
5. **分类**：商家只能读取分类（`CategoryController.treeselect` 无需权限），不能修改
6. **前端路由**：商家菜单在 `sys_menu` 中配置，后端根据角色返回

## Project Structure

### Documentation

```text
specs/005-merchant-web-portal/
├── plan.md              # 本文件
├── spec.md              # 功能规格
├── data-model.md        # 数据模型变更
├── quickstart.md        # 验证指南
├── contracts/           # 接口契约
│   └── interfaces.md
├── sql/
│   └── migration.sql    # 数据库迁移
├── checklists/
│   └── requirements.md
└── tasks.md             # 实现任务（由 /speckit.tasks 生成）
```

### Source Code

```text
# 后端变更
share-auth/
  └── src/main/java/com/share/auth/controller/
      └── MerchantTokenController.java      # 新增：商家登录接口

share-modules/share-merchant/
  ├── src/main/java/com/share/merchant/
  │   ├── controller/
  │   │   ├── MerchantAuthController.java   # 新增：商家信息/密码修改
  │   │   └── MerchantDashboardController.java # 新增：数据概览
  │   ├── domain/
  │   │   └── MerchantUser.java             # 新增：商家用户实体
  │   ├── mapper/
  │   │   └── MerchantUserMapper.java       # 新增
  │   └── service/
  │       └── MerchantUserServiceImpl.java  # 新增

share-modules/share-goods/
  └── src/main/java/com/share/goods/controller/
      └── MerchantProductController.java    # 新增：商家商品管理

share-modules/share-order/
  └── src/main/java/com/share/order/controller/
      ├── MerchantOrderController.java      # 新增：商家订单管理
      └── MerchantAfterSaleController.java  # 新增：商家售后审核

# 前端变更
share-ui/
  └── src/
      ├── api/
      │   └── merchant/                     # 新增目录
      │       ├── product.js
      │       ├── order.js
      │       ├── afterSale.js
      │       └── dashboard.js
      ├── views/
      │   └── merchant/                     # 新增目录
      │       ├── dashboard/index.vue       # 控制台首页
      │       ├── product/
      │       │   ├── index.vue             # 商品列表
      │       │   └── edit.vue              # 商品编辑(新增/修改)
      │       ├── order/
      │       │   ├── index.vue             # 订单列表
      │       │   └── detail.vue            # 订单详情
      │       ├── afterSale/
      │       │   └── index.vue             # 售后列表
      │       └── setting/
      │           └── index.vue             # 店铺设置
      └── router/
          └── merchant.js                   # 新增：商家路由配置
```

## Implementation Phases

### Phase 1 — 基础架构（商家登录 + 数据隔离）

| 任务 | 涉及模块 | 工作量 |
|------|----------|--------|
| merchant_user 表 + 实体 + Mapper | share-merchant | 小 |
| MerchantTokenController（登录/登出/获取信息） | share-auth | 中 |
| 网关白名单添加商家登录路径 | share-gateway | 小 |
| MerchantInfo 增加 userId 字段 | share-merchant | 小 |
| 商家 token 解析 + merchantId 上下文 | share-common-security | 小 |
| 商家登录前端页面 | share-ui | 中 |

### Phase 2 — 商品管理

| 任务 | 涉及模块 | 工作量 |
|------|----------|--------|
| MerchantProductController（列表/新增/编辑/上下架/SKU管理） | share-goods | 中 |
| 商品管理前端页面（列表 + 编辑表单 + SKU 管理） | share-ui | 大 |
| Service 层增加 merchant_id 过滤 | share-goods | 小 |

### Phase 3 — 订单管理

| 任务 | 涉及模块 | 工作量 |
|------|----------|--------|
| MerchantOrderController（列表/详情/发货） | share-order | 中 |
| 订单列表前端页面（状态筛选/发货弹窗） | share-ui | 中 |

### Phase 4 — 售后审核

| 任务 | 涉及模块 | 工作量 |
|------|----------|--------|
| MerchantAfterSaleController（列表/审核同意/拒绝） | share-order | 小 |
| 售后列表前端页面（审核操作按钮） | share-ui | 中 |

### Phase 5 — 店铺设置 + 数据概览

| 任务 | 涉及模块 | 工作量 |
|------|----------|--------|
| MerchantDashboardController（统计查询） | share-merchant | 中 |
| 控制台首页 + 店铺设置页面 | share-ui | 中 |
| 菜单权限配置（sys_menu） | share-system | 小 |
| 商家角色（sys_role）创建 | share-system | 小 |

## Architecture Decisions

### 1. 商家登录流程

```
商家登录页 → POST /auth/merchant/login
  → 校验 merchant_user 表账号密码
  → 校验 MerchantInfo.status == "1"（已启用）
  → 生成 JWT token，payload 包含: userId + merchantId + role="merchant"
  → 返回 token + 商家基本信息

后续请求：
  前端在 Header 携带 token
  网关/auth 拦截校验 token 有效性
  商家 Controller 通过 SecurityUtils.getMerchantId() 获取当前商家 ID
  所有查询自动增加 merchant_id/supplier_id = 当前商家 ID 过滤
```

### 2. 数据隔离策略

不使用 RuoYi DataScope（那是为组织架构设计的）。改用显式 merchantId 过滤：

```java
// MerchantProductController
@GetMapping("/list")
public TableDataInfo list(Product product) {
    startPage();
    Long merchantId = SecurityUtils.getMerchantId();
    product.setMerchantId(merchantId);  // 强制设置商家ID
    List<Product> list = productService.selectProductList(product);
    return getDataTable(list);
}
```

### 3. 前端路由策略

商家登录后，前端通过 `/system/menu/getRouters` 获取商家角色菜单。在 sys_menu 中配置商家菜单：
- 父菜单：`商家管理`（仅 merchant 角色可见）
- 子菜单：商品管理、订单管理、售后管理、店铺设置、控制台

前端 views/merchant/ 下的页面通过 `import.meta.glob` 自动发现，无需手动注册。

## Complexity Tracking

无 — 符合项目现有架构模式，未引入新的架构复杂度。
