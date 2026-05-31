# Contributing to TinyBrain

感谢您对 TinyBrain 的关注！我们欢迎各种形式的贡献。

## 🐛 Bug 报告

1. 检查是否已有相关 Issue
2. 使用 Issue 模板创建新 Issue
3. 描述步骤、预期行为和实际行为
4. 附上日志和截图

## 💡 功能建议

1. 先讨论再实现 — 创建 Feature Request Issue
2. 描述使用场景和想要解决的问题
3. 保持范围聚焦

## 🔧 提交 PR

1. Fork 仓库
2. 创建功能分支: `git checkout -b feat/your-feature`
3. 提交代码: `git commit -m 'feat: add some feature'`
4. 推送到远程: `git push origin feat/your-feature`
5. 创建 Pull Request

### Commit 规范

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试
chore: 构建/工具
```

### 开发指南

```bash
# 后端
mvn clean install -DskipTests
cd tinybrain-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端
cd tinybrain-ui
npm install
npm run dev
```

### 代码审查

所有 PR 需要至少一个 Review。审查重点关注：
- 正确性
- 测试覆盖
- 性能影响
- 向后兼容

## 📋 行为准则

请保持尊重和专业。我们欢迎来自任何背景的贡献者。
