# share-parent 项目上下文

## 项目简介

小区电商平台，基于 RuoYi-Cloud v3.6.3 二次开发（原共享充电宝项目改造）。

## 技术栈

- **后端**：Spring Boot 2.7 + Spring Cloud Alibaba + MyBatis-Plus
- **前端**：Vue 3 + Element Plus（Vite 构建）
- **注册/配置中心**：Nacos 2.x
- **网关**：Spring Cloud Gateway
- **数据库**：MySQL + Redis（Redisson 分布式锁）+ RocketMQ
- **支付**：微信支付 v3
- **部署**：nginx 反代 + CentOS 7 VM

## 模块结构

| 模块 | 端口 | 功能 |
|---|---|---|
| share-ui | 80 (nginx) | 前端页面 |
| share-gateway | 8080 | Spring Cloud 网关 |
| share-auth | 9200 | 认证中心（JWT + Redis） |
| share-system | 9201 | 后台管理（RuoYi 框架） |
| share-gen | 9202 | 代码生成 |
| share-job | 9203 | 定时任务 |
| share-file | 9300 | 文件服务 |
| share-goods | 9210 | ✅ 已构建 |
| share-order | 9211 | ✅ 已构建 |
| share-payment | 9213 | 🟡 骨架 |
| share-coupon | 9214 | ✅ 已构建 |
| share-user | 9209 | ✅ 已构建（微信小程序） |
| share-merchant | 9215 | ✅ 已构建 |
| share-api-goods | — | ✅ Feign 接口 |
| share-api-order | — | ✅ Feign 接口 |
| share-api-payment | — | ✅ Feign 接口 |
| share-api-coupon | — | ✅ Feign 接口 |
| share-api-user | — | ✅ Feign 接口 |
| share-api-merchant | — | ✅ Feign 接口 |

## 数据库（VM MySQL 5.7.38，192.168.10.129:3306）

| 库名 | 用途 |
|---|---|
| share-system | 后台管理（RuoYi 自带，19 张表） |
| share-goods | 商品 SPU、SKU、分类、区域 |
| share-user | C 端用户 + 微信小程序 |
| share-order | 订单 + RocketMQ |
| share-payment | 微信支付 v3 |
| share-coupon | 优惠券 |
| share-merchant | 商家信息 + 商家用户 |

**账号密码**：root / root

## 中间件（VM 192.168.10.129）

| 服务 | 端口 | 说明 |
|---|---|---|
| Nacos | 8848 | 命名空间: `a746e297-417e-4aec-bfb1-e42df33fbe93` |
| MySQL | 3306 | 5.7.38 |
| Redis (master) | 6379 | Docker，随 Docker 自启 |
| Redis (replica) | 6380 | tyx 用户，编译自源码 |
| Redis (replica) | 6381 | tyx 用户，编译自源码 |
| Redis Sentinel | 26379/26380/26381 | monitor mymaster quorum=2 |
| RocketMQ | 9876/10911/10912 | tonyu 用户，4.9.7，ASYNC_MASTER |

### Redis Sentinel

```
路径: /home/tyx/redis-7.2.5/          (编译的 Redis 7.2.5 二进制)
路径: /home/tyx/redis-sentinel/       (配置、数据、日志、管理脚本)
管理: bash ~/redis-sentinel/manage.sh {start|stop|status}
密码: ShareRedis2024!                 (requirepass + masterauth)
```

- 6379 无密码持久化，重启后 manage.sh 自动 `CONFIG SET`
- 6380/6381 已持久化密码
- Sentinel quorum=2，down-milliseconds=5000，failover-timeout=10000
- 6380/6381 配置了 `replica-announce-ip 192.168.10.129`
- 哨兵通过 systemd 自启：`redis-replica-sentinel.service`，User=tyx

### RocketMQ

```
路径: /home/tonyu/rocketmq/              (4.9.7 版本)
配置: broker-local.conf（brokerIP1=192.168.10.129，ASYNC_MASTER）
管理: bash ~/rocketmq/manage.sh {start|stop|status}
内存: namesrv 256m / broker 512m（VM 1.8G RAM）
```
- `autoCreateTopicEnable=true`
- 通过 systemd 自启：`rocketmq.service`，User=tonyu

### 各模块 Redis 配置（Nacos dev.yml）

