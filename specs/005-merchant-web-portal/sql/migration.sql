-- ============================================================
-- 商家端 Web 后台 — 数据库迁移
-- 目标库: share-merchant
-- ============================================================

-- 1. 新建 merchant_user 表（商家登录用户）
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
  `del_flag`    char(1)     NOT NULL DEFAULT '0'       COMMENT '逻辑删除 0正常 2删除',
  `create_by`   varchar(64)  DEFAULT ''                COMMENT '创建者',
  `create_time` datetime     DEFAULT NULL              COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT ''                COMMENT '更新者',
  `update_time` datetime     DEFAULT NULL              COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家登录用户';

-- 2. merchant_info 增加 user_id 字段（关联商家登录账号）
ALTER TABLE `merchant_info`
  ADD COLUMN `user_id` bigint(20) DEFAULT NULL COMMENT '关联商家用户ID' AFTER `id`;
