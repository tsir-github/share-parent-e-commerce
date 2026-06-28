# Quickstart: 微信小程序登录注册完善

**Phase**: 1 — Design

## 前置条件

- share-user 已启动
- 微信小程序 appId 和 secret 已配置（Nacos）

## 验证场景

### 场景1：新用户首次登录

```bash
# 模拟微信小程序返回的 code
curl -X POST http://localhost:8080/user/api/v1/user/wxLogin/test-code-123
```

**期望**: 返回 isNewUser=true，token 有效，userInfo 中 nickname 为时间戳。

### 场景2：老用户再次登录

```bash
# 同一个 code 第二次调用
curl -X POST http://localhost:8080/user/api/v1/user/wxLogin/test-code-123
```

**期望**: 返回 isNewUser=false，token 有效。

### 场景3：更新昵称

```bash
# 用登录返回的 token
curl -X PUT http://localhost:8080/user/api/v1/user/profile/nickname \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"测试昵称"}'
```

**期望**: 返回操作成功，再次获取用户信息时 nickname 已更新。

### 场景4：获取当前用户信息

```bash
curl -X GET http://localhost:8080/user/api/v1/user/profile \
  -H "Authorization: Bearer <token>"
```

**期望**: 返回 id, nickname, avatarUrl, phone。

### 场景5：昵称校验

```bash
# 空昵称
curl -X PUT ... -d '{"nickname":""}' → 400 提示"昵称不能为空"

# 超长昵称
curl -X PUT ... -d '{"nickname":"超过三十个字的超长昵称测试一二三四五六七八九十十一十二十三十四"}' → 400 提示"昵称长度不能超过30个字符"
```