所有模块 Redis 已切换 Sentinel 模式：
```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes: 192.168.10.129:26379,192.168.10.129:26380,192.168.10.129:26381
    password: ShareRedis2024!
```

## 环境与部署

- **VM**：192.168.10.129，tonyu(sudo)/tyx(sudo)
- **sudo 密码**：tonyu=`tongyuxin123T`，tyx=`123456`
- **Windows**：运行 IDEA，微服务在此启动
- **nginx**：VM 上反代前端 + API 到 Windows 网关
- **前端构建**：`npm run build:prod` → 上传到 VM nginx 目录
- **微服务**在 Windows IDEA 直接启动，网关 8080

### nginx

- `location /` → `/usr/share/nginx/html/share-ui/`
- `location /prod-api/` → proxy_pass `http://<Windows IP>:8080/`（去掉 /prod-api 前缀）
- 前端 API 前缀在 `share-ui/.env.production` 配置为 `/prod-api`

### 中间件启动（Seata + Sentinel）

双击 `middleware\start-middleware.bat`：

| 服务 | 地址 | 账号 |
|---|---|---|
| Sentinel Dashboard | `http://localhost:8718` | sentinel / sentinel |
| Seata Console | `http://localhost:7091` | seata / seata |
| Seata TC | 注册到 Nacos，端口 8091 | — |

### Windows WiFi IP 变了怎么办？

**改两处：**
1. `middleware\start-middleware.bat` → `-Dseata.service.bind-ip=新IP`
2. VM nginx → `proxy_pass http://新IP:8080/;` → `sudo systemctl reload nginx`

**不需要改：** Nacos 配置（通过 Nacos 发现 TC）、bootstrap.yml（指向 VM 的 Nacos）

## 编码规范（强制遵守）

### 1. 分层架构

```
Controller（很薄）→ Service 接口 → ServiceImpl（核心）→ Mapper
```

- Controller：接收请求、参数校验、调用 Service、返回 `R<T>`。**禁止写业务逻辑**
- Service 接口：定义方法签名
- ServiceImpl：所有业务逻辑、`@Transactional`、调用 Mapper 和远程服务
- Mapper：继承 `BaseMapper`，复杂 SQL 写 XML

**禁止行为：**
- Controller 里写 if/else 业务判断
- Service 直接调别的 Service 的 Mapper
- Controller 直接调 Mapper
- 跨模块直接调数据库（必须走 Feign）
- try-catch 吞异常

### 2. 配置管理

- 环境差异配置 → **Nacos**（数据源、Redis、MQ、业务参数）
- `bootstrap.yml` 只放：`spring.application.name`、`spring.cloud.nacos` 地址、`spring.profiles.active`
- 敏感信息放 Nacos，禁止硬编码

### 3. API 规范（RESTful）

```
GET    /api/v1/orders          → 分页查询
GET    /api/v1/orders/{id}     → 查询详情
POST   /api/v1/orders          → 新增
PUT    /api/v1/orders/{id}     → 修改
DELETE /api/v1/orders/{id}     → 删除
```

- 接口标注 `@Operation(summary = "xxx")`
- 前端交互接口加 `@RequiresLogin`，模块内部 Feign 加 `@InnerAuth`
- 路径设计遵循 RESTful 语义：资源用复数名词，层级用 `/` 分隔，状态变更用子资源或 PATCH

### 3.5. 返回类型规范（谁用谁）

项目有三种返回类型，**按场景区分，不要混用**：

| 类型 | 适用场景 | 路径特征 | 方法特征 |
|---|---|---|---|
| `R<T>` | **C 端 API / Feign 内部调用 / 新接口** | `/api/v1/**`、`/inner/**` | 必须带泛型 `R<XxxDTO>`，禁止 raw `R` |
| `AjaxResult` | **Ruoyi 管理后台（B 端页面）** | `/list`、`/{id}`（无 /api/v1 前缀） | 继承 `BaseController` 使用 `success()`/`error()`/`toAjax()` |
| `TableDataInfo` | **分页查询** | 任何路径 | 继承 `BaseController` 使用 `getDataTable(list)` |

**选择路径：**
1. 新接口 / C 端接口 → `R<T>` + 不用 `BaseController`
2. 管理后台（Ruoyi 页面）→ `AjaxResult` + `BaseController`
3. Feign 内部接口 → `R<T>` + 不用 `BaseController` + `@InnerAuth`

