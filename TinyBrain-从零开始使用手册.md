# TinyBrain 从零开始使用手册

> 目标读者：完全不懂这个项目的人
> 读完本文后：你能启动项目、理解它在干啥、知道数据库在哪、会用基本功能

---

[TOC]

---

## 一、这个项目到底是什么？

**一句话：** 一个把「你自己的笔记/文档」变成「AI 知识库」的工具。

**举个例子：**
1. 你写了一篇笔记《Spring 事务详解》
2. 把这篇笔记导入 TinyBrain
3. 然后你可以问：「Spring 事务的传播机制有哪些？」
4. AI 会**基于你的笔记内容**来回答，而不是去网上瞎搜

**它不是：**
- ❌ 不是 ChatGPT 网页版（只能基于你自己的知识回答）
- ❌ 不是数据库管理工具
- ❌ 不是前端网站（虽然有个前端界面）

---

## 二、先搞懂项目结构（很重要）

拿到项目后，第一件事是看懂文件夹是干嘛的：

```
tinybrain/                          # ← 这是项目根目录
│
├── tinybrain-app/                  # 🔵 启动入口（你只需要运行这个）
│   └── src/main/resources/
│       ├── application.yml         # 主配置文件
│       ├── application-dev.yml     # 开发环境配置（用 H2 内存数据库）
│       ├── application-docker.yml  # Docker 环境配置（用 MySQL）
│       ├── application-prod.yml    # 生产环境配置
│       ├── application-ollama.yml  # 本地模型配置
│       └── sql/schema.sql          # ★★★ 数据库表结构（建表语句在这里）
│
├── tinybrain-common/               # 公共工具（不用管）
├── tinybrain-user/                 # 用户模块（登录注册逻辑）
├── tinybrain-knowledge/            # 知识库模块（文档增删改查）
├── tinybrain-rag/                  # 🔵 RAG问答核心（最重要的模块）
│   ├── service/RAGService.java     # RAG 问答逻辑
│   ├── service/LLMApiClient.java   # 调用 AI 接口
│   └── vector/VectorStore.java     # ★★★ 向量数据库（存你的文档向量）
├── tinybrain-agent/                # AI Agent 智能体
├── tinybrain-gateway/              # API 网关
├── tinybrain-ui/                   # 🖥️ 前端界面（Vue3 写的网页）
│
├── docker-compose.yml              # Docker 一键启动配置
├── Dockerfile                      # Docker 镜像构建文件
├── pom.xml                         # Maven 项目配置文件（所有依赖在这里）
│
├── TinyBrain-从零开始使用手册.md      # ◀ 就是你现在看的这个
│
└── docs/                           # 纯技术文档
```

---

## 三、数据库到底在哪里？（最绕的问题）

这是新手最困惑的地方，我来彻底讲清楚。

### 场景一：你用 dev 模式启动 → 数据库在「内存里」

```
启动方式: mvn spring-boot:run -Dspring-boot.run.profiles=dev
数据库:   H2（一种纯 Java 的内存数据库）
数据位置: Java 进程的内存中（电脑上没有任何文件）
持久化:   不持久，重启就没了
怎么查看: 打开 http://localhost:8080/h2-console
          JDBC URL: jdbc:h2:mem:tinybrain
          账号: sa
          密码: （空的，不用填）
```

> 这就是为什么你在电脑上找不到 MySQL、找不到数据库文件——因为 dev 模式根本不需要 MySQL，它跑在内存里。
> 重启后数据丢失，是因为内存断电就清空了。

### 场景二：你用 docker 模式启动 → 数据库在「Docker 容器里」

```
启动方式: docker-compose up -d
数据库:   MySQL 8（运行在 Docker 容器中）
数据位置: Docker Volume（mysql-data）
持久化:   重启不丢数据
怎么连接: 用 Navicat/DataGrip 连接 localhost:3306
          账号: root
          密码: tinybrain2026
          数据库: tinybrain
```

### 场景三：向量数据存在哪里？

除了常规数据库（存用户名、密码、文档标题等），还有「向量数据」：

```
向量数据: 存的是「文档的数学向量表示」（一堆浮点数）
开发模式: 存在 ./data/vectorstore/vectors.json
Docker模式: 存在 Docker Volume 里
作用: AI 靠这些向量来「理解」你的文档内容
```

