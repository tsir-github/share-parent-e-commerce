# Data Model: 商家端 Web 后台

## 新增表

### merchant_user（商家登录用户表）

```sql
CREATE TABLE IF NOT EXISTS `merchant_user` (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `merchant_id` bigint(20)   NOT NULL                  COMMENT '关联商家ID',
  `username`    varchar(64)  NOT NULL                  COMMENT '登录账号',
  `password`    varchar(256) NOT NULL                  COMMENT '密码（BCrypt加密）',
  `phone`       varchar(20)  DEFAULT NULL              COMMENT '手机号',
  `email`       varchar(50)  DEFAULT NULL              COMMENT '邮箱',
  `status`      char(1)     NOT NULL DEFAULT '0'       COMMENT '状态 0正常 1停用',
  `login_ip`    varchar(128) DEFAULT NULL              COMMENT '最后登录IP',
  `login_date`  datetime     DEFAULT NULL              COMMENT '最后登录时间',
  `del_flag`    char(1)     NOT NULL DEFAULT '0',
  `create_by`   varchar(64)  DEFAULT '',
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT '',
  `update_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家登录用户';
```

## 变更表

### merchant_info — 新增字段

```sql
ALTER TABLE `merchant_info`
  ADD COLUMN `user_id` bigint(20) DEFAULT NULL COMMENT '关联商家用户ID' AFTER `id`;
```

### order_info — supplierId 即为商家ID，无变更

OrderInfo 现有 `supplier_id` 字段即商家 ID，查询时直接使用此字段过滤。

## 现有相关表状态

| 表 | 已有字段 | 说明 |
|---|---|---|
| `product` | merchant_id | ✅ 已存在，直接使用 |
| `product_sku` | product_id | ✅ 通过 product.merchant_id 关联商家 |
| `order_info` | supplier_id | ✅ 即商家 ID（命名不同） |
| `after_sale_request` | merchant_id | ✅ 已存在 |
| `merchant_info` | 缺 user_id | 🆕 需新增 |