**禁止行为：**
- 同一 Controller 混用 `AjaxResult` 和 `R<T>`（二选一，不要一锅乱炖）
- `R<T>` 省略泛型（`R` → 必须 `R<Xxx>`）
- 分页接口返回 `R<List<Xxx>>`（必须 `TableDataInfo`）

**框架类速查：**

| 类 | 路径 | 用法 |
|---|---|---|
| `R<T>` | `com.share.common.core.domain.R` | `R.ok(data)` / `R.ok()` / `R.fail(msg)` / `R.fail(code, msg)` |
| `AjaxResult` | `com.share.common.core.web.domain.AjaxResult` | `success(data)` / `error(msg)` / `warn(msg)` / 链式 `put(key, value)` |
| `TableDataInfo` | `com.share.common.core.web.page.TableDataInfo` | `getDataTable(list)` 自动填充 `total`+`rows`+`code`+`msg` |
| `BaseController` | `com.share.common.core.web.controller.BaseController` | `startPage()` / `getDataTable(list)` / `success()` / `error()` / `toAjax(rows)` |

**正例：**
```java
// ✅ C 端 API — R<T>
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.getDetail(id));
    }
    @PostMapping
    public R<Long> create(@Valid @RequestBody CreateOrderDTO dto) {
        return R.ok(orderService.create(dto));
    }
}

// ✅ 管理后台 — AjaxResult + BaseController
@RestController
@RequestMapping("/order")
public class OrderAdminController extends BaseController {
    @GetMapping("/list")
    public TableDataInfo list(Order order) {
        startPage();
        return getDataTable(orderService.selectList(order));
    }
    @PostMapping
    public AjaxResult add(@RequestBody Order order) {
        return toAjax(orderService.save(order));
    }
}

// ✅ Feign 内部 — R<T> + @InnerAuth
@InnerAuth
@GetMapping("/inner/getByOrderNo/{orderNo}")
public R<OrderInfo> getByOrderNo(@PathVariable String orderNo) {
    return R.ok(orderInfoService.getByOrderNo(orderNo));
}
```

**反例：**
```java
// ❌ 同一 Controller 混用
@RequestMapping("/orderInfo")
public class OrderController extends BaseController {
    @GetMapping("/{id}")
    public AjaxResult getInfo(...) { ... }       // AjaxResult
    @PostMapping("/createOrder")
    public R<Void> createOrder(...) { ... }      // R<T> — 混了
}

// ❌ R 不带泛型
public R getUser() { return R.ok(user); }        // 不明确，无法 Swagger 生成正确 schema

// ❌ 分页返回 R<List>
public R<List<Order>> list(...) {
    startPage();
    return R.ok(orderService.list());            // 翻页信息丢了
}
```

### 3.6. 路径规范（按角色区分 Controller 路径前缀）

**三个角色，三种路径前缀：**

| 角色 | 路径前缀 | 返回类型 | 鉴权 | 示例 |
|---|---|---|---|---|
| **C端业主** | `/api/v1/xxx` | `R<T>` | `@RequiresLogin` 或公开 | `/api/v1/user/avatar`、`/api/v1/product/list` |
| **商家** | `/api/v1/merchant/xxx` | `R<T>` | `@RequiresLogin` | `/api/v1/merchant/product/list`、`/api/v1/merchant/profile` |
| **平台管理员** | 无前缀 `/xxx` | `AjaxResult` | `@RequiresPermissions` | `/product/list`、`/merchant/list`、`/userInfo/list` |
| **内部 Feign** | `/inner/xxx` | `R<T>` | `@InnerAuth` | `/inner/sku/{skuId}`、`/inner/order/getByOrderNo/{orderNo}` |

**原则：**
- C端和商家的 Controller **不混用** `AjaxResult` 和 `R<T>`，一个 Controller 只服务一个角色
- 同一服务下可以有多个角色的 Controller（如 share-goods 有 ProductApiController、MerchantProductController、ProductController）
- 内部 Feign 接口单独拆分到 InnerXxxController，不与 C端/商家/管理员接口混在同一类
- 网关 `StripPrefix=1`，Controller 路径**不要**重复网关路由前缀

### 4. 代码注释

