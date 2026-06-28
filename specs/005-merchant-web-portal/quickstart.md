# Quickstart: 商家端 Web 后台

## 前置条件

- MySQL 本地 + VM 同步迁移
- share-auth / share-goods / share-order / share-merchant 微服务运行
- share-ui 前端 dev 模式

## 验证步骤

### 1. 数据库迁移

```bash
# 本地 MySQL
mysql -u root -proot -h 127.0.0.1 --database="share-merchant" < specs/005-merchant-web-portal/sql/migration.sql

# VM MySQL
ssh tonyu@192.168.10.129 "mysql -u root -proot share-merchant < /tmp/migration.sql"
```

### 2. 商家账号创建

管理员后台 → 商家管理 → 新增商家 → 系统自动创建 merchant_user 登录账号

### 3. 商家登录验证

1. 访问 `/merchant/login` → 输入账号密码
2. 验证通过后跳转控制台首页
3. 未审核商家登录 → 提示"店铺未审核通过"

### 4. 商品管理验证

1. 新增商品（含 SKU）→ 提交 → 列表显示
2. 上架/下架 → 前端状态变更
3. 编辑商品 → 保存成功

### 5. 订单管理验证

1. 创建测试订单（API 或小程序）→ 商家端看到待发货订单
2. 发货 → 录入配送员 → 订单状态变为配送中

### 6. 售后审核验证

1. 用户提交售后申请 → 商家端看到申请
2. 同意退款 → 系统自动退款
3. 拒绝退款 → 流转到客服处理

### 7. 数据隔离验证

1. 商家 A 登录 → 只能看到自己的商品/订单
2. 商家 B 登录 → 只能看到自己的商品/订单
