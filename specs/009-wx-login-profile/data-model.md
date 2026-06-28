# Data Model: 微信小程序登录注册完善

**Phase**: 1 — Design
**Date**: 2026-06-27

## 实体

无新增数据库表。仅在已有 `share-user.user_info` 表上操作已有字段。

### UserInfo — 已有字段

| 字段 | 当前值（新用户） | 改动后 |
|---|---|---|
| `nickname` | `System.currentTimeMillis()`（毫秒时间戳） | 用户授权后从微信获取的真实昵称 |
| `avatar_url` | `""`（空字符串） | 用户授权后从微信获取头像 URL 或上传自定义头像 |
| `phone` | `null` | 可选绑定，通过微信 `getPhoneNumber` 授权获取 |

## wxLogin 返回体

当前返回 `R<UserInfo>`。改为返回包含以下字段的结构：

| 字段 | 说明 |
|---|---|
| `token` | JWT 登录令牌 |
| `userInfo` | 用户基本信息（含 id, nickname, avatarUrl, phone） |
| `isNewUser` | 是否首次注册（boolean） |