- **类注释**：说明类职责
- **方法注释**：`@param` `@return` `@throws`，关键逻辑说明
- **复杂逻辑**：写"为什么这么做"（Why），不写"做了什么"
- **TODO 注释**：遗留问题用 `// TODO: xxx` 标记
- **禁止**：`i++ // i 加 1` 这种废话

### 5. 数据库

- 所有业务表保留 `del_flag`（`0=正常, 2=删除`）
- 必须加 `create_time`、`create_by`、`update_time`、`update_by`
- 表名、字段名小写+下划线
- 索引命名：`idx_表名_字段名`
- 避免 JOIN 超过 3 张表，禁止循环中查数据库

### 5.5. 状态变更规范（updateById 陷阱）

**禁止使用 `updateById()` 更新订单/业务状态字段。** 原因：并发下状态被回退。

```java
// ❌ 错误
order.setStatus("已完成");
baseMapper.updateById(order);

// ✅ 正确：只更新指定字段 + 乐观锁
baseMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
        .eq(OrderInfo::getId, order.getId())
        .eq(OrderInfo::getVersion, order.getVersion())
        .set(OrderInfo::getStatus, "已完成")
        .set(OrderInfo::getVersion, order.getVersion() + 1));
```

> 所有订单状态变更必须通过 `IOrderStatusService.transition()` 路由，自动记录 OrderLog 并处理乐观锁。

### 6. 事务与并发

```
@Transactional(rollbackFor = Exception.class)  // 必须指定回滚异常
```

- 事务只加在 ServiceImpl，不加在 Controller
- 长事务拆分，事务内避免远程 RPC
- 涉及金额/库存/状态变更 → 悲观锁或乐观锁 `version`
- Redis 分布式锁：`RedissonClient.getLock(key)` + `RLock.tryLock()`
- 扣库存/余额 → Redis + Lua 或数据库乐观锁
- 订单防重提交：唯一索引 / Redis token / 状态机
- 异步处理：`@Async` + 线程池隔离

### 7. 分布式微服务

- 服务间通信一律走 Feign，指定 `fallbackFactory`
- 分布式事务：Seata AT（`@GlobalTransactional`），只用在跨服务写操作
- 消息队列：RocketMQ（订单超时延迟消息、支付回调通知）
- 服务熔断：Sentinel（Feign 集成 Sentinel）
- MQ 消费者必须幂等（业务唯一键去重）
- 定时任务加分布式锁防重复执行

### 8. 异常处理

- 业务异常：抛 `ServiceException("xxx")`
- 参数校验：`@Valid` + `@NotBlank` / `@NotNull`
- 全局异常由 `GlobalExceptionHandler` 统一处理
- Feign fallback 返回友好提示，不能抛空指针
- **禁止** `catch (Exception e) { }`

### 9. 代码质量红线

| 禁止项 | 后果 |
|---|---|
| `@Autowired` 字段注入 | 不可测试，违反 DI |
| `@SuppressWarnings` 掩盖问题 | 运行期炸 |
| 循环中查数据库/N+1 | 性能灾难 |
| 事务内调 RPC | 连接池撑爆 |
| 硬编码 IP/密码/密钥 | 环境迁移必挂 |
| 跨服务循环依赖 A→B→A | 启动失败 |
| `updateById()` 更新状态字段 | 并发回退 |
| Feign Fallback 内 `throw ServiceException` | Sentinel 异常误报（用 `R.fail()`） |
| 公开接口缺 `@RequiresLogin`/`@InnerAuth` | 未授权访问 |
| `R<T>` 省略泛型（raw R） | 类型不安全 |

### 10. 模块开发流程

1. Nacos 创建配置：`share-xxx-dev.yml`
2. 网关加路由：`share-gateway-dev.yml`
3. share-api 定义 Feign 接口（如已有则复用）
4. 开发 Service/Impl
5. 开发 Controller（薄层收口）
6. 单元测试 → 自测 → 代码审查

### 11. 类复用规范（先查再用）

**原则：** 新建任何类（常量、工具类、枚举等）之前，必须先在整个项目中检索是否存在同类。有则更新，无则新建。

**查询顺序：**
```
① share-common-core  →  通用工具、常量、枚举、异常、注解
② share-common-redis →  Redis 操作类
③ share-common-security → 鉴权、安全类
④ share-api-xxx      →  Feign 接口、DTO
⑤ 目标模块自身       →  本模块已有的工具/基类
⑥ 以上都不存在       →  新建
```

