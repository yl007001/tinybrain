# TinyBrain 测试报告

> 最后更新: 2026-06-02
> 测试环境: Java 17, Maven 3.9, Windows 11

---

## 一、测试概览

| 指标 | 数据 |
|------|------|
| 总测试数 | 108 |
| 通过 | 108 ✅ |
| 失败 | 0 |
| 错误 | 0 |
| 整体覆盖率 | 44.4% (668/1506 行) |

```
[INFO] tinybrain-common ................................... SUCCESS
[INFO] tinybrain-user ..................................... SUCCESS
[INFO] tinybrain-knowledge ................................ SUCCESS
[INFO] tinybrain-rag ...................................... SUCCESS
[INFO] tinybrain-agent .................................... SUCCESS
[INFO] tinybrain-gateway .................................. SUCCESS
[INFO] tinybrain-app ...................................... SUCCESS
[INFO] BUILD SUCCESS
```

---

## 二、各模块测试详情

### tinybrain-common (34 tests)

| 测试类 | 测试数 | 覆盖内容 |
|--------|--------|---------|
| RTest | 6 | 统一响应体 R<T> 的各种工厂方法 |
| PageResultTest | 3 | 分页结果封装 |
| BusinessExceptionTest | 7 | 业务异常工厂方法 (notFound, unauthorized, forbidden, badRequest, conflict) |
| CommonConstantTest | 2 | 常量值验证 |
| JwtUtilTest | 11 | Token 生成、解析、校验、过期、无效 Token |
| SecurityUtilTest | 5 | getCurrentUserId, getCurrentUserRole, isAdmin |

**覆盖率: 46.5%** (101/217 行)

| 关键类 | 覆盖率 |
|--------|--------|
| BusinessException | 100% |
| SecurityUtil | 100% |
| JwtUtil | 90% |
| R | 88% |
| PageResult | 65% |

### tinybrain-user (8 tests)

| 测试类 | 测试数 | 覆盖内容 |
|--------|--------|---------|
| UserServiceImplTest | 8 | 注册(成功+重复用户名)、登录(成功+密码错+用户不存在+禁用)、查询用户(成功+不存在) |

**覆盖率: 50.4%** (66/131 行)

| 关键类 | 覆盖率 |
|--------|--------|
| UserServiceImpl | 100% |
| LoginRequest | 100% |
| RegisterRequest | 100% |
| User | 73% |

### tinybrain-rag (18 tests)

| 测试类 | 测试数 | 覆盖内容 |
|--------|--------|---------|
| DocChunkStrategyTest | 7 | 分块策略: 空内容、短文本、长文本、自定义参数、段落边界 |
| RAGServiceTest | 5 | 文档索引(成功+文档不存在)、RAG问答(正常+空结果+Embedding失败) |
| VectorStoreTest | 6 | 向量插入、检索、低相似度过滤、删除、维度设置 |

**覆盖率: 33.6%** (244/727 行)

| 关键类 | 覆盖率 |
|--------|--------|
| RAGService | 81% |
| DocChunkStrategy | 91% |
| VectorStore | 单元测试覆盖 |
| IvfIndex | 单元测试覆盖 |

### tinybrain-agent (37 tests)

| 测试类 | 测试数 | 覆盖内容 |
|--------|--------|---------|
| AgentEngineTest | 8 | 工具注册、批量注册、工具定义构建、工具执行、错误处理 |
| AgentServiceTest | 5 | 直接回答、工具调用循环、最大迭代限制、null响应、会话清理 |
| CalculatorToolTest | 20 | 四则运算、括号、幂运算、函数(sqrt/sin/abs)、常量(pi)、错误处理(空表达式/除零/无效表达式)、复杂表达式、一元负号 |
| DateTimeToolTest | 4 | 工具名称、描述、参数Schema、日期时间返回格式 |

**覆盖率: 59.6%** (257/431 行)

| 关键类 | 覆盖率 |
|--------|--------|
| AgentEngine | 100% |
| AgentService | 88% |
| CalculatorTool | 90% |
| CalculatorTool.ExprParser | 82% |
| DateTimeTool | 100% |
| AgentRequest | 100% |
| AgentResponse | 100% |

