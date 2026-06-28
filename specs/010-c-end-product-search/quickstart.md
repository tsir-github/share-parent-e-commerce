# Quickstart: C端商品搜索功能

## 前提条件
- share-goods 已启动
- 数据库中有已上架商品数据

## 验证场景

### 场景1：关键词搜索
```
curl "http://localhost:8080/goods/api/v1/product/search?keyword=牛奶"
```
期望：返回名称或副标题包含"牛奶"的上架商品列表。

### 场景2：关键词为空
```
curl "http://localhost:8080/goods/api/v1/product/search"
```
期望：返回全部上架商品。

### 场景3：分类+搜索组合
```
curl "http://localhost:8080/goods/api/v1/product/search?keyword=可乐&categoryId=1"
```
期望：仅返回分类ID=1且名称含"可乐"的商品。

### 场景4：价格区间 + 排序
```
curl "http://localhost:8080/goods/api/v1/product/search?keyword=牛奶&minPrice=10&maxPrice=100&sortBy=sales"
```
期望：价格10-100元之间的牛奶，按销量从高到低排列。

### 场景5：分页
```
curl "http://localhost:8080/goods/api/v1/product/search?keyword=牛奶&pageNum=1&pageSize=5"
```
期望：返回第1页，每页5条，含 total 总数。