**新建规则：**
- 如果仅在本服务使用 → 建在本服务模块下（如 `share-modules/share-xxx/.../constant/`、`share-modules/share-xxx/.../utils/`）
- 如果其他服务也要用 → 建在公共模块下（`share-common-core`、`share-common-redis` 等），以便所有服务调用

**禁止：** 不查就新建工具类、在业务模块写通用工具方法、硬编码常量、重复定义已有枚举

### 12. Domain/VO/DTO/Entity 放置规范

| 类型 | 位置 | 说明 |
|---|---|---|
| Entity | `share-api-xxx/domain/` | `@TableName`，继承 `BaseEntity`，兼作 Feign DTO |
| VO | 本地 `xxx/domain/vo/` | 返回值跟 Entity 不一致时再建 |
| DTO | 本地 `xxx/domain/dto/` | 入参/出参比 Entity 复杂时再建 |

**规则：**
- **Entity**：与数据库表一一对应，提前设计好，放在 `share-api-xxx/domain/`，一份即可，业务模块禁止重复放
- **VO/DTO**：按需设计——业务逻辑需要返回跟 Entity 不一致的结构时再建 VO，入参比 Entity 复杂时再建 DTO。不要为了"以后可能用到"提前建

### 13. Service 接口规范（强制）

| 规则 | 说明 |
|---|---|
| **每个 Entity 一个接口** | 必须有 `IXxxService` 接口，禁止 Controller 直接注入 Impl 类 |
| **继承 IService** | `IXxxService extends IService<XxxEntity>`，获得通用 CRUD 方法 |
| **Impl 继承 ServiceImpl** | `class XxxServiceImpl extends ServiceImpl<XxxMapper, XxxEntity> implements IXxxService` |
| **Controller 注入接口** | `private final IXxxService xxxService;` — 禁止 `private final XxxServiceImpl xxxService;` |
| **特殊门面服务** | 聚合业务（如 `IMerchantXxxService`）同样继承 `IService<Entity>`，Impl 继承 `ServiceImpl<Mapper, Entity>`，保持统一模式 |

**例外：** 非实体 CRUD 工具类（如 `OrderStatusServiceImpl` 状态机引擎）可跳过 `IService` 继承。

**反例（禁止）：**
```java
// ❌ Controller 直接注入 Impl
private final ProductServiceImpl productService;

// ❌ 接口未继承 IService
public interface IXxxService { }
```

**正例（强制）：**
```java
// ✅ 接口继承 IService
public interface IProductService extends IService<Product> { }

// ✅ Impl 继承 ServiceImpl + 实现接口
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService { }

// ✅ Controller 注入接口
private final IProductService productService;
```

## 框架类速查

### 启动注解

| 注解 | 作用 |
|---|---|
| `@EnableCustomConfig` | AOP + MapperScan("com.share.**.mapper") + @Async + Feign 拦截器 |
| `@EnableRyFeignClients` | 启用 Feign，扫描 `com.share` |
| `@SpringBootApplication` | 标准 Spring Boot |

**标准启动类：**
```java
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class ShareXxxApplication { ... }
```

### 响应模型

| 类 | 路径 | 用途 |
|---|---|---|
| `R<T>` | share-common-core | 统一返回，`R.ok(data)` / `R.fail(msg)` |
| `AjaxResult` | share-common-core | 后台管理返回（可链式 put） |
| `TableDataInfo` | share-common-core | 分页响应（total / rows / code / msg） |
| `BaseEntity` | share-common-core | 实体基类（id, createTime, updateTime, delFlag, params） |
| `TreeEntity` | share-common-core | 树形实体（parentId, orderNum, children） |

### 控制器基类

**BaseController**（share-common-core）：所有 Controller 继承

| 方法 | 用途 |
|---|---|
| `startPage()` | 读取 pageNum/pageSize 初始化分页 |
| `getDataTable(list)` | 分页结果 → TableDataInfo |
| `success(data)` | AjaxResult.success(data) |
| `toAjax(rows)` | int 影响行数 → success/error |
| `error(msg)` | AjaxResult.error(msg) |

### 鉴权注解