### 总结表格

| 你的问题 | dev 模式 | docker 模式 |
|---------|---------|------------|
| 数据库在哪 | 内存里 | Docker MySQL 容器 |
| 数据持久吗 | ❌ 重启丢失 | ✅ 永久保存 |
| 需要安装 MySQL 吗 | ❌ 不需要 | ✅ 需要 Docker |
| 表结构在哪 | schema.sql | schema.sql |
| 向量数据在哪 | vectors.json 文件 | Docker Volume |
| 怎么连数据库看数据 | h2-console | Navicat/DataGrip |

---

## 四、从零开始启动（手把手）

### 第1步：确认环境

打开终端（CMD 或 PowerShell），输入：

```bash
java -version
# 必须有 Java 17 或以上
# 如果没有 → 去 https://adoptium.net 下载 JDK 17

mvn -version
# 必须有 Maven 3.9 或以上
# 如果没有 → 去 https://maven.apache.org/download.cgi 下载
```

### 第2步：第一次编译（下载依赖）

```bash
# 进入项目根目录
cd tinybrain

# 编译（第一次会下载几十个依赖包，可能需要 3-5 分钟）
# 如果失败，可能是网络问题，多试几次
mvn clean install -DskipTests
```

看到 `BUILD SUCCESS` 就说明编译成功了。

### 第3步：启动后端

```bash
# 进入启动模块
cd tinybrain-app

# 启动（dev 模式，用 H2 内存数据库，不需要 MySQL）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

看到类似这样的日志说明启动成功：
```
Started TinyBrainApplication in 15.789 seconds
```

**验证启动：** 打开浏览器访问 http://localhost:8080/actuator/health

应该看到：
```json
{"status":"UP"}
```

### 第4步：查看 API 文档（验证一切正常）

打开 http://localhost:8080/swagger-ui.html

你会看到一个网页，列出了所有可用的 API 接口。比如：
- auth-controller：登录注册
- document-controller：文档管理
- rag-controller：RAG 问答
- agent-controller：Agent 智能体

### 第5步（可选）：启动前端

如果需要图形界面，新开一个终端：

```bash
cd tinybrain-ui
npm install
npm run dev
```

然后打开 http://localhost:3000

---

## 五、配置 AI 能力（否则问答功能不可用）

TinyBrain 的 RAG 问答需要一个 AI 接口。有两种方式：

### 方式一：用 DeepSeek（推荐，注册送额度）

1. 访问 https://platform.deepseek.com 注册账号
2. 获取 API Key
3. 在启动后端前设置环境变量：

```bash
# Windows PowerShell
$env:TINYBRAIN_LLM_KEY="sk-你的key"

# 然后启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 方式二：用 Ollama 本地模型（免费，不需要 Key）

1. 安装 Ollama：https://ollama.ai
2. 拉取模型（在终端执行）：
   ```bash
   ollama pull qwen2.5:7b
   ollama pull nomic-embed-text
   ```
