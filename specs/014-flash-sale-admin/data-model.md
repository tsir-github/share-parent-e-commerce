# 数据模型：秒杀活动管理后台

## 现有表改造：`seckill_activity`

### DDL

```sql
-- 秒杀活动表（改造后）
CREATE TABLE `seckill_activity` (
  `id`             bigint          NOT NULL AUTO_INCREMENT  COMMENT '活动ID',
  `name`           varchar(100)    NOT NULL                  COMMENT '活动名称',
  `merchant_id`    bigint          NOT NULL DEFAULT 1        COMMENT '商家ID',
  `product_id`     bigint          NOT NULL                  COMMENT '商品ID（关联 product）',
  `sku_id`         bigint          NOT NULL                  COMMENT 'SKU ID（关联 product_sku）',
  `seckill_price`  decimal(10,2)   NOT NULL                  COMMENT '秒杀价',
  `stock`          int             NOT NULL DEFAULT 0        COMMENT '秒杀库存',
  `limit_per_user` int             NOT NULL DEFAULT 1        COMMENT '每人限购数量',
  `sort`           int             NOT NULL DEFAULT 0        COMMENT '排序权重（越大越靠前）',
  `start_time`     datetime        NOT NULL                  COMMENT '活动开始时间',
  `end_time`       datetime        NOT NULL                  COMMENT '活动结束时间',
  `status`         char(1)         NOT NULL DEFAULT '0'      COMMENT '状态（0-未开始 1-进行中 2-已结束 3-已禁用）',
  `version`        int             NOT NULL DEFAULT 0        COMMENT '乐观锁版本号',
  `create_by`      varchar(64)     DEFAULT ''                COMMENT '创建者',
  `create_time`    datetime        DEFAULT NULL              COMMENT '创建时间',
  `update_by`      varchar(64)     DEFAULT ''                COMMENT '更新者',
  `update_time`    datetime        DEFAULT NULL              COMMENT '更新时间',
  `del_flag`       char(1)         DEFAULT '0'               COMMENT '删除标志（0-正常 2-删除）',
  `remark`         varchar(255)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';
```

### 与现有表的差异

| 字段 | 原表 | 改造后 |
|------|------|--------|
| `limit_per_user` | ❌ 缺失 | ✅ int NOT NULL DEFAULT 1 |
| `merchant_id` | ❌ 缺失 | ✅ bigint NOT NULL DEFAULT 1 |
| `sort` | ❌ 缺失 | ✅ int NOT NULL DEFAULT 0 |
| `version` | ❌ 缺失 | ✅ int NOT NULL DEFAULT 0 |
| `status` | `tinyint DEFAULT 0` | `char(1) NOT NULL DEFAULT '0'` |
| `create_by/update_by/update_time/del_flag/remark` | ❌ 缺失 | ✅ 全部补齐 |
| 索引 | 仅主键 | ✅ 5 个辅助索引 |
| `stock` | `int DEFAULT 0` | `int NOT NULL DEFAULT 0` |

### 状态定义

| 值 | 含义 | 说明 |
|----|------|------|
| `0` | 未开始 | 已创建但未到 start_time，C 端可见但不可抢购 |
| `1` | 进行中 | `status='1'` 且 `start_time <= now <= end_time` |
| `2` | 已结束 | `end_time < now` 自动视为已结束 |
| `3` | 已禁用 | 管理员手动禁用，C 端不可见 |

## 实体：`SeckillActivity.java`

**位置**: `share-api/share-api-goods/src/main/java/com/share/goods/domain/SeckillActivity.java`

继承 `BaseEntity`（提供 `createBy/updateBy/createTime/updateTime/delFlag/remark`）。

```java
@Data
@TableName("seckill_activity")
@Schema(description = "秒杀活动")
public class SeckillActivity extends BaseEntity {
    private Long id;
    private String name;
    private Long merchantId;
    private Long productId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer limitPerUser;
    private Integer sort;
    private Date startTime;
    private Date endTime;
    private String status;
    private Integer version;

    // 非数据库字段：关联查询
    @TableField(exist = false)
    private String productName;     // 商品名称
    @TableField(exist = false)
    private String skuSpecs;        // SKU 规格 JSON
    @TableField(exist = false)
    private String mainImage;       // 商品主图
}
```

## 实体间关系

```
SeckillActivity (N) ──→ (1) Product      (通过 product_id)
SeckillActivity (N) ──→ (1) ProductSku   (通过 sku_id)
SeckillActivity (N) ──→ (1) Merchant     (通过 merchant_id，从 product 表推导)
```

一个活动针对**一个 SKU**，不支持多 SKU 秒杀（简化 MVP 设计）。
