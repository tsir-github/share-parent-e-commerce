# Research: 微信小程序登录注册完善

**Phase**: 0 — Research
**Date**: 2026-06-27

## Decisions

### 1. 登录注册模式

**Decision**: 静默登录 + 自动注册（wx.login → openid exchange → create/find user → return token + isNewUser flag）
**Rationale**: 微信小程序标准登录流程，无需手机号注册。openid 是唯一标识。
**Alternatives**: 手机号注册 → 不需要，增加了复杂度且用户体验差

### 2. 新用户标识

**Decision**: wxLogin 返回体中增加 `isNewUser` 字段
**Rationale**: 前端据此判断是否弹窗引导用户授权头像昵称。当前 wxLogin 返回 `R<UserInfo>`，需改为返回 DTO 或 Map 包含 token + userInfo + isNewUser。

### 3. 手机号绑定

**Decision**: 仅微信快速授权（`getPhoneNumber`），不做短信验证码
**Rationale**: 微信授权获取手机号零成本、用户无需手动输入，适合 C端场景

### 4. 无新增数据库变更

**Decision**: 不新建表，不修改表结构
**Rationale**: `user_info` 表已有 `nickname`、`avatar_url`、`phone` 字段，功能完全复用
