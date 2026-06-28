# 验证指南：秒杀活动管理后台

## 前置条件

- [ ] 项目已完整导入 IDEA，share-goods 模块可正常启动
- [ ] Nacos 配置中心正常，`share-goods-dev.yml` 已加载
- [ ] MySQL share-goods 数据库 `seckill_activity` 表已按 data-model.md 改造
- [ ] 管理员账号拥有 `goods:seckill:xxx` 相关权限

## 验证场景

### 场景 1：创建秒杀活动

```bash
# 新增活动
curl -X POST http://localhost:8080/seckillActivity \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "name": "测试秒杀-1元购",
    "productId": 100,
    "skuId": 200,
    "seckillPrice": 1.00,
    "stock": 50,
    "limitPerUser": 1,
    "startTime": "2026-06-28 12:00:00",
    "endTime": "2026-06-30 12:00:00"
  }'

# 预期返回: {"code": 0, "msg": "操作成功"}
```

### 场景 2：查询活动列表

```bash
curl -s "http://localhost:8080/seckillActivity/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <admin-token>"

# 预期返回: TableDataInfo 格式，包含刚创建的活动
```

### 场景 3：启用/禁用活动

```bash
# 启用活动（id=1）
curl -X PUT http://localhost:8080/seckillActivity/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{"id": 1, "status": "1"}'

# 预期返回: {"code": 0, "msg": "操作成功"}
# 之后 GET /seckillActivity/list 看到 status 变为 "1"
```

### 场景 4：参数校验

```bash
# 秒杀价 = 0（应拒绝）
curl -X POST http://localhost:8080/seckillActivity \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "name": "测试",
    "productId": 100,
    "skuId": 200,
    "seckillPrice": 0,
    "stock": 10
  }'

# 预期返回: {"code": 1, "msg": "秒杀价必须大于0"}
```

### 场景 5：时间冲突检测

```bash
# 为同一 SKU 创建第二个时间重叠的活动
curl -X POST http://localhost:8080/seckillActivity \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "name": "冲突测试",
    "productId": 100,
    "skuId": 200,
    "seckillPrice": 1.00,
    "stock": 10,
    "startTime": "2026-06-28 12:00:00",
    "endTime": "2026-06-30 12:00:00"
  }'

# 预期返回: {"code": 1, "msg": "该SKU在所选时间范围内已有进行中的活动"}
```

## 编译检查

```bash
# 在项目根目录执行
mvn compile -pl share-modules/share-goods -am -q
# 预期: 无错误输出（静默成功）
```

## 接口清单速查

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/seckillActivity/list` | `goods:seckill:list` | 分页列表 |
| GET | `/seckillActivity/{id}` | `goods:seckill:query` | 详情 |
| POST | `/seckillActivity` | `goods:seckill:add` | 新增 |
| PUT | `/seckillActivity` | `goods:seckill:edit` | 修改 |
| DELETE | `/seckillActivity/{ids}` | `goods:seckill:remove` | 删除 |
| PUT | `/seckillActivity/status` | `goods:seckill:edit` | 启用/禁用 |
| GET | `/seckillActivity/product/list` | `goods:seckill:add` | 商品列表 |
| GET | `/seckillActivity/stats/{id}` | `goods:seckill:list` | 统计 |

## 相关文件

- [API 契约](contracts/api.md)
- [数据模型](data-model.md)
- [功能规格](spec.md)
