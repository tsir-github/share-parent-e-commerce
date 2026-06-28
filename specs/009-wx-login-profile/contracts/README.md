# API Contracts: 微信小程序登录注册完善

**Phase**: 1 — Design

## 1. 修改：wxLogin（登录/注册）

### POST /api/v1/user/wxLogin/{code}

**Auth**: 公开

**Response**:
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGci...",
    "userInfo": {
      "id": 1,
      "nickname": "张三",
      "avatarUrl": "http://...",
      "phone": null
    },
    "isNewUser": true
  }
}
```

**改动**: 返回体从 `R<UserInfo>` 变为 `R<WxLoginResultDTO>`，新增 `token` 和 `isNewUser` 字段。

---

## 2. 新增：更新用户昵称（UserProfileController）

### PUT /api/v1/user/profile/nickname

**Auth**: `@RequiresLogin`

**Request**:
```json
{
  "nickname": "新昵称"
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

**校验**: 昵称长度 1-30 个字符

---

## 3. 新增：获取当前用户信息（UserProfileController）

### GET /api/v1/user/profile

**Auth**: `@RequiresLogin`

**Response**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "nickname": "张三",
    "avatarUrl": "http://...",
    "phone": "138****1234"
  }
}
```

---

## 4. 已有：上传头像（UserAvatarController）

### POST /api/v1/user/avatar

**Auth**: `@RequiresLogin`

**Request**: `multipart/form-data`，字段 `file`（图片，≤10MB，仅图片格式）

**Response**:
```json
{
  "code": 200,
  "data": {
    "avatarUrl": "http://..."
  }
}
```

> 此接口已在先前的多角色图片上传功能中实现，无需改动。

---

## 5. 可选：绑定手机号（后续迭代，本 spec 不做）

### POST /api/v1/user/profile/phone

**Auth**: `@RequiresLogin`

**Request**:
```json
{
  "encryptedData": "...",
  "iv": "..."
}
```

后端使用微信小程序的 session_key 解密 `encryptedData` 获取手机号并保存。
