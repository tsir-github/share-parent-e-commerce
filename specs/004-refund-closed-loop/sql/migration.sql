-- =============================================
-- 退款闭环数据库迁移脚本
-- 适用: share-order 库 / share-payment 库
-- =============================================

-- 1. share-order 库: 新建售后申请表
-- DROP TABLE IF EXISTS `after_sale_request`;
CREATE TABLE IF NOT EXISTS `after_sale_request` (
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `order_no`      varchar(32)  NOT NULL                  COMMENT '订单号',
  `user_id`       bigint(20)   NOT NULL                  COMMENT '用户ID',
  `merchant_id`   bigint(20)   DEFAULT NULL              COMMENT '商家ID',
  `refund_amount` decimal(10,2) NOT NULL DEFAULT '0.00'  COMMENT '退款金额',
  `refund_reason` varchar(500) DEFAULT NULL              COMMENT '退款原因',
  `audit_status`  char(1)     NOT NULL DEFAULT '0'       COMMENT '审核状态: 0-待审核 1-商家同意 2-商家拒绝 3-客服介入 4-客服同意退款 5-客服拒绝',
  `audit_remark`  varchar(500) DEFAULT NULL              COMMENT '审核备注',
  `audit_time`    datetime     DEFAULT NULL              COMMENT '审核时间',
  `audit_by`      varchar(64)  DEFAULT NULL              COMMENT '审核人',
  `close_time`    datetime     DEFAULT NULL              COMMENT '关闭时间',
  `close_type`    char(1)     DEFAULT NULL               COMMENT '关闭类型',
  `ext_json`      varchar(1000) DEFAULT NULL             COMMENT '扩展字段JSON',
  `del_flag`      char(1)     NOT NULL DEFAULT '0'       COMMENT '删除标志（0存在 2删除）',
  `create_by`     varchar(64)  DEFAULT ''                COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL              COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT ''                COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL              COMMENT '更新时间',
  `remark`        varchar(255) DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后申请表';

-- 2. share-order 库: order_info 表新增退款字段
ALTER TABLE `order_info`
  ADD COLUMN `refund_amount`  decimal(10,2) DEFAULT '0.00' COMMENT '已退款金额累计' AFTER `pay_amount`,
  ADD COLUMN `refund_time`   datetime       DEFAULT NULL    COMMENT '最近退款时间' AFTER `refund_amount`,
  ADD COLUMN `refund_count`  int(11)        DEFAULT '0'     COMMENT '退款次数' AFTER `refund_time`;

-- 3. share-payment 库: payment_info 表新增退款字段
ALTER TABLE `payment_info`
  ADD COLUMN `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '已退款金额' AFTER `amount`,
  ADD COLUMN `refund_time`   datetime      DEFAULT NULL COMMENT '最近退款时间' AFTER `refund_amount`;
