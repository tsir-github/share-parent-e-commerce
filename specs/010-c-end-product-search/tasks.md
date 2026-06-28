# Tasks: C端商品搜索功能

**Input**: Design documents from `specs/010-c-end-product-search/`

## Phase 1: Setup

- [x] T001 确认 ProductMapper.xml 路径和 mybatis-plus 配置

## Phase 2: User Story 1+2+3 — 商品搜索（P1）🎯 MVP

**Goal**: 支持关键词搜索 + 价格筛选 + 排序 + 分类组合

**Independent Test**: 调 `GET /api/v1/product/search?keyword=牛奶&categoryId=1&minPrice=10&sortBy=sales&pageNum=1&pageSize=20` 返回正确结果

### Implementation

- [x] T002 [P] [US1] IProductService 新增 `searchProducts()` 方法声明在 `share-modules/share-goods/.../service/IProductService.java`
- [x] T003 [P] [US1] ProductMapper 新增 `searchProducts()` 方法声明在 `share-modules/share-goods/.../mapper/ProductMapper.java`
- [x] T004 [US1] ProductMapper.xml 新增搜索 SQL（LIKE name/subtitle + 筛选 + 排序 + PageHelper 分页）在 `share-modules/share-goods/.../mapper/ProductMapper.xml`
- [x] T005 [US1] ProductServiceImpl 实现 `searchProducts()` 在 `share-modules/share-goods/.../service/impl/ProductServiceImpl.java`
- [x] T006 [US1] ProductApiController 新增 `GET /search` 端点在 `share-modules/share-goods/.../controller/ProductApiController.java`

## Phase 3: Polish

- [x] T007 编译 share-goods + 按 quickstart.md 验证各场景
- [ ] T008 空关键词、无结果、分页边界测试

---

## Notes

- 只改 share-goods 一个模块
- 无新增表/字段/依赖
- 搜索接口公开，无需登录