| 注解 | 位置 | 作用 |
|---|---|---|
| `@RequiresLogin` | Controller 方法 | 要求已登录 |
| `@RequiresPermissions("sys:user:list")` | Controller 方法 | 要求权限标识 |
| `@RequiresRoles("admin")` | Controller 方法 | 要求特定角色 |
| `@InnerAuth` | Feign 接口方法 | 仅允许内部 Feign 调用 |

### 获取当前用户

```java
Long userId = SecurityUtils.getUserId();
String username = SecurityUtils.getUsername();
LoginUser loginUser = SecurityUtils.getLoginUser();
String token = SecurityUtils.getToken();

// 线程上下文
SecurityContextHolder.getUserId();
SecurityContextHolder.setUserId(userId);
SecurityContextHolder.remove();
```

### 权限校验工具

```java
AuthUtil.checkLogin();                          // 检查是否登录
AuthUtil.hasPermi("system:user:list");          // 是否有权限
AuthUtil.checkPermi("system:user:list");        // 无权限抛异常
AuthUtil.hasRole("admin");
AuthUtil.checkRole("admin");
```

### 异常体系

| 异常 | 用途 |
|---|---|
| `ServiceException("msg")` | **业务异常（最常用）** |
| `NotLoginException` | 未登录 |
| `NotPermissionException` | 无权限 |
| `NotRoleException` | 无角色 |
| `InnerAuthException` | 内部 Feign 认证失败 |
| `DemoModeException` | 演示模式 |

> 全局异常由 `GlobalExceptionHandler`（share-common-security）统一处理，自行 try-catch 属于违规。

### Redis 操作

```java
@Autowired
private RedisService redisService;

redisService.setCacheObject("key", value);
redisService.setCacheObject("key", value, 30, TimeUnit.MINUTES);
redisService.getCacheObject("key");
redisService.deleteObject("key");
redisService.hasKey("key");
redisService.expire("key", 30, TimeUnit.MINUTES);
redisService.incrementBy("key", 1);
```

### Feign 调用标准模式

```java
// ① share-api-xxx 定义接口
@FeignClient(contextId = "remoteXxxService",
             value = ServiceNameConstants.XXX_SERVICE,
             fallbackFactory = XxxFallbackFactory.class)
public interface RemoteXxxService {
    @GetMapping("/inner/path")
    R<SomeDTO> getData(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}

// ② 调用方注入
@Autowired
private RemoteXxxService remoteXxxService;

R<SomeDTO> result = remoteXxxService.getData(SecurityConstants.INNER);
```

### 分页查询标准模式

```java
// Controller
public TableDataInfo list(SysUser user) {
    startPage();
    List<SysUser> list = userService.selectUserList(user);
    return getDataTable(list);
}

// ServiceImpl
public List<SysUser> selectUserList(SysUser user) {
    return userMapper.selectList(queryWrapper);
}
```

### 日志注解 / 数据源 / 数据权限

```java
// 操作日志
@Log(title = "用户管理", businessType = BusinessType.INSERT)

// 数据源
@Master  // 主库（默认）
@Slave   // 从库

// 数据权限
@DataScope(deptAlias = "d", userAlias = "u")
```

### 服务名称常量

```java
ServiceNameConstants.AUTH_SERVICE     // "share-auth"
ServiceNameConstants.GOODS_SERVICE    // "share-goods"
ServiceNameConstants.ORDER_SERVICE    // "share-order"
ServiceNameConstants.PAYMENT_SERVICE  // "share-payment"
ServiceNameConstants.COUPON_SERVICE   // "share-coupon"
ServiceNameConstants.MERCHANT_SERVICE // "share-merchant"
```

### 工具类目录（share-common-core.utils）

| 类 | 用途 |
|---|---|
| `DateUtils` | 日期格式化、计算 |
| `StringUtils` | 字符串判空、截取、转换 |
| `ExceptionUtil` | 异常堆栈转字符串 |
| `JwtUtils` | JWT 编解码 |
| `PageUtils` | 分页参数处理 |
| `ServletUtils` | 获取 request/response/session |
| `SpringUtils` | Spring Bean 获取 |
| `IpUtils` (`.ip`) | 获取客户端 IP |
| `IdUtils` (`.uuid`) | UUID / 雪花ID / 号段ID |
| `SnowflakeIdWorker` (`.uuid`) | 雪花算法 |
| `IdSegmentGenerator` (`.uuid`) | 号段发号器 |
| `Seq` (`.uuid`) | 序列号生成 |
| `UUID` (`.uuid`) | 高性能 UUID |

