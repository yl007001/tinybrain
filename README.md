# 🧠 TinyBrain — 个人 AI 知识引擎

> 一个 Spring Boot 3.x + Spring Cloud Alibaba 驱动的个人 AI 知识管理平台。
> 集成 RAG 检索增强生成与自主 Agent 能力。

<div align="center">

| `v1-handcrafted` | `v2-spring-ai-alibaba` ← **当前分支** |
|:---:|:---:|
| 🖐️ 手写 AI 层 | 🏗️ Spring AI Alibaba 框架集成 |
| [查看分支](https://gitee.com/lisusuyeye/personal-ai-knowledge-engine/tree/v1-handcrafted/) | 你在这里 |

</div>

## 📋 项目信息

| 项目 | 内容 |
|------|------|
| **目标** | 构建个人知识库 + AI 问答系统 |
| **技术栈** | Java 17, Spring Boot 3.2, Spring Cloud 2023, MyBatis-Plus, MySQL, Elasticsearch |
| **AI** | RAG + Agent + Spring AI Alibaba (DashScope 通义千问) |
| **部署** | Docker Compose (MySQL + ES + Prometheus + Grafana + Zipkin) |
| **仓库** | [Gitee](https://gitee.com/lisusuyeye/personal-ai-knowledge-engine) |

## 🔀 双版本策略

本项目维护两个并行分支，展示同一业务框架下的两种 AI 集成方式：

| | v1-handcrafted 🖐️ | v2-spring-ai-alibaba 🏗️ ← **当前** |
|---|---|---|
| **AI 层实现** | 手写 WebClient 直调 LLM API | Spring AI Alibaba 框架 (ChatClient / EmbeddingModel) |
| **向量检索** | 手写 IVF 倒排索引 + 余弦相似度 | VectorStoreWrapper（预留 Spring AI VectorStore 升级） |
| **Function Calling** | 手写 JSON Schema + AgentEngine 调度引擎 | `@Tool` 注解 + ChatClient 自动调度 |
| **重试/熔断** | Resilience4j 手配 | Spring AI 自动重试 |
| **供应商** | DeepSeek / OpenAI / Ollama | DashScope (通义千问) — 可扩展 |

## 🏗 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│ 客户端 (Swagger UI / curl / 前端)                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ API 网关: tinybrain-gateway (Spring Cloud Gateway)           │
│     JWT 鉴权 · 路由转发 · 限流 · 跨域                        │
└──────┬─────────┬─────────┬─────────┬────────────────────────┘
       │         │         │         │
┌──────▼──┐ ┌───▼────┐ ┌──▼────┐ ┌─▼────────┐
│ 用户服务 │ │知识库服务│ │RAG服务│ │Agent服务  │
│ tinybrain│ │tinybrain│ │tinybrain│ │tinybrain │
│ -user   │ │-know-  │ │-rag   │ │-agent    │
│         │ │ledge   │ │       │ │          │
│ JWT认证 │ │文档CRUD │ │向量化 │ │Function  │
│ RBAC权限│ │ES全文搜 │ │语义检索│ │Calling   │
│         │ │索      │ │LLM增强│ │工具插件  │
└──┬──────┘ └──┬─────┘ └──┬────┘ └──┬───────┘
   │           │          │         │
   └───────────┴──────────┴─────────┘
               │
     ┌─────────▼──────────────┐
     │ 基础模块: tinybrain-common │
     │ 统一响应 · 异常处理 · 工具类 │
     └────────────────────────┘
```

## 🔧 技术栈

| 领域 | 选型 | 说明 |
|------|------|------|
| **框架** | Spring Boot 3.2 | 自动配置、AOP、事件驱动 |
| **微服务** | Spring Cloud 2023 + Nacos | 服务注册发现、配置中心 |
| **网关** | Spring Cloud Gateway | 路由转发、JWT 鉴权、限流 |
| **ORM** | MyBatis-Plus 3.5 | 分页、自动填充、逻辑删除 |
| **数据库** | MySQL 8 + H2 (dev) | 主业务数据库 |
| **搜索引擎** | Elasticsearch 7.17 | 全文检索 |
| **AI API** | Spring AI Alibaba (DashScope 通义千问) | ChatClient + EmbeddingModel 统一抽象 |
| **文档** | SpringDoc OpenAPI 2.6 | Swagger UI 自动生成 |
| **监控** | Micrometer + Prometheus + Grafana | JVM/业务指标大盘 |
| **追踪** | Micrometer Tracing + Zipkin | 分布式链路追踪 |
| **日志** | Logback + Logstash Encoder + ELK | 结构化 JSON 日志 |
| **部署** | Docker Compose | 一键启动所有服务 |

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+（开发环境可用 H2 内存库替代）
- Elasticsearch 7.x（可选，不影响核心功能）

### 2. 开发环境启动

```bash
# 编译
mvn clean install -DskipTests

# 启动（默认使用 H2 内存数据库，无需安装 MySQL）
cd tinybrain-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Docker 环境启动

```bash
# 一键启动所有服务（MySQL + ES + Redis + Prometheus + Grafana + Zipkin + App + Gateway）
docker-compose up -d

# 查看日志
docker-compose logs -f tinybrain-app

# 停止
docker-compose down -v
```

### 4. 访问地址

| 服务 | 地址 |
|------|------|
| API 文档 (Swagger UI) | http://localhost:8080/swagger-ui.html |
| API 网关 | http://localhost:8088 |
| Prometheus | http://localhost:9090 |
| Grafana (admin/admin) | http://localhost:3000 |
| Zipkin | http://localhost:9411 |

### 5. 配置 LLM API

**v2-spring-ai-alibaba（当前分支）** 使用 Spring AI Alibaba DashScope：

```bash
# 设置 DashScope API Key（阿里云通义千问）
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

或在 `application.yml` 中修改：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY:sk-your-api-key}
      chat:
        options:
          model: qwen-plus
      embedding:
        options:
          model: text-embedding-v2
```

> **切换到 v1-handcrafted 分支**时，配置方式不同：
> ```bash
> export TINYBRAIN_LLM_KEY=sk-your-api-key
> ```
> 详见 v1-handcrafted 分支的 README。

## 📚 API 概览

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户认证 | `POST /api/auth/register` | 注册 |
| 用户认证 | `POST /api/auth/login` | 登录（返回 JWT） |
| 用户认证 | `GET /api/auth/me` | 当前用户信息 |
| 知识库 | `POST /api/documents` | 创建文档 |
| 知识库 | `GET /api/documents` | 分页查询 |
| 知识库 | `GET /api/documents/{id}` | 文档详情 |
| RAG | `GET /api/rag/ask` | RAG 智能问答 |
| RAG | `POST /api/rag/index/{id}` | 索引文档 |
| Agent | `POST /api/agent/chat` | Agent 对话 |
| Agent | `GET /api/agent/tools` | 工具列表 |

完整 API 文档：启动后访问 [Swagger UI](http://localhost:8080/swagger-ui.html)

## 📊 监控与可观测

### 指标采集
- **JVM 指标**: 内存、GC、线程、类加载
- **业务指标**: RAG 问答耗时、文档索引次数、Agent 对话数
- **HTTP 指标**: 请求量、延迟、错误率
- **自定义指标**: Micrometer @Timed 注解

### 链路追踪
- 基于 Micrometer Tracing + Brave (Zipkin)
- 自动注入 traceId 到 MDC，关联日志
- 跨服务传递 W3C traceparent 头

### 日志
- 开发环境: 控制台彩色输出
- Docker: JSON 格式到控制台
- 生产环境: JSON 文件 + ELK 采集

## 🧪 测试

```bash
# 运行单元测试
mvn test

# 运行指定模块测试
mvn test -pl tinybrain-common,tinybrain-rag,tinybrain-agent
```

## 📐 项目结构

```
tinybrain/
├── tinybrain-app          # 启动入口 (单体模式)
├── tinybrain-gateway      # API 网关
├── tinybrain-common       # 公共模块 (工具类、配置、异常)
├── tinybrain-user         # 用户模块 (认证、权限)
├── tinybrain-knowledge    # 知识库模块 (文档 CRUD、全文检索)
├── tinybrain-rag          # RAG 模块 (向量化、语义检索、LLM生成)
├── tinybrain-agent        # Agent 模块 (Function Calling、工具插件)
├── docker-compose.yml     # Docker Compose 部署
├── Dockerfile             # App Docker 镜像
└── deploy/                # 部署配置 (Prometheus、Grafana)
```

## 📈 开发路线

- ✅ Phase 1: Spring Boot 核心框架 + 统一响应 + JWT 认证
- ✅ Phase 2: RAG 检索系统（文档分块、向量检索、LLM 增强生成）
- ✅ Phase 3: Agent 智能体系统（Function Calling、工具插件）
- ✅ Phase 4: 微服务化（Gateway、Nacos 服务发现）
- ✅ Phase 5: 可观测与工程化（日志、监控、追踪、Docker、CI/CD）
- ✅ Phase 6: 项目收尾与文档
- ✅ **双版本拆分**: v1-handcrafted (手写 AI) + v2-spring-ai-alibaba (Spring AI Alibaba)

## 📝 技术文档

详见 `docs/` 目录：

- [自动配置与 AOP](./docs/phase1/01-Spring-Boot-自动配置与AOP实战.md)
- [MySQL 索引与事务](./docs/phase1/02-MySQL-索引优化与事务原理.md)
- [面试高频题](./docs/phase1/03-面试高频题精讲.md)
- [RAG 系统实现](./docs/phase1/04-RAG检索系统实现.md)
- [Agent 系统实现](./docs/phase1/05-Agent智能体系统实现.md)
- [微服务与 Spring Cloud](./docs/phase1/06-微服务化与SpringCloud.md)
- [项目亮点与简历包装](./docs/phase6/01-项目亮点与简历包装.md)
- [模拟面试题精讲](./docs/phase6/02-模拟面试题精讲.md)
- [源码深挖与高频考点](./docs/phase6/03-源码深挖与高频考点.md)
