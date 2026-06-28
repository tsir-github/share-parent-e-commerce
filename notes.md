# 开发工具备忘

## MCP 服务器（可接入 OpenCode）

### mcp-spring-boot-actuator
Spring Boot Actuator 分析工具。可直接查询运行中微服务的健康、指标、配置、Bean、启动耗时、缓存、日志级别。
- https://github.com/Dmitriusan/mcp-spring-boot-actuator
- 安装：npx / docker
- 用途：排查启动失败、性能瓶颈、配置安全风险

### mcp-jdwp-java
Java 远程调试工具，通过 JDWP 协议 attach 到运行中的 JVM。可查看运行时变量、设置断点、执行表达式。
- https://github.com/FgForrest/mcp-jdwp-java
- 安装：npx / docker
- 用途：无需重启服务即可查运行时状态

### ariadne（whyy9527/ariadne）
微服务 API 依赖图工具，扫描 Spring Boot + TypeScript 项目的 REST/GraphQL/Kafka 端点，生成跨服务调用链。
- https://github.com/whyy9527/ariadne
- 安装：pip install ariadne-mcp
- 用途：微服务多了以后梳理服务间调用关系

### api2mcp4j（TheEterna/api2mcp4j）
Spring Boot Starter，自动将现有 @RestController 暴露为 MCP Tool，零代码改动。
- https://github.com/TheEterna/api2mcp4j
- 安装：Maven 依赖
- 用途：让 AI 直接调项目已有的 API

### log-mcp-server（sheikhBasit/log-mcp-server）
日志分析 MCP：tail、search、filter、summarize，支持 Docker 容器日志。
- https://github.com/sheikhBasit/log-mcp-server
- 安装：pip install

### mcp-log-analyzer（Arun07AK/mcp-log-analyzer）
进阶日志分析：自动检测格式、错误分类、异常检测、时间线分析。
- https://github.com/Arun07AK/mcp-log-analyzer
- 安装：pip install / uvx

### mcp-tailserver（adam-palmer1/mcp-tailserver）
网络可访问的日志 tail 服务，支持多客户端同时读日志，实时 SSE 推送。
- https://github.com/adam-palmer1/mcp-tailserver
- 安装：pip install

### thhart/log-mcp
多目录日志监控 MCP，支持正则搜索、分页读取大文件。
- https://github.com/thhart/log-mcp
- 安装：pip install

### kascada/logmcp
远程 Linux 服务器日志读取，只读 + token 鉴权 + 审计，支持 systemd journal。
- https://github.com/kascada/logmcp
- 安装：pip install

## 日志平台（适合上生产后考虑）

### Plumelog
国产分布式日志系统，无代码侵入，支持百亿级别日志，自动关联调用链。
- https://gitee.com/sheepedu/plumelog

### 秒查（Miaocha）
基于 Apache Doris 的企业级日志检索平台，毫秒级查询。
- https://github.com/Hinadt-Inc/miaocha

## 其他

### SkyWalking
Apache 分布式链路追踪，Java agent 无侵入挂载，生产环境查调用链路和性能。