### 常量目录（share-common-core.constant）

| 类 | 用途 |
|---|---|
| `Constants` | 通用常量（字符集、时区、HTTP） |
| `CacheConstants` | Redis key 前缀 |
| `SecurityConstants` | 安全 header 常量 |
| `TokenConstants` | Token 鉴权常量 |
| `HttpStatus` | HTTP 状态码 |
| `ServiceNameConstants` | 微服务名称 |
| `UserConstants` | 用户状态/类型 |
| `GenConstants` / `ScheduleConstants` | 代码生成 / 定时任务 |

### 枚举 / 异常 / 注解（share-common-core）

| 枚举 | 用途 |
|---|---|
| `UserStatus` | 用户状态（OK/DISABLE/DELETED） |

| 异常 | 用途 |
|---|---|
| `ServiceException` | **业务异常（最常用）** |
| `NotLoginException` | 未登录 |
| `NotPermissionException` | 无权限 |
| `NotRoleException` | 无角色 |
| `InnerAuthException` | 内部 Feign 认证失败 |
| `DemoModeException` | 演示模式 |
| `PreAuthorizeException` | 权限校验失败 |

| 注解 | 用途 |
|---|---|
| `@Excel` / `@Excels` | 导出 Excel 字段标注 |

### 通用配置（share-common-core）

| 类 | 用途 |
|---|---|
| `MybatisPlusConfig` | MyBatis-Plus 分页/乐观锁插件 |

### Redisson 分布式锁（share-common-redis）

```java
private final RedissonClient redissonClient;

RLock lock = redissonClient.getLock("biz:key:" + id);
if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
    try { /* 业务逻辑 */ }
    finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
}
```

`share-common-redis` 不是所有模块默认依赖，使用前确认 pom.xml 已添加。

## Nacos 配置汇总

所有配置在 Nacos `192.168.10.129:8848`，命名空间 `a746e297-417e-4aec-bfb1-e42df33fbe93`

### 网关路由（share-gateway-dev.yml）

| 路由 | 路径 | 目标服务 |
|---|---|---|
| share-auth | `/auth/**` | lb://share-auth |
| share-system | `/system/**` | lb://share-system |
| share-gen | `/code/**` | lb://share-gen |
| share-job | `/schedule/**` | lb://share-job |
| share-file | `/file/**` | lb://share-file |
| share-goods | `/goods/**` | lb://share-goods |
| share-order | `/order/**` | lb://share-order |
| share-user | `/user/**` | lb://share-user |
| share-payment | `/payment/**` | lb://share-payment |
| share-coupon | `/coupon/**` | lb://share-coupon |
| share-merchant | `/merchant/**` | lb://share-merchant |

全部 StripPrefix=1。白名单（免登录）：`/auth/logout`、`/auth/login`、`/auth/register`、`/*/v2/api-docs`、`/csrf`、`/auth/h5/**`

**注意：** Controller 路径不能重复模块名前缀（StripPrefix 已去掉）。`share-order` 用 `api/OrderInfoApiController` 替代 `controller/` 目录。

### 各模块数据源

| 模块 | 数据库 | 额外中间件 | 特殊配置 |
|---|---|---|---|
| share-auth | 无 | Redis | 登录认证 |
| share-system | share-system | Redis | RuoYi 框架 |
| share-goods | share-goods | Redis | 商品模块 |
| share-user | share-user | Redis | 微信小程序 appId: `wxcc651fcbab275e33` |
| share-order | share-order | Redis + RocketMQ | 订单消息队列 |
| share-payment | share-payment | Redis + RocketMQ | 微信支付 v3（商户号 1631833859） |
| share-coupon | share-coupon | Redis + RocketMQ | 优惠券 |
| share-merchant | share-merchant | Redis | 商家管理 |

## 注意事项（踩坑记录）

- **MySQL 查表**：本地 MCP 用 `mysql-s2`，SSH 到 VM 上要加 `-h 127.0.0.1`（没开 Unix socket）
- **nginx proxy_pass**：IP 必须写 Windows 物理网卡地址（WiFi），不是 VMware 虚拟网卡（192.168.10.1）
- **微服务在 Windows 启动**，代码改完在 IDEA 重启对应模块即可
- **nacos 配置**在各模块 `bootstrap.yml` 指定地址和命名空间
