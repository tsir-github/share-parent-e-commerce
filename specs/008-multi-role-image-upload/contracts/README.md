# API Contracts: 多角色图片上传能力

**Phase**: 1 — Design & Contracts
**Date**: 2026-06-27

约定：所有上传接口统一走 `multipart/form-data`，返回图片可访问URL。

---

## 1. share-file — 通用文件上传

> 已有接口，本功能直接复用。

### POST /upload

上传单张图片。

**Request**:
```
Content-Type: multipart/form-data

file: (MultipartFile)
```

**Response**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "name": "2026/06/27/abc123.jpg",
    "url": "http://192.168.10.129:9000/spzx/2026/06/27/abc123.jpg"
  }
}
```

---

## 2. share-user — C端头像上传

### POST /api/v1/user/avatar

C端业主更换头像。

**Auth**: `@RequiresLogin`

**Request**:
```
Content-Type: multipart/form-data

file: (MultipartFile, 单张, ≤10MB, 图片格式)
```

**Response**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "avatarUrl": "http://192.168.10.129:9000/spzx/avatars/xxx.jpg"
  }
}
```

**逻辑**: 接收图片 → 调 share-file 上传 → 更新 `user_info.avatar_url` → 返回新URL

---

## 3. share-merchant — 店铺Logo上传

### POST /api/v1/merchant/profile/logo

商家更新店铺Logo。

**Auth**: `@RequiresLogin`

**Request**:
```
Content-Type: multipart/form-data

file: (MultipartFile, 单张, ≤10MB, 图片格式)
```

**Response**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "logoUrl": "http://192.168.10.129:9000/spzx/logos/xxx.jpg"
  }
}
```

**逻辑**: 接收图片 → 调 share-file 上传 → 更新 `merchant_info.logo` → 返回新URL

---

## 4. share-goods — 商品图片上传

### POST /api/v1/merchant/product/{productId}/images

商家为商品上传多张图片（主图由前端指定第一张或传 `type=main` 参数）。

**Auth**: `@RequiresLogin`

**Request**:
```
Content-Type: multipart/form-data

files: (MultipartFile[], 最多9张, 每张≤10MB)
type: (String, 可选, "main" 表示主图，不传则表示详情图)
```

**Response**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "mainImage": "http://...",
    "images": [
      {"url": "http://...", "sortOrder": 0},
      {"url": "http://...", "sortOrder": 1}
    ]
  }
}
```

**逻辑**: 接收图片 → 逐张调 share-file 上传 → 若 type=main 更新 `product.main_image` → 其余插入 `product_image` 表

---

## 5. share-order — 评价晒图

### POST /api/v1/order/review/{orderItemId}/images

C端业主在评价订单时上传晒图。

**Auth**: `@RequiresLogin`

**Request**:
```
Content-Type: multipart/form-data

files: (MultipartFile[], 最多9张, 每张≤10MB)
```

**Response**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "images": [
      {"url": "http://...", "sortOrder": 0},
      {"url": "http://...", "sortOrder": 1}
    ]
  }
}
```

**逻辑**: 接收图片 → 逐张调 share-file 上传 → 插入 `review_image` 表 → 返回URL列表
