<div align="center">
  <h1>🧠 TinyBrain</h1>
  <p><strong>Personal AI Knowledge Engine</strong></p>
  <p>Spring Boot 3.x · RAG · Agent · Microservices · Observability</p>

  <p>
    <a href="./LICENSE">
      <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" />
    </a>
    <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen" alt="Spring Boot 3.2" />
    <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blueviolet" alt="Spring Cloud 2023" />
  </p>

  <p>
    <a href="#introduction">English</a> ·
    <a href="#项目介绍">中文</a>
  </p>

  <br>
</div>

---

<a id="introduction"></a>
## 📖 Introduction

**TinyBrain** is a personal AI knowledge engine built with **Spring Boot 3.x** and **Spring Cloud Alibaba**. It integrates **RAG (Retrieval-Augmented Generation)** search and autonomous **Agent** capabilities, allowing you to build a personalized knowledge base and interact with it through natural language.

<div align="center">

| `v1-handcrafted` 🖐️ ← **当前分支** | `v2-spring-ai-alibaba` 🏗️ |
|:---:|:---:|
| 手写 AI 层（WebClient + IVF + 自定义 Agent 引擎） | Spring AI Alibaba 框架集成 |
| [GitHub](https://github.com/lisusuyeye/tinybrain/tree/v1-handcrafted) | [GitHub](https://github.com/lisusuyeye/tinybrain/tree/v2-spring-ai-alibaba) |

</div>

### ✨ Key Features

| Feature | Description |
|---------|-------------|
| 🔐 **Authentication** | JWT-based auth with Spring Security, RBAC role management |
| 📚 **Knowledge Base** | Document CRUD with MyBatis-Plus, Markdown/text support, file upload |
| 🔍 **RAG Search** | Document chunking → vectorization → semantic search → LLM-augmented answering |
| 🤖 **AI Agent** | Function Calling (手写 AgentEngine + JSON Schema 解析), tool plugins (calculator, datetime, knowledge search, web search) |
| 🚪 **API Gateway** | Spring Cloud Gateway, JWT global filter, routing, CORS |
| 📊 **Observability** | Prometheus + Grafana dashboards, Zipkin distributed tracing, structured JSON logging |
| 🐳 **Docker** | One-command `docker-compose up` for full stack deployment |
| 🔄 **CI/CD** | GitHub Actions pipeline with build, test, Docker image |
| 🛡️ **Resilience** | Circuit breaker, retry, rate limiting (Resilience4j) |

### 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Clients (Swagger UI / Vue Frontend / curl)                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  API Gateway: Spring Cloud Gateway                          │
│     JWT Auth · Routing · Rate Limiting · CORS               │
└──────┬─────────┬─────────┬─────────┬────────────────────────┘
       │         │         │         │
┌──────▼──┐ ┌───▼────┐ ┌──▼────┐ ┌─▼────────┐
│  User   │ │Knowledge│ │ RAG   │ │  Agent   │
│ Service │ │ Service │ │Service│ │ Service  │
│         │ │         │ │      │ │          │
│ JWT Auth│ │Doc CRUD │ │Vector│ │Function  │
│ RBAC    │ │Upload   │ │Search│ │Calling   │
│         │ │    │    │ │LLM   │ │Plugins   │
└──┬──────┘ └──┬─────┘ └──┬────┘ └──┬───────┘
   │           │          │         │
   └───────────┴──────────┴─────────┘
               │
     ┌─────────▼──────────────┐
     │  Common Module          │
     │  Response · Exception   │
     │  Config · Utilities     │
     └────────────────────────┘
```

### 🛠 Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Java 17 (Virtual Threads, Records, Pattern Matching) |
| **Framework** | Spring Boot 3.2.5, Spring Cloud 2023.0.3, Spring Cloud Alibaba |
| **ORM** | MyBatis-Plus 3.5.7 |
| **Database** | MySQL 8 (production), H2 (dev) |
| **Auth** | JWT (jjwt 0.12.5), Spring Security, BCrypt |
| **AI** | DeepSeek API / OpenAI API / Ollama (OpenAI-compatible) |
| **Vector Store** | In-memory + JSON file persistence (pluggable for ChromaDB/Milvus) |
| **Gateway** | Spring Cloud Gateway (WebFlux, reactive) |
| **Fault Tolerance** | Resilience4j (Circuit Breaker, Retry, Rate Limiter) |
| **Observability** | Micrometer + Prometheus + Grafana + Zipkin |
| **Logging** | Logback + Logstash JSON encoder + MDC traceId |
| **API Docs** | SpringDoc OpenAPI 2.6 (Swagger UI) |
| **Frontend** | Vue 3 + TypeScript + Element Plus (optional) |
| **Container** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions |

---

<a id="project-intro"></a>
## 📖 项目介绍

**TinyBrain** 是一个基于 **Spring Boot 3.x** + **Spring Cloud Alibaba** 的个人 AI 知识引擎，集成了 **RAG 检索增强生成**和 **Agent 智能体**能力。

### ✨ 功能特性

- 🔐 **JWT 认证** — Spring Security + RBAC 权限管理
- 📚 **知识库管理** — 文档 CRUD、Markdown/文本支持、文件上传
- 🔍 **RAG 智能问答** — 文档分块 → 向量化 → 语义检索 → LLM 增强回答
- 🤖 **AI Agent** — Function Calling 工具调用（计算器、时间日期、知识搜索、网络搜索）
- 🚪 **API 网关** — Spring Cloud Gateway、JWT 全局鉴权、路由转发、CORS
- 📊 **可观测性** — Prometheus + Grafana 大盘、Zipkin 链路追踪、JSON 结构化日志
- 🐳 **Docker 部署** — `docker-compose up` 一键启动全套服务
- 🔄 **CI/CD** — GitHub Actions 流水线（构建、测试、Docker 镜像）
- 🛡️ **容错保护** — Resilience4j 熔断、重试、限流
- 🧪 **Ollama 支持** — 可接入本地大模型，无需 API Key

### 🏗 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│  客户端 (Swagger UI / Vue 前端 / curl)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  API 网关: Spring Cloud Gateway                              │
│     JWT 鉴权 · 路由转发 · 限流 · CORS                        │
└──────┬─────────┬─────────┬─────────┬────────────────────────┘
       │         │         │         │
┌──────▼──┐ ┌───▼────┐ ┌──▼────┐ ┌─▼────────┐
│ 用户模块 │ │知识库模块│ │RAG模块│ │Agent模块  │
│ JWT认证  │ │文档CRUD │ │向量化 │ │Function  │
│ RBAC权限 │ │文件上传 │ │语义检索│ │Calling   │
│         │ │        │ │LLM增强│ │工具插件  │
└──┬──────┘ └──┬─────┘ └──┬────┘ └──┬───────┘
   │           │          │         │
   └───────────┴──────────┴─────────┘
               │
     ┌─────────▼──────────────┐
     │  基础模块: tinybrain-common │
     │  统一响应 · 异常处理 · 工具类 │
     └────────────────────────┘
```

### 🛠 技术栈

| 领域 | 技术 |
|------|------|
| **语言** | Java 17 |
| **框架** | Spring Boot 3.2.5, Spring Cloud 2023.0.3, Spring Cloud Alibaba |
| **ORM** | MyBatis-Plus 3.5.7 |
| **数据库** | MySQL 8 (生产), H2 (开发) |
| **认证** | JWT (jjwt 0.12.5), Spring Security, BCrypt |
| **AI** | DeepSeek / OpenAI / Ollama 兼容 API |
| **向量存储** | 内存 + JSON 文件持久化（可替换为 ChromaDB/Milvus） |
| **网关** | Spring Cloud Gateway (WebFlux) |
| **容错** | Resilience4j (熔断、重试、限流) |
| **可观测** | Micrometer + Prometheus + Grafana + Zipkin |
| **日志** | Logback + Logstash JSON + MDC traceId |
| **前端** | Vue 3 + TypeScript + Element Plus |
| **部署** | Docker Compose |
| **CI/CD** | GitHub Actions |

---

## 🔀 双版本策略

本项目维护**两个并行分支**，展示同一业务框架下两种 AI 集成方式：

| | v1-handcrafted 🖐️ ← **当前** | v2-spring-ai-alibaba 🏗️ |
|---|---|---|
| **AI 层实现** | 手写 WebClient → LLM API（深度理解 HTTP 通信细节） | Spring AI Alibaba (ChatClient + EmbeddingModel) |
| **向量检索** | 手写 IVF 倒排索引（K-means 聚类 + 多桶探测 + 暴力搜索融合） | VectorStoreWrapper（预留 Spring AI VectorStore 升级） |
| **Function Calling** | 手写 JSON Schema + AgentEngine 循环 + 嵌套 JSON 解析 | `@Tool` 注解 + ChatClient 自动调度 |
| **重试/熔断** | Resilience4j 熔断 + 重试 + 限流（手配阈值参数） | Spring AI 自动重试 |
| **LLM 供应商** | DeepSeek / OpenAI / Ollama（OpenAI 兼容 API） | DashScope（阿里通义千问） |
| **VectorStore 增强** | IVF 索引 + `IvfIndex` 类 + `DocumentIndexingService` 异步索引 | 等量功能，架构简化 |
| **流式响应** | SSE 流式输出（`RAGStreamController`） | 同 v1，流式可选 |


---

## 🚀 Quick Start / 快速开始

### Prerequisites / 前提条件

- JDK 17+
- Maven 3.9+
- Docker & Docker Compose (for containerized deployment)
- LLM API Key (DeepSeek / OpenAI, or Ollama for local)

### Development Mode / 开发模式

```bash
# 1. Clone
git clone https://github.com/yourusername/tinybrain.git
cd tinybrain

# 2. Build
mvn clean install -DskipTests

# 3. Set LLM API Key
export TINYBRAIN_LLM_KEY=sk-your-api-key-here

# 4. Start backend (H2 in-memory database, no MySQL needed)
cd tinybrain-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. Start frontend (optional)
cd ../tinybrain-ui
npm install
npm run dev
```

### Docker Deployment / Docker 部署

```bash
# Start all services (MySQL + ES + Redis + Prometheus + Grafana + Zipkin + App)
docker-compose up -d

# Set LLM API Key
export TINYBRAIN_LLM_KEY=sk-your-api-key-here
docker-compose up -d
```

### Ollama Local Mode / 本地模型模式

```bash
# Install Ollama: https://ollama.ai
ollama pull qwen2.5:7b
ollama pull nomic-embed-text

# Start TinyBrain with Ollama profile
export SPRING_PROFILES_ACTIVE=ollama
mvn spring-boot:run
```

### Access / 访问地址

| Service | URL |
|---------|-----|
| API Docs (Swagger) | http://localhost:8080/swagger-ui.html |
| Vue Frontend | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Zipkin | http://localhost:9411 |
| MySQL | localhost:3306 |

---

## 📚 API Overview / API 概览

| Module | Method | Path | Description |
|--------|--------|------|-------------|
| Auth | POST | `/api/auth/register` | Register |
| Auth | POST | `/api/auth/login` | Login → JWT Token |
| Auth | GET | `/api/auth/me` | Current user info |
| Documents | POST | `/api/documents` | Create document |
| Documents | POST | `/api/documents/upload` | Upload file |
| Documents | GET | `/api/documents` | List documents |
| Documents | GET | `/api/documents/{id}` | Get detail |
| RAG | POST | `/api/rag/index/{id}` | Index document → vector |
| RAG | GET | `/api/rag/ask` | RAG question answering |
| Agent | POST | `/api/agent/chat` | Agent conversation |
| Agent | GET | `/api/agent/tools` | List available tools |

---

## 📁 Project Structure / 项目结构

```
tinybrain/
├── tinybrain-app          # Bootable entry point (Spring Boot)
├── tinybrain-gateway      # API Gateway (Spring Cloud Gateway)
├── tinybrain-common       # Common module (config, exception, utilities)
├── tinybrain-user         # User module (auth, JWT, RBAC)
├── tinybrain-knowledge    # Knowledge base (document CRUD)
├── tinybrain-rag          # RAG module (vectorization, semantic search, LLM)
├── tinybrain-agent        # Agent module (Function Calling, tool plugins)
├── tinybrain-ui           # Vue 3 frontend (optional)
├── docs/                  # Educational documentation
├── deploy/                # Deployment configs (Prometheus, Grafana)
├── docker-compose.yml     # Docker Compose deployment
├── Dockerfile             # Multi-stage Docker build
└── .github/workflows/     # CI/CD pipeline
```

---

## 📊 Observability / 可观测性

The project includes full observability stack:

### Metrics
- **JVM**: Memory, GC, threads, class loading
- **Business**: RAG latency, document index count, Agent conversation count
- **HTTP**: Request rate, latency, error rate
- **Custom**: Micrometer @Timed annotations

### Tracing
- Micrometer Tracing + Brave (Zipkin)
- Auto-inject traceId to MDC for log correlation
- W3C traceparent header propagation

### Logging
- Dev: Colorful console output
- Docker: JSON format
- Production: JSON file + log rotation

---

## 🧪 Testing / 测试

```bash
# Run unit tests
mvn test

# Run specific module
mvn test -pl tinybrain-rag -am
```

---

## 🤝 Contributing / 贡献

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md).

---

## 📄 License / 许可证

This project is licensed under the Apache License 2.0 - see the [LICENSE](./LICENSE) file for details.

---

## 📈 Project Goals / 项目目标

TinyBrain is designed as a **comprehensive portfolio project** for Java backend developers, covering:

- Java 17 features (Records, Pattern Matching, Virtual Threads, Sealed Classes)
- Spring Boot 3.x auto-configuration, AOP, event-driven architecture
- MyBatis-Plus ORM, pagination, auto-fill, logical delete
- JWT authentication, Spring Security, RBAC
- RAG: Document chunking, vector embedding, semantic search, LLM prompting
- Agent: Function Calling architecture, tool plugin system, multi-turn conversation
- Spring Cloud Gateway, Nacos service discovery
- Observability: Prometheus, Grafana, Zipkin, structured logging
- Containerization: Docker, multi-stage builds, health checks
- CI/CD: GitHub Actions
- Fault tolerance: Resilience4j circuit breaker, retry, rate limiting

> 💡 Educational documentation is available in the `docs/` directory!
