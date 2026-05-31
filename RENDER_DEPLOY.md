# TinyBrain Render 部署指南

## 前置条件
- GitHub 仓库: https://github.com/yl007001/tinybrain
- Render 账号: https://dashboard.render.com （GitHub 登录即可）

## 部署步骤

### 1. 连接仓库

打开 Render Dashboard → **New + → Web Service**

- 选 **Connect a repository** → 授权 GitHub → 选 `yl007001/tinybrain`
- 没有出现仓库的话，点 **Configure GitHub App** 授权全部仓库

### 2. 选择分支

| 想部署哪个 | Branch 填 |
|-----------|-----------|
| v2 Spring AI Alibaba（推荐） | `master` |
| v1 手写 AI 层 | `v1-handcrafted` |

> 两个版本可以分别部署两个 Web Service，一个免费账号可以部署多个服务

### 3. 关键配置项

| 字段 | 填什么 |
|------|--------|
| **Branch** | `master`（或 `v1-handcrafted`） |
| **Name** | `tinybrain-v2`（自动生成，可改） |
| **Runtime** | `Java`（自动识别） |
| **Region** | 选最近的（新加坡或香港） |
| **Branch** | 见上表 |
| **Build Command** | `mvn clean package -DskipTests -pl tinybrain-app -am` |
| **Start Command** | `cd tinybrain-app && java -jar target/*.jar --spring.profiles.active=dev` |
| **Plan** | **Free** ✅ |

### 4. 设置环境变量

点 **Advanced** → **Add Environment Variable**：

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `AI_DASHSCOPE_API_KEY` | 你的 DashScope API Key（v2 需要，可选） |
| `TINYBRAIN_LLM_KEY` | 你的 LLM API Key（v1 需要，可选） |

> LLM Key 不填也能部署，只是 RAG 问答和 Agent 功能不可用，其他功能正常

### 5. 部署

点 **Create Web Service** → 等 3-5 分钟编译部署

部署完成后：
```
https://tinybrain-v2.onrender.com/swagger-ui.html
https://tinybrain-v2.onrender.com/actuator/health
```

### 6. 双版本部署（可选）

部署完第一个后，再点 **New + → Web Service**，选同一个仓库，

Branch 选 `v1-handcrafted`，Name 填 `tinybrain-v1`，其他配置相同。

两个版本同时在线：
- `https://tinybrain-v1.onrender.com` — v1 手写 AI 层
- `https://tinybrain-v2.onrender.com` — v2 Spring AI Alibaba

> ⚠️ 免费限制：15 分钟无请求自动休眠，冷启动约 30 秒
> 用 https://cron-job.org 每 14 分钟发个请求就能保持唤醒
