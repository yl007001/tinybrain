# TinyBrain Bug 修复记录 #01

> 记录时间：2026-06-11
> 问题数量：10/10

---

## Bug #1: Skill/MCP Controller 未被 Spring 注册

**现象**：访问 `/api/skills/list` 返回 500，日志显示 `No static resource api/skills/list`

**原因**：用 `mvn spring-boot:run` 启动时，Controller 类没有被正确加载到 Spring 容器

**解决**：改用 `java -jar target/tinybrain-app-1.0.0-SNAPSHOT.jar` 直接运行 jar 包

**文件**：无代码修改，启动方式问题

---

## Bug #2: 文档重启后丢失

**现象**：上传的文档下次启动项目就没了

**原因**：开发环境使用 H2 内存数据库 (`jdbc:h2:mem:tinybrain`)，重启后数据清空

**解决**：改为 H2 文件数据库 `jdbc:h2:file:./data/tinybrain`

**文件**：`tinybrain-app/src/main/resources/application-dev.yml`

---

## Bug #3: H2 文件库启动报错 AUTO_SERVER 冲突

**现象**：`Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"`

**原因**：H2 数据库不支持同时使用 `AUTO_SERVER=TRUE` 和 `DB_CLOSE_ON_EXIT=FALSE`

**解决**：去掉 `DB_CLOSE_ON_EXIT=FALSE`，只保留 `AUTO_SERVER=TRUE`

**文件**：`tinybrain-app/src/main/resources/application-dev.yml`

---

## Bug #4: 重启后初始数据插入冲突

**现象**：`Unique index or primary key violation: "PUBLIC.UK_USERNAME"`

**原因**：schema.sql 使用 `INSERT INTO` 插入初始用户，但文件库中数据已存在

**解决**：改为 `MERGE INTO ... KEY(username)` 实现存在则更新、不存在则插入

**文件**：`tinybrain-app/src/main/resources/sql/schema.sql`

---

## Bug #5: DeepSeek 不支持 Embedding 模型

**现象**：文档索引失败，日志显示 `LLM Embedding API HTTP 错误: status=404`

**原因**：DeepSeek 只有对话模型（deepseek-v4-flash/pro），没有 Embedding 模型

**解决**：重写 RAG 方案，改用 DeepSeek 提取关键词 + 数据库 LIKE 模糊匹配，不再依赖 Embedding

**文件**：
- `tinybrain-rag/src/main/java/com/tinybrain/rag/service/RAGService.java`
- `tinybrain-knowledge/src/main/java/com/tinybrain/knowledge/entity/DocumentChunk.java`
- `tinybrain-app/src/main/resources/sql/schema.sql`

---

## Bug #6: 关键词搜索用 AND 逻辑导致搜不到

**现象**：已索引文档，但 RAG 搜索返回空结果

**原因**：搜索逻辑使用 AND（所有关键词必须匹配），但提取的关键词和文档内容不完全一致

**解决**：改为 OR 逻辑（任一关键词匹配即命中），使用 `wrapper.nested()` 构建嵌套条件

**文件**：`tinybrain-rag/src/main/java/com/tinybrain/rag/service/RAGService.java`

---

## Bug #7: 前端 Dashboard 显示向量数为 0

**现象**：RAG 已索引，但 Dashboard 显示"向量数 = 0"

**原因**：前端读取 `totalVectors` 字段，但新 RAG 返回的是 `indexedChunks`

**解决**：在 RAG 统计中增加 `totalVectors` 字段，值等于 `indexedChunks`

**文件**：`tinybrain-rag/src/main/java/com/tinybrain/rag/service/RAGService.java`

---

## Bug #8: Agent 返回原始 XML 工具调用标签

**现象**：前端显示 `<<｜DSML｜｜tool_calls>` 等原始标签

**原因**：LLM 返回的工具调用格式不是期望的 JSON，而是 XML 格式

**解决**：更新系统提示，明确要求只用 JSON 格式调用工具，并给出具体示例

**文件**：`tinybrain-agent/src/main/java/com/tinybrain/agent/core/AgentEngine.java`

---

## Bug #9: 前端请求超时导致 Agent 调用失败

**现象**：Agent 调用多个工具后返回"请求失败，请检查后端服务"

**原因**：前端 API 超时设置为 30 秒，Agent 调用多个工具（web_search、knowledge_search 等）超过 30 秒

**解决**：将前端 API 超时从 30 秒改为 120 秒

**文件**：`tinybrain-ui/src/api/index.ts`

---

## Bug #9: H2 索引冲突导致启动失败

**现象**：`Index "IDX_USER_ID" already exists`

**原因**：schema.sql 使用 `CREATE TABLE IF NOT EXISTS` 但内联 INDEX 定义在表已存在时仍会执行

**解决**：去掉内联 INDEX 定义，改为在实体类上用 MyBatis-Plus 注解管理索引

**文件**：`tinybrain-app/src/main/resources/sql/schema.sql`

---

## Bug #10: Skill 初始化缺少 userId 导致插入失败

**现象**：`NULL not allowed for column "USER_ID"`

**原因**：SkillService.initBuiltinSkills() 插入内置技能时未设置 userId 字段

**解决**：为内置技能和市场技能设置 `userId = 1L`（admin 用户）

**文件**：`tinybrain-agent/src/main/java/com/tinybrain/agent/service/SkillService.java`

---

## 待修复问题

（暂无）

---

## 经验总结

1. **启动方式**：`mvn spring-boot:run` 可能有类加载问题，优先用 `java -jar` 直接运行
2. **数据库持久化**：开发环境也要用文件库，方便测试和调试
3. **LLM 兼容性**：不同 LLM 服务商的 API 能力不同，需要做兼容处理
4. **超时设置**：涉及多步骤 AI 调用的接口，超时时间要足够长
5. **系统提示**：LLM 的输出格式取决于提示词，需要明确指定期望格式
6. **H2 兼容性**：`CREATE TABLE IF NOT EXISTS` 不处理内联索引冲突，索引要单独管理
7. **非空字段**：数据库初始化数据时，确保所有 NOT NULL 字段都有值
