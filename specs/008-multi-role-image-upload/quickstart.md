# Quickstart: 多角色图片上传能力

**Phase**: 1 — Design & Contracts
**Date**: 2026-06-27

## 前置条件

- 所有微服务已启动（至少: share-file, share-auth, share-gateway）
- 需要测试的模块也已启动（share-user / share-merchant / share-goods / share-order）
- MinIO 可访问 (`http://192.168.10.129:9000`)
- 准备几张测试图片（.jpg/.png，建议 <5MB）

## 验证场景

### 场景1: share-file 基础上传

```bash
# 直接用 Postman 或 curl 调用 share-file 上传
curl -X POST http://localhost:9300/upload \
  -F "file=@/path/to/test.jpg"
```

**期望**: 返回 200，data.url 可浏览器直接打开查看图片。

---

### 场景2: C端头像上传

```bash
# 先用小程序登录获取 token
# 调用头像上传
curl -X POST http://localhost:8080/user/api/v1/user/avatar \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/avatar.jpg"
```

**期望**: 返回新头像URL。再次获取用户信息时 `avatar_url` 已更新。

---

### 场景3: 商家上传Logo

```bash
# 商家后台登录获取 token
curl -X POST http://localhost:8080/merchant/api/v1/merchant/profile/logo \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/logo.jpg"
```

**期望**: 返回新LogoURL。获取店铺信息时 logo 字段已更新。

---

### 场景4: 商品多图上传

```bash
# 商家登录，已有商品ID=1
curl -X POST "http://localhost:8080/goods/api/v1/merchant/product/1/images" \
  -H "Authorization: Bearer <token>" \
  -F "files=@/path/to/main.jpg" \
  -F "files=@/path/to/detail1.jpg" \
  -F "type=main"
```

**期望**: 返回主图URL和多图列表。查商品详情时 mainImage 和 product_image 表中数据对应。

---

### 场景5: 评价晒图

```bash
# C端业主登录，已有订单项ID=1
curl -X POST "http://localhost:8080/order/api/v1/order/review/1/images" \
  -H "Authorization: Bearer <token>" \
  -F "files=@/path.to/review1.jpg" \
  -F "files=@/path/to/review2.jpg"
```

**期望**: 返回晒图URL列表。查评价详情时 review_image 表中有对应记录。

---

## 失败场景验证

| 测试项 | 操作 | 期望 |
|---|---|---|
| 非图片文件 | 上传 .txt 文件 | 返回 400，提示"只允许图片格式" |
| 超大文件 | 上传 >10MB 文件 | 返回 400，提示"图片大小不能超过10MB" |
| 未登录 | 不带 token 上传 | 返回 401 未认证 |
| 空文件 | 上传 0 字节文件 | 返回 400，提示"文件不能为空" |
