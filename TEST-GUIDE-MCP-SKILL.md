# TinyBrain MCP & Skill 管理功能测试指南

## 访问地址

- **前端**: http://localhost:3000
- **后端 API**: http://localhost:8080
- **登录账号**: demo / password

## 功能概览

新增了两个管理页面：
1. **MCP 管理** - 配置和管理 MCP 服务器
2. **Skill 管理** - 创建、编辑、删除、蒸馏 Skill

---

## 测试步骤

### 1. 登录系统

1. 打开浏览器访问 http://localhost:3000
2. 输入账号密码：demo / password
3. 点击登录

### 2. 查看 Skill 管理页面

1. 在左侧导航栏点击 **"Skill 管理"**
2. 你应该看到：
   - 4 个内置 Skill：calculator、knowledge_search、web_search、get_datetime
   - 顶部操作栏有：创建 Skill、蒸馏 Skill、Skill 市场、刷新 按钮

### 3. 创建自定义 Skill

1. 点击 **"创建 Skill"** 按钮
2. 填写表单：
   - Skill 名称：`翻译助手`
   - 描述：`帮助用户进行多语言翻译`
   - 工具名称：选择 `web_search`
   - 触发条件：`翻译,translate,多语言`
   - 标签：`translation,language`
3. 点击 **"创建"**
4. 验证：列表中应该出现新创建的 Skill

### 4. 从 Skill 市场安装

1. 点击 **"Skill 市场"** 按钮
2. 你应该看到 5 个市场 Skill：
   - 网页抓取
   - 数据分析
   - 代码生成
   - 翻译助手
   - 摘要生成
3. 选择一个 Skill，点击 **"安装"**
4. 验证：列表中应该出现新安装的 Skill，来源显示为 "market"

### 5. 启用/禁用 Skill

1. 在 Skill 列表中，找到一个自定义 Skill
2. 点击状态开关，切换启用/禁用状态
3. 验证：状态应该正确切换

### 6. 删除 Skill

1. 在 Skill 列表中，找到一个自定义 Skill
2. 点击 **"删除"** 按钮
3. 确认删除
4. 验证：Skill 应该从列表中消失

### 7. 查看 MCP 管理页面

1. 在左侧导航栏点击 **"MCP 管理"**
2. 你应该看到：
   - 空的服务器列表（初始状态）
   - 顶部操作栏有：添加 MCP 服务器、刷新 按钮

### 8. 添加 MCP 服务器

1. 点击 **"添加 MCP 服务器"** 按钮
2. 填写表单：
   - 服务器名称：`filesystem-server`
   - 描述：`文件系统访问服务器`
   - 传输类型：`Stdio`
   - 命令：`npx`
   - 参数：`@modelcontextprotocol/server-filesystem /tmp`
   - 自动连接：关闭（避免连接失败）
3. 点击 **"添加"**
4. 验证：列表中应该出现新添加的服务器

### 9. 测试 MCP 服务器连接

1. 在 MCP 服务器列表中，找到刚添加的服务器
2. 点击 **"测试连接"** 按钮
3. 验证：应该显示连接结果（成功或失败）

### 10. 查看 MCP 工具

1. 在 MCP 服务器列表中，找到一个已连接的服务器
2. 点击 **"查看工具"** 按钮
3. 验证：应该显示该服务器提供的工具列表

### 11. 删除 MCP 服务器

1. 在 MCP 服务器列表中，找到一个服务器
2. 点击 **"删除"** 按钮
3. 确认删除
4. 验证：服务器应该从列表中消失

### 12. 测试 Skill 蒸馏功能

1. 点击 **"蒸馏 Skill"** 按钮
2. 填写表单：
   - 蒸馏来源：选择 "从对话历史蒸馏"
   - 来源内容：粘贴一段对话内容
   - Skill 名称：`问答助手`
   - 描述：`基于对话历史的问答助手`
3. 点击 **"开始蒸馏"**
4. 验证：应该生成一个新的 Skill

---

## API 测试（可选）

如果你想直接测试 API，可以使用以下命令：

### 获取 Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"password"}'
```

### 获取 Skill 列表
```bash
curl http://localhost:8080/api/skills/list \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 创建 Skill
```bash
curl -X POST http://localhost:8080/api/skills/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name":"测试Skill","description":"测试描述","toolName":"calculator"}'
```

### 获取 MCP 服务器列表
```bash
curl http://localhost:8080/api/mcp/servers \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 添加 MCP 服务器
```bash
curl -X POST http://localhost:8080/api/mcp/servers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name":"test-server","description":"测试服务器","transportType":"stdio","command":"echo","args":["hello"],"autoConnect":false}'
```

---

## 常见问题

### Q: MCP 服务器连接失败怎么办？
A: 确保：
1. 已安装 Node.js 和 npm
2. MCP 服务器命令正确
3. 网络连接正常

### Q: Skill 蒸馏失败怎么办？
A: 确保：
1. 来源内容足够详细
2. Skill 名称不为空
3. LLM API 可用

### Q: 前端页面显示空白怎么办？
A: 确保：
1. 后端服务正在运行（端口 8080）
2. 前端服务正在运行（端口 3000）
3. 浏览器控制台没有错误

---

## 功能亮点

1. **动态 MCP 配置**：可以随时添加、删除、测试 MCP 服务器
2. **Skill 市场**：提供预置的 Skill 供用户安装
3. **Skill 蒸馏**：从对话历史中提炼出可复用的 Skill
4. **工具管理**：统一管理所有工具（内置 + MCP + 自定义）
5. **实时状态**：显示 MCP 服务器的连接状态和工具数量