3. 启动应用：
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=ollama
   ```

---

## 六、完整功能使用流程（照着做）

假设你已经启动成功了后端，按这个顺序体验：

### 6.1 注册账号

打开 Swagger UI → 找到 `auth-controller` → `/api/auth/register`

填入：
```json
{
  "username": "admin",
  "password": "123456"
}
```

点击 Execute。返回 `200` 说明注册成功。

### 6.2 登录

找到 `/api/auth/login`，填入同样的账号密码。

返回结果里找到 `token` 字段，复制它。这个 token 就是你的「身份证」，
后续所有操作都需要在请求头里带上它。

### 6.3 在 Swagger UI 里设置 Token

在 Swagger 页面右上角点击 **Authorize** 按钮。
在弹出的框里输入：`Bearer 你刚才复制的token`
点击 Authorize 关闭。

这样后面所有请求都会自动带上这个 Token。

### 6.4 创建文档

找到 `document-controller` → `POST /api/documents`

填入：
```json
{
  "title": "Spring 事务",
  "content": "Spring 事务的传播机制有七种：\n1. REQUIRED：默认，有事务则加入，没有则新建\n2. REQUIRES_NEW：必须新建事务\n3. NESTED：嵌套事务\n4. SUPPORTS：有则用，没有不用\n5. NOT_SUPPORTED：不支持事务\n6. MANDATORY：必须有事务\n7. NEVER：必须没有事务\n\n隔离级别：READ_UNCOMMITTED、READ_COMMITTED、REPEATABLE_READ、SERIALIZABLE",
  "contentType": "markdown"
}
```

返回结果里记下文档的 `id`（应该是 1）。

### 6.5 索引文档（关键！不做这步就查不到）

找到 `rag-controller` → `POST /api/rag/index/{documentId}`

把上面得到的 id 填进去（比如 1），执行。

这一步会：
1. 把文档切成小块（分块）
2. 每块转成数学向量（向量化）
3. 存入向量库

> 如果不做这一步，后面问答的时候就查不到任何内容。

### 6.6 RAG 问答

找到 `rag-controller` → `GET /api/rag/ask`

填入参数：
- question：`Spring事务的传播机制有哪些`
- topK：`5`

执行。如果 API Key 配置正确，会返回 AI 基于你的文档生成的回答。

### 6.7 Agent 对话

找到 `agent-controller` → `POST /api/agent/chat`

填入：
```json
{
  "message": "今天几号？"
}
```

Agent 会调用 `DateTimeTool` 工具获取当前时间后回答。

再试试：
```json
{
  "message": "计算 (15 + 3) * 2 / 4 等于多少"
}
```

Agent 会调用 `CalculatorTool` 计算。

---

## 七、如果遇到问题

### 启动报错

| 错误信息 | 原因 | 解决 |
|---------|------|------|
| `Address already in use: bind` | 8080 端口被占了 | 关掉其他程序，或改端口 |
| `Java 17 required` | JDK 版本太低 | 安装 JDK 17+ |
| `Failed to read artifact descriptor` | Maven 依赖下载失败 | 重试，或换网络 |
| `Table 'xxx' not found` | 数据库表没建 | 检查 dev 模式是否激活 |
| `Redis connection failed` | 没装 Redis | dev 模式不需要 Redis，忽略 |

### 使用报错

| 现象 | 原因 | 解决 |
|-----|------|------|
| 登录返回 401 | Token 不对或过期 | 重新登录拿新 Token |
| 创建文档返回 403 | 没带 Token | 检查 Authorize 设置 |
| RAG 问答返回"未检索到" | 没索引文档 | 先调 `/api/rag/index/{id}` |
| RAG 问答返回空/报错 | 没配 API Key | 设置 TINYBRAIN_LLM_KEY |
| Swagger 页面打不开 | 后端没启动 | 检查终端日志 |

---

## 八、一些重要概念的解释

### Spring Boot 是什么？
Java 的一个框架，让你能快速搭建 Web 服务。TinyBrain 就是用这个写的。

### Maven 是什么？
Java 的「包管理工具」，类似前端的 npm。项目用到的所有第三方代码（依赖）都由 Maven 管理。

### H2 是什么？
一个用 Java 写的内存数据库。开发时不需要安装 MySQL 就能跑起来。数据存在内存里，重启就没了。

### 什么是「向量」？
AI 不能直接理解文字，它理解的是「数字」。向量就是把一段文字变成一串数字（比如 [0.12, -0.34, 0.56, ...]）。
两段文字如果意思相近，它们的向量也会比较接近。

### 什么是「索引」？
把你的文档切块 → 每块转成向量 → 存到向量库。这个过程叫索引。
你可以理解为「让 AI 先把你的文档读一遍并记住」。

### Profile 是什么？
Spring Boot 的配置分组。不同的 profile 用不同的配置：
- `dev`：开发用，H2 内存库，SQL 日志全打印
- `docker`：部署用，MySQL + Redis
- `prod`：生产用，更安全严格
- `ollama`：本地模型用

---

## 九、总结

这个项目本质上就是：

```
你写的文档 → 分块 → 转成向量 → 存起来
                              ↓
你问问题 → 转成向量 → 在库里找相似的 → 拼成提示词 → 问 AI → 给你回答
```

**记住三点：**
1. **dev 模式**不需要任何外部服务（MySQL、Redis 都不用），开箱即用
2. **数据库在内存里**，重启就没了
3. **必须先索引文档**，不然 RAG 问答查不到任何东西

---

*遇到问题请参考 [docs/](./docs/) 目录下的技术文档，或提交 GitHub Issue。*
