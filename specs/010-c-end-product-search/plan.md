# Implementation Plan: C端商品搜索功能

**Branch**: `010-c-end-product-search` | **Date**: 2026-06-27 | **Spec**: [spec.md](spec.md)

## Summary

在现有 `ProductApiController` 基础上增加搜索端点。MySQL LIKE 匹配名称+副标题，支持价格区间、排序、分类组合。

## Technical Context

**Language/Version**: Java 21, Spring Boot 2.7

**Primary Dependencies**: MyBatis-Plus, PageHelper

**Storage**: MySQL (share-goods.product)，LIKE 查询，不上 ES

**Testing**: Postman 联调

**Target Platform**: 后端微服务 + 微信小程序

**Project Type**: 微服务后端，在 share-goods 模块内新增接口

**Performance Goals**: 搜索 <2s，筛选排序 <1s

**Constraints**: 无新增依赖，不改表结构

**Scale/Scope**: 小区级，商品量 1000 以内

## Constitution Check

宪法为模板无实质性约束。 ✅ PASS

## Project Structure

```text
share-modules/share-goods/
├── controller/ProductApiController.java   # 修改：新增搜索端点
├── mapper/ProductMapper.java              # 修改：新增搜索方法
├── mapper/ProductMapper.xml               # 修改：新增搜索 SQL
├── service/IProductService.java           # 修改：新增搜索方法
└── service/impl/ProductServiceImpl.java   # 修改：实现搜索
```

## Complexity Tracking

无违规项。