### tinybrain-app (29 tests — 接口集成测试)

| 测试类 | 测试数 | 覆盖接口 |
|--------|--------|---------|
| TinyBrainApplicationTest | 1 | Spring 上下文加载验证 |
| AuthControllerTest | 7 | 登录(正确/错误密码/不存在用户)、注册(成功/重复)、获取用户(无Token/无效Token/正确) |
| DocumentControllerTest | 12 | 创建(成功/无权限/空标题)、列表(分页/关键词)、详情(成功/不存在)、删除(成功/不存在)、更新、上传(成功/空文件) |
| RAGControllerTest | 4 | 统计、问答(空问题/无Token)、索引文档 |
| AgentControllerTest | 5 | 工具列表(返回/含calculator)、对话(成功/无Token)、清除会话 |

---

## 三、覆盖率详情

### 按模块

```
模块              覆盖行数    总行数    覆盖率
─────────────────────────────────────────────
tinybrain-common    101       217      46.5%
tinybrain-user       66       131      50.4%
tinybrain-rag       244       727      33.6%
tinybrain-agent     257       431      59.6%
─────────────────────────────────────────────
总计                668      1506      44.4%
```

### 未覆盖的关键类说明

| 类 | 原因 |
|----|------|
| LLMApiClient | 需要真实 DeepSeek API Key，已通过 Resilience4j 降级测试间接覆盖 |
| WebSearchTool | 需要 SerpAPI Key |
| GlobalExceptionHandler | 通过集成测试间接覆盖（异常场景已测试） |
| JwtAuthFilter | 通过集成测试间接覆盖（认证流程已测试） |
| Config 类 (Jackson, MybatisPlus, Redis 等) | 框架配置类，通常不单独测试 |

---

## 四、运行测试

```bash
# 运行全部测试
mvn test -DforkCount=1 -DreuseForks=false

# 运行特定模块测试
mvn test -pl tinybrain-common
mvn test -pl tinybrain-user
mvn test -pl tinybrain-rag
mvn test -pl tinybrain-agent
mvn test -pl tinybrain-app

# 生成覆盖率报告
mvn test jacoco:report
# 报告位置: 各模块 target/site/jacoco/index.html
```

---

## 五、接口测试列表

### 用户认证 (/api/auth)

| 方法 | 路径 | 测试状态 | 说明 |
|------|------|---------|------|
| POST | /api/auth/login | ✅ | 登录成功、密码错误、用户不存在 |
| POST | /api/auth/register | ✅ | 注册成功、用户名重复 |
| GET | /api/auth/me | ✅ | 获取当前用户、无Token、无效Token |

### 知识库文档 (/api/documents)

| 方法 | 路径 | 测试状态 | 说明 |
|------|------|---------|------|
| POST | /api/documents | ✅ | 创建成功、无权限、空标题 |
| GET | /api/documents | ✅ | 分页查询、关键词搜索 |
| GET | /api/documents/{id} | ✅ | 详情成功、ID不存在 |
| PUT | /api/documents/{id} | ✅ | 更新成功 |
| DELETE | /api/documents/{id} | ✅ | 删除成功、ID不存在 |
| POST | /api/documents/upload | ✅ | 上传成功、空文件 |

### RAG 检索 (/api/rag)

| 方法 | 路径 | 测试状态 | 说明 |
|------|------|---------|------|
| GET | /api/rag/ask | ✅ | 问答、空问题、无Token |
| POST | /api/rag/index/{id} | ✅ | 索引文档 |
| GET | /api/rag/stats | ✅ | 统计信息 |

### Agent 智能体 (/api/agent)

| 方法 | 路径 | 测试状态 | 说明 |
|------|------|---------|------|
| POST | /api/agent/chat | ✅ | 对话成功、无Token |
| GET | /api/agent/tools | ✅ | 工具列表、含calculator |
| DELETE | /api/agent/session/{id} | ✅ | 清除会话 |
