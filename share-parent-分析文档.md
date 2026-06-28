# share-parent 项目分析文档

> **项目名称**: 小区电商管理系统（基于若依微服务框架）
> **版本**: 3.6.3  
> **GroupId**: com.share  
> **ArtifactId**: share  
> **技术栈**: Spring Cloud Alibaba + RocketMQ + Redisson + Vue 3 + Element Plus

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈总览](#2-技术栈总览)
3. [项目模块结构](#3-项目模块结构)
4. [核心模块详解](#4-核心模块详解)
   - [4.1 share-gateway — 网关模块](#41-share-gateway--网关模块)
   - [4.2 share-auth — 认证中心](#42-share-auth--认证中心)
   - [4.3 share-common — 通用模块](#43-share-common--通用模块)
   - [4.4 share-api — 接口模块](#44-share-api--接口模块)
    - [4.5 share-modules — 业务模块](#45-share-modules--业务模块)
    - [4.5.1 share-system — 系统管理](#451-share-system--系统管理)
    - [4.5.2 share-gen — 代码生成](#452-share-gen--代码生成)
    - [4.5.3 share-job — 定时任务](#453-share-job--定时任务)
    - [4.5.4 share-file — 文件服务](#454-share-file--文件服务)
    - [4.5.5 share-user — 用户模块](#455-share-user--用户模块)
    - [4.5.6 share-goods — 商品服务](#456-share-goods--商品服务)
    - [4.5.7 share-order — 订单模块](#457-share-order--订单模块)
    - [4.5.8 share-payment — 支付模块](#458-share-payment--支付模块)
    - [4.5.9 share-coupon — 优惠券模块](#459-share-coupon--优惠券模块)
   - [4.6 share-visual — 图形化管理模块](#46-share-visual--图形化管理模块)
   - [4.7 share-ui — 前端](#47-share-ui--前端)
5. [技术架构详解](#5-技术架构详解)
6. [服务端口与部署](#6-服务端口与部署)
7. [开发环境与构建](#7-开发环境与构建)
8. [扩展与定制内容](#8-扩展与定制内容)
9. [开发规范](#9-开发规范)

---

## 1. 项目概述

本项目是基于 **RuoYi-Cloud v3.6.3**（若依微服务快速开发平台）二次开发的 **小区电商管理系统**。

若依框架提供了完善的后台管理基础功能（用户、角色、菜单、权限、日志、字典等）。本项目在其微服务架构基础上，面向**小区电商**领域进行开发，为社区供应商提供微信小程序电商能力。

### 原始框架能力

- 前后端分离的微服务架构
- 注册中心、配置中心: **Nacos**
- 权限认证: **Redis + JWT**
- 流量控制: **Sentinel**
- 分布式事务: **Seata**
- API 文档: **Knife4j** (Swagger)

---

## 2. 技术栈总览

### 后端核心

| 组件 | 版本 | 用途 |
|---|---|---|
| Java | 17 | 运行环境 |
| Spring Boot | 3.0.5 | 基础框架 |
| Spring Cloud | 2022.0.2 | 微服务治理 |
| Spring Cloud Alibaba | 2022.0.0.0-RC2 | 阿里云微服务组件 |
| Nacos | — | 注册中心 + 配置中心 |
| Spring Cloud Gateway | — | API 网关 |
| Sentinel | — | 流量控制/熔断降级 |
| Seata | — | 分布式事务 |
| MyBatis-Plus | 3.5.3.1 | ORM 持久层 |
| Redis / Redisson | 3.23.3 | 缓存与分布式锁 |
| Druid | 1.2.21 | 数据库连接池 |
| Dynamic Datasource | 4.2.0 | 多数据源 |

### 业务中间件

| 组件 | 版本 | 用途 |
|---|---|---|
| MinIO | 8.5.2 | 对象存储（文件服务） |
| FastDFS | 1.27.2 | 分布式文件系统 |
| RocketMQ | 2.2.3 | 消息队列（订单异步处理、延迟消息） |
| XXL-Job / Quartz | — | 分布式定时任务 |

### 前端

| 组件 | 版本 |
|---|---|
| Vue | 3.3.9 |
| Element Plus | 2.4.3 |
| Vite | 5.0.4 |
| Pinia | 2.1.7 |
| Vue Router | 4.2.5 |
| Axios | 0.27.2 |
| ECharts | 5.4.3 |

### 工具库

- **JWT**: `jjwt 0.9.1` — Token 鉴权
- **FastJSON2**: `2.0.43` — JSON 序列化
- **Hutool**: `5.8.16` — Java 工具集
- **Knife4j**: `4.1.0` — API 文档
- **PageHelper**: `2.0.0` — 分页插件
- **POI**: `4.1.2` — Excel 处理
- **Velocity**: `2.3` — 代码生成模板引擎
- **Lombok**: `1.18.30`
- **微信支付 SDK**: `wechatpay-java 0.2.11`
- **微信小程序 SDK**: `weixin-java-miniapp 4.5.5.B`
- **TransmittableThreadLocal**: `2.14.4`

---

## 3. 项目模块结构

```
share-parent (E:\IJIDE\IdeaProjects\share-parent)
│
├── pom.xml                             # 父 POM，统一依赖与版本管理
├── README.md                           # 项目介绍
├── LICENSE                             # 开源协议
├── .gitignore
│
├── share-gateway/                      # 网关模块 [端口: 8080]
│   └── src/main/java/com/share/gateway/
│       ├── filter/                     # 过滤器链（认证/黑名单/验证码/XSS/缓存）
│       ├── handler/                    # 异常处理 + Sentinel 降级 + Swagger
│       ├── config/                     # 网关配置（验证码/路由/Swagger）
│       ├── config/properties/          # 配置属性（白名单/XSS/验证码）
│       └── service/                    # 验证码服务
│
├── share-auth/                         # 认证中心 [端口: 9200]
│   └── src/main/java/com/share/auth/
│       ├── controller/
│       │   ├── TokenController.java        # 登录/退出/刷新/注册
│       │   └── H5TokenController.java      # H5/移动端登录
│       ├── form/
│       │   ├── LoginBody.java              # 登录表单
│       │   └── RegisterBody.java           # 注册表单
│       └── service/
│           ├── SysLoginService.java         # 系统登录业务
│           ├── H5LoginService.java          # H5端登录业务
│           ├── SysPasswordService.java      # 密码校验服务
│           └── SysRecordLogService.java     # 登录日志记录
│
├── share-common/                       # 公共模块（无业务逻辑）
│   ├── share-common-core/              # 核心模块
│   │   ├── annotation/                 # 自定义注解（@Excel）
│   │   ├── constant/                   # 常量定义（缓存/安全/服务名/令牌等）
│   │   ├── domain/                     # 通用领域（R 响应/AjaxResult/BaseEntity）
│   │   ├── enums/                      # 枚举（UserStatus）
│   │   ├── exception/                  # 异常体系（全局/认证/文件/用户/任务）
│   │   ├── utils/                      # 工具类（JWT/Spring/Date/POI/XSS/Bean）
│   │   ├── web/                        # Web基类（BaseController/分页）
│   │   └── xss/                        # XSS 过滤
│   │
│   ├── share-common-security/          # 安全模块
│   │   ├── annotation/                 # @EnableCustomConfig/@InnerAuth/@Requires*
│   │   ├── aspect/                     # InnerAuth/PreAuthorize AOP
│   │   ├── auth/                       # AuthUtil/AuthLogic 权限校验逻辑
│   │   ├── config/                     # WebMvc/Application 配置
│   │   ├── feign/                      # Feign 请求拦截器（Token传递）
│   │   ├── handler/                    # 全局异常处理器
│   │   ├── interceptor/                # 请求头拦截器（HeaderInterceptor）
│   │   ├── service/                    # TokenService
│   │   └── utils/                      # SecurityUtils/DictUtils
│   │
│   ├── share-common-redis/             # 缓存模块
│   │   ├── configure/                  # RedisConfig + FastJson2 序列化
│   │   └── service/                    # RedisService 封装
│   │
│   ├── share-common-log/               # 日志模块
│   │   ├── annotation/@Log             # 操作日志注解
│   │   ├── aspect/LogAspect            # 日志切面
│   │   ├── enums/                      # BusinessType/OperatorType 枚举
│   │   ├── filter/                     # 属性过滤
│   │   └── service/AsyncLogService     # 异步日志服务
│   │
│   ├── share-common-datascope/         # 数据权限模块
│   │   ├── annotation/@DataScope       # 数据权限注解
│   │   └── aspect/DataScopeAspect      # 数据权限切面
│   │
│   ├── share-common-datasource/        # 多数据源模块
│   │   └── annotation/                 # @Master / @Slave
│   │
│   ├── share-common-seata/             # 分布式事务模块
│   │
│   └── share-common-rocketmq/          # RocketMQ 消息队列模块
│
├── share-api/                          # 接口定义模块（Feign 客户端 + DTO）
│   ├── share-api-system/               # 系统接口（LoginUser/系统配置/菜单等）
│   ├── share-api-user/                 # 用户接口（UserInfoApi）
│   ├── share-api-goods/                # ✅ 商品 Feign 接口
│   └── share-api-order/                # 🔧 订单 Feign 接口（构建中）
│
├── share-modules/                      # 业务模块
│   ├── share-system/                   # 系统管理 [9201]
│   │   ├── controller/                 # 14个控制器（用户/角色/菜单/部门/字典/配置/日志等）
│   │   ├── domain/                     # 实体 & VO（Menu/Dept/Role/Config/Notice等）
│   │   ├── mapper/                     # MyBatis Mapper
│   │   └── service/                    # 业务接口+实现
│   │
│   ├── share-gen/                      # 代码生成 [9202]
│   │   ├── controller/                 # 生成器控制器
│   │   ├── domain/                     # GenTable/GenTableColumn
│   │   ├── mapper/                     # 表结构读取
│   │   ├── service/                    # 代码生成+模板渲染
│   │   └── util/                       # Velocity模板引擎/工具类
│   │
│   ├── share-job/                      # 定时任务 [9203]
│   │   ├── config/                     # Quartz 调度配置
│   │   ├── controller/                 # 任务/日志管理
│   │   ├── domain/                     # SysJob/SysJobLog
│   │   ├── mapper/
│   │   ├── service/
│   │   ├── task/                       # RyTask 示例任务
│   │   └── util/                       # Quartz 工具（Cron/调度/执行）
│   │
│   ├── share-file/                     # 文件服务 [9300]
│   │   ├── config/                     # MinIO 配置
│   │   ├── controller/                 # 文件上传下载
│   │   ├── service/                    # 三种实现：Local/FastDFS/MinIO
│   │   └── utils/                      # 文件上传工具
│   │
│   ├── share-user/                     # ⭐ 用户模块
│   │   ├── config/                     # 微信小程序配置
│   │   ├── controller/                 # 用户信息管理/API
│   │   ├── mapper/
│   │   ├── service/                    # 会员信息管理
│   │   └── api/                        # 内部 API 接口
│   │
│   ├── share-goods/                    # ✅ 商品服务 [9210]
│   │   ├── controller/                 # 分类/商品/SKU 管理 API
│   │   ├── domain/                     # Category / Product / ProductSku 实体
│   │   ├── mapper/                     # 商品数据访问 + XML（含库存扣减）
│   │   └── service/                    # 商品业务（分类/商品/SKU/库存）
│   │
│   ├── share-order/                    # ⭐ 订单模块（构建中）
│   │   ├── controller/                 # 订单 API
│   │   ├── domain/                     # OrderLog/OrderStatistics/TradeVo
│   │   ├── mapper/                     # OrderBill/OrderInfo/OrderStatistics
│   │   ├── service/                    # 订单业务（计费/支付/结算）
│   │   └── receiver/                   # RocketMQ 消息消费者
│   │
│   ├── share-payment/                   # 🔧 支付模块 [9213]（骨架）
│   │
│   └── share-coupon/                   # 🔧 优惠券模块 [9214]（骨架）
│
├── share-visual/                       # 图形化管理
│   └── share-monitor/                  # 监控中心 [9100]
│       ├── config/                     # Spring Security 安全配置
│       └── ...                         # Spring Boot Admin 服务监控
│
├── share-ui/                           # 前端（Vue 3 + Element Plus + Vite）
│   ├── src/
│   │   ├── api/                        # API 接口调用
│   │   ├── views/                      # 页面视图
│   │   │   ├── system/                 # 系统管理页面
│   │   │   ├── monitor/                # 监控页面
│   │   │   └── tool/                   # 工具页面（代码生成/表单构建）
│   │   ├── components/                 # 公共组件
│   │   ├── layout/                     # 布局组件
│   │   ├── router/                     # 路由配置
│   │   ├── store/                      # Pinia 状态管理
│   │   ├── utils/                      # 工具函数
│   │   └── styles/                     # 全局样式
│   ├── package.json                    # 依赖配置
│   └── vite.config.js                  # Vite 构建配置
│
└── sql/                                # 数据库脚本
    ├── share-system.sql                # 系统库表结构 + 初始化数据
    └── quartz.sql                      # Quartz 定时任务表
```

---

## 4. 核心模块详解

### 4.1 share-gateway — 网关模块

**端口**: 8080 | **技术**: Spring Cloud Gateway（Reactive）

网关作为系统的统一入口，负责请求路由、认证拦截、流量控制和跨域处理。

#### 过滤器链（按顺序）

| 过滤器 | 作用 |
|---|---|
| **CacheRequestFilter** | 缓存请求体（解决 Body 只能读取一次的问题，用于 XSS 过滤和验签） |
| **BlackListUrlFilter** | 黑名单 URL 拦截 |
| **AuthFilter** | 全局认证过滤器，校验 Token 有效性，白名单 URL 跳过 |
| **ValidateCodeFilter** | 验证码校验过滤器 |
| **XssFilter** | XSS 攻击过滤 |

#### 处理器

| 处理器 | 作用 |
|---|---|
| **GatewayExceptionHandler** | 统一网关异常处理 |
| **SentinelFallbackHandler** | Sentinel 限流降级处理 |
| **SwaggerHandler** | 聚合各微服务 Swagger 文档 |
| **ValidateCodeHandler** | 验证码生成接口 |

#### 配置属性

| 配置类 | 用途 |
|---|---|
| `IgnoreWhiteProperties` | 认证白名单 URL 配置 |
| `CaptchaProperties` | 验证码类型/开关配置 |
| `XssProperties` | XSS 过滤配置 |

#### 关键特性

- **路由转发**: 根据请求路径将请求转发到对应的微服务（如 `/auth/**` → `share-auth`）
- **统一鉴权**: AuthFilter 在所有非白名单请求上校验 JWT Token
- **验证码**: Kaptcha 图形验证码生成与校验
- **Sentinel 集成**: 网关层面进行流量控制和熔断降级
- **跨域配置**: CORS 统一处理
- **Swagger 聚合**: 汇总所有微服务的 API 文档

---

### 4.2 share-auth — 认证中心

**端口**: 9200 | **不加载数据源**（`@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)`）

认证中心负责用户认证、Token 颁发与管理。

#### 核心 API

| 接口 | 方法 | 说明 |
|---|---|---|
| `/login` | POST | 用户名密码登录，返回 Token |
| `/logout` | DELETE | 登出，清除 Token 缓存 |
| `/refresh` | POST | 刷新 Token 有效期 |
| `/register` | POST | 用户注册 |
| `/h5/login` | POST | H5/移动端快捷登录 |

#### 登录流程

```
用户请求 /login
  → SysLoginService.login(username, password)
    → 1. 校验验证码（从 Redis 获取）
    → 2. 调用 share-system API 获取用户信息（Feign 远程调用）
    → 3. SysPasswordService 校验密码
    → 4. SysRecordLogService 记录登录日志（异步 Feign）
    → 5. 返回 LoginUser
  → TokenService.createToken(loginUser)
    → 1. 生成 JWT Token（含用户ID/用户名/权限信息）
    → 2. 用户信息存入 Redis（带过期时间）
    → 3. 返回 Token 给客户端
```

#### 关键类

- **TokenController**: 对外暴露认证 REST API
- **H5TokenController**: H5/移动端专用认证接口
- **SysLoginService**: 封装系统登录业务逻辑
- **SysPasswordService**: 密码强度校验和加密校验
- **SysRecordLogService**: 异步记录登录日志

---

### 4.3 share-common — 通用模块

无业务逻辑，提供全系统共享的基础能力。分为 8 个子模块：

#### share-common-core — 核心模块

提供全系统使用的基础类与工具：

- **domain/R.java**: 统一响应体，封装 `code`/`msg`/`data`，提供 `ok()`/`fail()` 静态工厂
- **constant/**: 系统常量定义
  - `Constants`: 通用常量（成功/失败码、Token前缀、时间格式等）
  - `SecurityConstants`: 安全相关常量（请求头、Token类型等）
  - `CacheConstants`: Redis 缓存 Key 命名规范
  - `ServiceNameConstants`: 微服务名称常量
   - `UserConstants`, `GenConstants`, `ScheduleConstants` 等
- **exception/**: 完整的异常体系
  - 基础: `BaseException`, `GlobalException`, `ServiceException`
  - 认证: `NotLoginException`, `NotPermissionException`, `NotRoleException`
  - 文件: `FileException`, `FileSizeLimitExceededException`
  - 用户: `UserException`, `UserPasswordNotMatchException`
  - 工具: `CaptchaExpireException`, `CaptchaException`, `DemoModeException`
- **utils/**: 丰富的工具类集
  - `JwtUtils` — JWT Token 编解码
  - `SpringUtils` — Spring 容器工具
  - `StringUtils`, `DateUtils`, `ServletUtils`, `PageUtils`
  - `poi/ExcelUtil` — Excel 导入导出（基于注解）
  - `ip/IpUtils` — IP 地址工具
  - `file/FileUtils`, `FileTypeUtils`, `MimeTypeUtils`
  - `sign/Base64` — Base64 编解码
   - `uuid/IdUtils`, `Seq`, `UUID`, `SnowflakeIdWorker`, `IdSegmentGenerator`
  - `html/EscapeUtil`, `HTMLFilter` — HTML 转义/XSS 过滤
  - `bean/BeanUtils`, `BeanValidators`
  - `sql/SqlUtil` — SQL 注入过滤
- **web/**: Web 层基类
  - `BaseController` — Controller 基类（分页/响应封装）
  - `BaseEntity` — 实体基类（createTime/updateTime/params）
  - `TreeEntity` — 树形实体基类
  - `TableDataInfo` / `TableSupport` — 分页查询支持
- **config/MybatisPlusConfig**: MyBatis-Plus 分页插件配置
- **xss/**: `@Xss` 注解 + `XssValidator` 校验器

#### share-common-security — 安全模块

微服务间和微服务内的安全控制：

- **注解体系**:
  - `@EnableCustomConfig` — 启用自定义安全配置
  - `@EnableRyFeignClients` — 启用若依风格的 Feign 客户端（带请求拦截器）
  - `@InnerAuth` — 仅允许内部 Feign 调用（校验请求头）
  - `@RequiresLogin` / `@RequiresPermissions` / `@RequiresRoles` — 权限校验
- **AOP**: `InnerAuthAspect` 内部认证 AOP，`PreAuthorizeAspect` 权限注解 AOP
- **Token 服务**: `TokenService` — Token 创建/刷新/校验/清除
- **安全工具**: `SecurityUtils` — 获取当前登录用户信息
- **Feign 拦截**: `FeignRequestInterceptor` — Feign 调用时自动携带 Token 和内部请求标识
- **Web 配置**: `WebMvcConfig` — 拦截器注册
- **全局异常**: `GlobalExceptionHandler` — `@RestControllerAdvice` 统一异常处理
- **AuthUtil/AuthLogic**: 基于 Token 的权限校验逻辑

#### share-common-redis — 缓存模块

- **RedisConfig**: Redis 配置（序列化方式、连接池）
- **RedisService**: 对 `RedisTemplate` 的封装（get/set/delete/expire/incrementBy 等）
- **FastJson2JsonRedisSerializer**: 使用 FastJSON2 作为 Redis 序列化方案
- **RedissonConfig**: Redisson 分布式锁配置（单节点/哨兵模式，看门狗自动续期）

#### share-common-log — 日志模块

- **@Log 注解**: 标记需要记录操作日志的 Controller 方法
- **LogAspect**: 日志切面，自动记录操作日志（模块/标题/业务类型/操作类型/请求参数/返回结果/耗时）
- **AsyncLogService**: 异步保存操作日志（通过 Feign 调用 system 服务）
- **BusinessType/OperatorType**: 业务类型（INSERT/UPDATE/DELETE/EXPORT/IMPORT 等）和操作者类型枚举

#### share-common-datascope — 数据权限模块

- **@DataScope 注解**: 标记需要数据权限过滤的方法
- **DataScopeAspect**: 数据权限 AOP，根据用户角色自动拼接 SQL 条件（部门/用户/自定义）

#### share-common-datasource — 多数据源模块

- **@Master / @Slave 注解**: 标记主库/从库数据源

#### share-common-seata — 分布式事务模块

提供 Seata 分布式事务支持，用于跨微服务的分布式事务场景。

#### share-common-rocketmq — 消息队列模块

集成 RocketMQ，用于订单异步处理、延迟消息（订单超时取消）、支付回调通知等场景。

---

### 4.4 share-api — 接口模块

定义 Feign 远程调用接口和 DTO，供各微服务之间通信。

| 子模块 | 核心接口 | 用途 |
|---|---|---|
| **share-api-system** | — | 系统接口（用户信息/权限/配置/字典/操作日志/登录日志） |
| | `domain/LoginUser` | 登录用户模型（用户ID/角色/权限/部门） |
| | `factory/RemoteUserFallbackFactory` | Feign 降级工厂 |
| **share-api-user** | `UserInfoApi` | 用户信息接口 |
| | `domain/AppUser` | 应用端用户模型 |

Feign 调用示例:
```
share-auth → share-api-system (获取用户信息/记录日志)
share-user → share-api-system (获取配置/字典)
```

---

### 4.5 share-modules — 业务模块

#### 4.5.1 share-system — 系统管理 [9201]

完整的后台管理功能，提供 13 个业务控制器：

| 控制器 | 功能 |
|---|---|
| SysUserController | 用户管理（CRUD/密码重置/状态修改/导入导出） |
| SysRoleController | 角色管理（CRUD/权限分配/状态修改） |
| SysMenuController | 菜单管理（树形结构/权限标识/角色菜单分配） |
| SysDeptController | 部门管理（树形结构/数据权限） |
| SysPostController | 岗位管理 |
| SysDictDataController | 字典数据维护 |
| SysDictTypeController | 字典类型维护 |
| SysConfigController | 系统参数配置 |
| SysNoticeController | 通知公告 |
| SysOperlogController | 操作日志查询/删除/清空 |
| SysLogininforController | 登录日志查询/删除/清空 |
| SysUserOnlineController | 在线用户监控 |
| SysProfileController | 个人中心（头像/密码/基本信息） |

数据模型：基于 MySQL 关系型数据库，使用 MyBatis-Plus Mapper 访问。

#### 4.5.2 share-gen — 代码生成 [9202]

基于 Velocity 模板引擎的代码生成器。

- 读取数据库表结构 → 自动生成 Controller/Service/Mapper/Entity/前端页面
- 支持 CRUD 下载，大幅提高开发效率

#### 4.5.3 share-job — 定时任务 [9203]

基于 Quartz 的分布式定时任务管理：

- 在线创建/修改/删除/启停任务
- 任务调用支持：Spring Bean 调用 + URL 调用
- 执行结果日志记录
- 并发控制（`@DisallowConcurrentExecution`）
- Cron 表达式工具
- 失败处理策略

借助 `ScheduleConfig` 配置 Scheduler 工厂 Bean。

#### 4.5.4 share-file — 文件服务 [9300]

统一文件服务接口，支持三种存储实现：

| 实现类 | 存储后端 |
|---|---|
| `LocalSysFileServiceImpl` | 本地磁盘存储 |
| `FastDfsSysFileServiceImpl` | FastDFS 分布式文件系统 |
| `MinioSysFileServiceImpl` | MinIO 对象存储 |

#### 4.5.5 share-user — ⭐ 用户模块（待开发）

C 端用户模块，支持微信小程序登录：

| 组件 | 功能 |
|---|---|
| UserInfoController | 用户信息查询/更新 |
| UserInfoApiController | 内部 API（用户校验/信息获取） |
| WxMaConfig / WxMaProperties | 微信小程序配置（appId/secret） |
| IUserInfoService | 用户信息服务（注册/登录/信息维护） |

集成微信小程序 SDK（`weixin-java-miniapp`），支持微信登录和用户绑定。

#### 4.5.6 share-goods — ✅ 商品服务 [9210]

已开发的电商商品模块，提供分类/商品/SKU 管理能力：

| 组件 | 功能 |
|---|---|
| CategoryController / Service | 商品分类管理（树形结构） |
| ProductController / Service | 商品 CRUD、上下架 |
| ProductSkuController / Service | SKU 管理、库存扣减（乐观锁+重试） |
| ProductSkuMapper | 库存扣减 SQL（含 version 版本号） |

库存扣减采用**乐观锁（version 字段）+ 3 次重试**策略，避免超卖。订单号使用**号段模式**（Leaf-Segment）生成，通过 `IdUtils.orderNo()` 获取。

#### 4.5.7 share-order — ✅ 订单服务 [9211]

正在开发的订单模块，已完成基础设施：

| 组件 | 状态 |
|---|---|
| OrderInfo / OrderBill / OrderLog 实体 | ✅ 已建 |
| OrderInfoMapper / OrderBillMapper | ✅ 已建 |
| 订单号生成（号段模式） | ✅ `IdUtils.orderNo()` |
| bootstrap.yml + Nacos 配置 | ✅ 已配（端口 9211） |
| RocketMQ 消息消费者 | 🔧 待开发 |
| 订单业务 Service | 🔧 待开发 |
| 支付对接 | 🔧 待开发 |

#### 4.5.8 share-payment — 🔧 支付模块 [9213]（骨架）

支付模块骨架，基于微信支付 v3：

| 组件 | 状态 |
|---|---|
| 启动类 / bootstrap.yml | ✅ 已建 |
| Nacos 配置（share-payment-dev.yml） | ✅ 已推 |
| 微信支付 v3 业务逻辑 | 🔧 待开发 |
| 支付回调处理 | 🔧 待开发 |

#### 4.5.9 share-coupon — 🔧 优惠券模块 [9214]（骨架）

优惠券模块骨架：

| 组件 | 状态 |
|---|---|
| 启动类 / bootstrap.yml | ✅ 已建 |
| Nacos 配置（share-coupon-dev.yml） | ✅ 已推 |
| 优惠券 CRUD | 🔧 待开发 |
| 发放/核销逻辑 | 🔧 待开发 |

---

### 4.6 share-visual — 图形化管理模块

#### share-monitor — 监控中心 [9100]

基于 **Spring Boot Admin** 的服务监控：

- 服务列表与健康状态
- 实时监控指标（CPU/内存/线程/GC）
- 日志级别动态调整
- JVM 信息与堆栈追踪
- `WebSecurityConfigurer` 保护监控端点安全

---

### 4.7 share-ui — 前端

基于 **Vue 3 + Element Plus + Vite** 的后台管理前端。

#### 技术栈

| 依赖 | 用途 |
|---|---|
| Vue 3 + Composition API | 前端框架 |
| Element Plus | UI 组件库 |
| Vue Router 4 | 路由管理 |
| Pinia | 状态管理 |
| Axios | HTTP 请求 |
| ECharts | 数据可视化 |
| vue-quill | 富文本编辑器 |
| vue-cropper | 图片裁剪 |
| vite-plugin-svg-icons | SVG 图标 |
| jsencrypt | RSA 加密（密码传输加密） |
| sass | CSS 预处理器 |

#### 目录结构概要

```
share-ui/src/
├── api/                  # API 接口封装
├── assets/               # 静态资源
├── components/           # 公共组件（Pagination/Editor/FileUpload/DictTag等）
├── layout/               # 布局（侧边栏/顶部导航/Tabs等）
├── router/               # 路由配置
├── store/                # Pinia Store
├── utils/                # 工具函数
├── views/                # 页面
│   ├── system/           # 系统管理（用户/角色/菜单/部门/字典/配置等）
│   ├── monitor/          # 监控（在线用户/操作日志/服务监控/缓存监控）
│   └── tool/             # 工具（代码生成/表单构建/系统接口）
└── styles/               # 全局样式
```

#### 构建命令

```bash
npm run dev           # 开发模式启动
npm run build:prod    # 生产构建
npm run build:stage   # 预发布构建
```

---

## 5. 技术架构详解

### 5.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      客户端 (浏览器 / 微信小程序)                   │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP/HTTPS
┌──────────────────────────────▼──────────────────────────────────┐
│                    Nginx (反向代理 / 负载均衡)                      │
└──────────────────────────────┬──────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│          Spring Cloud Gateway (API 网关) [8080]                  │
│   过滤器: AuthFilter / XssFilter / ValidateCodeFilter / ...      │
│   Sentinel 限流 / 熔断 / 跨域 / Swagger 聚合                     │
└──┬───────────┬───────────┬───────────┬───────────┬──────────────┘
   │           │           │           │           │
┌──▼──┐   ┌───▼────┐  ┌──▼────┐  ┌──▼───────┐
│Auth │   │System  │  │File   │  │业务模块   │
│9200 │   │ 9201   │  │ 9300  │  │(待开发)   │
└──┬──┘   └───┬────┘  └──┬────┘  └──┬───────┘
   │           │           │           │
   └───────────┴───────────┴───────────┘
                      │
          ┌───────────┴──────────────┐
          │    Nacos (注册中心 + 配置中心)   │
          │       192.168.10.129:8848      │
          └──────────────────────────────┘
```

### 5.2 服务注册与配置

所有服务通过 **Nacos** 进行服务注册发现和配置管理：

```yaml
# Nacos 配置 (统一配置中心)
spring.cloud.nacos:
  discovery:
    server-addr: 192.168.10.129:8848
    namespace: a746e297-417e-4aec-bfb1-e42df33fbe93
  config:
    server-addr: 192.168.10.129:8848
    namespace: a746e297-417e-4aec-bfb1-e42df33fbe93
    shared-configs:
      - application-dev.yml  # 共享配置（所有服务通用）
```

### 5.3 认证授权流程

```
┌──────┐    ┌──────────┐    ┌───────────┐    ┌────────────┐
│Client│    │ Gateway  │    │   Auth    │    │   System   │
│      │    │  8080    │    │  9200     │    │   9201     │
└──┬───┘    └────┬─────┘    └─────┬─────┘    └─────┬──────┘
   │             │                │                │
   │  POST/login │                │                │
   │────────────►│  AuthFilter    │                │
   │             │ (跳过白名单)    │                │
   │             │──►/auth/login──►                │
   │             │                │──Feign────────►│ 查询用户
   │             │                │◄───────────────│
   │             │                │ 校验密码       │
   │             │                │ 生成Token      │
   │             │◄───R(Token)────│ 缓存到Redis    │
   │◄────R(Token)│                │                │
   │             │                │                │
   │ 后续请求     │                │                │
   │  GET/xxx    │                │                │
   │────────────►│  AuthFilter    │                │
   │             │ 校验Token      │                │
   │             │ (Redis查)     │                │
   │◄────────────│ 转发到业务服务  │                │
```

### 5.4 Feign 远程调用

微服务间通过 Feign + 负载均衡（Nacos）通信：

```
share-auth ──Feign──► share-api-system (获取用户信息/记录日志)
```

Feign 请求拦截器自动传递 Token 和内部认证标识，支持 `@InnerAuth` 内部鉴权。

### 5.5 分布式事务

使用 **Seata** 处理跨服务的分布式事务场景（后续电商业务跨模块写操作时启用）。

### 5.6 消息队列

使用 **RocketMQ** 处理异步消息（订单超时取消、支付回调通知等），利用其延迟消息机制实现订单超时自动取消。

### 5.7 缓存策略

| 缓存用途 | 存储 | Key 前缀 |
|---|---|---|
| 用户 Token 会话 | Redis | `login_tokens:` |
| 验证码 | Redis | `captcha_codes:` |
| 用户权限 | Redis | `auth_perm:` |
| 路由/配置 | Redis | `sys_route:` / `sys_config:` |
| 字典缓存 | Redis | `sys_dict:` |
| 重复提交锁 | Redis | `repeat_submit:` |

---

## 6. 服务端口与部署

| 服务 | 端口 | 说明 |
|---|---|---|
| **share-gateway** | 8080 | API 网关（外部入口） |
| **share-auth** | 9200 | 认证授权中心 |
| **share-system** | 9201 | 系统管理 |
| **share-gen** | 9202 | 代码生成 |
| **share-job** | 9203 | 定时任务 |
| **share-file** | 9300 | 文件服务 |
| **share-goods** | 9210 | ✅ 商品服务（分类/商品/SKU） |
| **share-order** | 9211 | ✅ 订单服务（构建中） |
| **share-payment** | 9213 | 🔧 支付模块（骨架） |
| **share-coupon** | 9214 | 🔧 优惠券模块（骨架） |
| **share-visual-monitor** | 9100 | 监控中心 |
| **share-user** | — | 🔧 用户管理（待开发） |
| **share-ui** | 80 (dev: 80) | 前端 |
| **Nacos** | 8848 | 注册中心 + 配置中心 |
| **Nacos Namespace** | a746e297-417e-4aec-bfb1-e42df33fbe93 | Nacos 命名空间 |

**环境**: `dev`（开发环境），其他环境（`prod`/`test`）配置通过 Nacos 管理。

**部署架构**:
- 所有服务注册到 Nacos，通过 Gateway 统一对外暴露
- Gateway 按请求路径分发到各微服务
- 前端通过 API 网关访问，无需直接连接各微服务

---

## 7. 开发环境与构建

### 7.1 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.6+ |
| Node.js | 16+ |
| Nacos | 2.x |
| MySQL | 8.0+ |
| Redis | 6.x+ |
| RocketMQ | 5.x | 消息队列（异步解耦、延迟消息） |
| MinIO (可选) | — |

### 7.2 构建命令

```bash
# 后端打包
mvn clean package -DskipTests

# 前端构建
cd share-ui
npm install
npm run build:prod
```

### 7.3 数据库

```
sql/
├── share-system.sql    # 系统管理数据库表 + 初始数据
└── quartz.sql          # Quartz 定时任务表
```

### 7.4 项目 Git 历史

```
b6c8736 更新设备服务实现类，完善附近站点和站点详情查询功能,添加会员管理，规则模块
3e42525 对充电宝基本属性的增删改查/根据上级code获取下级数据列表
d84b64d Initial commit (若依原始框架初始化)
```

---

## 8. 扩展与定制内容

### 8.1 基于 RuoYi-Cloud 的扩展

| 扩展内容 | 说明 |
|---|---|
| **share-user** 用户模块 | C 端微信小程序会员体系 |
| **微信小程序集成** | 用户微信登录与支付 |
| **share-api** 接口扩展 | 用户/商品/订单 Feign API |
| **RocketMQ 集成** | 订单超时延迟消息、支付回调 |
| **Redisson 分布式锁** | 基于 RedissonClient 的看门狗自动续期锁 |
| **号段模式 ID 生成** | Leaf-Segment 发号器，`IdUtils.orderNo()` |
| **MinIO 文件存储** | 扩展文件存储方式 |
| **TransmittableThreadLocal** | 线程上下文传递 |

### 8.2 原始框架保留功能

| 功能 | 说明 |
|---|---|
| 用户/角色/菜单/部门管理 | RBAC 权限模型 |
| 操作日志 + 登录日志 | AOP 自动记录 |
| 数据权限 | 部门/用户数据隔离 |
| 代码生成器 | Velocity 模板引擎 |
| Quartz 定时任务 | 在线任务调度管理 |
| 服务监控 | Spring Boot Admin |
| 字典/参数/通知公告 | 系统基础数据管理 |
| 在线构建器 | 表单拖拽生成 |
| Sentinel 限流熔断 | 网关 + 客户端 |
| Seata 分布式事务 | 跨库事务管理 |
| Knife4j API 文档 | Swagger 增强 |

---

## 9. 开发规范

### 9.1 类复用规范（先查再用）

新增任何类（工具类、常量、枚举、异常、注解、配置类、泛型基类）之前，**必须先查询项目已有类目录**，确认不存在后再新建。

**查询顺序（优先级从高到低）：**

```
① share-common-core  →  通用工具、常量、枚举、异常、注解
② share-common-redis →  Redis 相关操作类
③ share-common-security → 鉴权、安全相关类
④ share-api-xxx      →  Feign 接口、DTO 类
⑤ 目标模块自身       →  本模块已有的工具/基类
⑥ 以上都不存在       →  新建，放入最合适的模块
```

**操作步骤：**

1. **查目录**：看 `share-common/share-common-core/src/main/java/com/share/common/core/` 下有没有现成的包（`utils/`、`constant/`、`enums/`、`exception/`、`annotation/`、`config/`、`domain/`）
2. **查文件名**：搜索 `*Util*`、`*Constant*`、`*Enum*`、`*Exception*` 关键词
3. **确认有则用**：直接 import，不要 copy 代码（避免多份副本）
4. **确认无则建**：
   - 跨模块通用的 → `share-common-core` 对应包
   - 模块内专用的 → 本模块的 `utils/` 或 `constant/` 包
   - Feign 接口相关 → `share-api-xxx` 模块

**禁止行为：**
- 不查就新建工具类 —— 导致 StringUtils、DateUtils 存在多份
- 在业务模块里写通用工具方法 —— 应该放在 share-common-core
- 在 Controller/Service 里硬编码常量 —— 应该抽取到 constant 包
- 重复定义已有枚举 —— 如 UserStatus 已在 share-common-core.enums

已有类目录速查详见 `AGENTS.md`「框架类速查」章节。

---

> **文档生成日期**: 2026-06-17
> **生成方式**: 基于项目源码自动分析
