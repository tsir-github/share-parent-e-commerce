# Research: 多角色图片上传能力

**Phase**: 0 — Outline & Research
**Date**: 2026-06-27

## Summary

无 NEEDS CLARIFICATION 项。所有技术上下文在现有项目结构中有明确依据。

## Key Decisions

### 1. 文件存储方案

**Decision**: 复用现有 share-file 服务的 MinIO 存储
**Rationale**: share-file 已有 MinIO 配置且运行中，`POST /upload` 接口可用
**Relevant**: MinIO url=http://192.168.10.129:9000, bucket=spzx

### 2. 业务模块不直接操作文件存储

**Decision**: 各模块通过 Feign 调用 share-file 上传，获取 URL 后存业务字段
**Rationale**: 遵循现有编码规范，避免跨模块直接调数据库，保持统一上传入口

### 3. 多文件上传

**Decision**: 单次请求上传单张，多张由前端循环调用（或扩展现有接口支持多文件）
**Rationale**: share-file 当前 `POST /upload` 接收单文件。多文件可通过前端多次调用或后端扩展 `@RequestParam("files") MultipartFile[]` 实现
