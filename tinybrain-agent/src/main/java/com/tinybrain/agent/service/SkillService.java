package com.tinybrain.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.SkillConfig;
import com.tinybrain.agent.dto.SkillDistillRequest;
import com.tinybrain.agent.dto.SkillInfo;
import com.tinybrain.agent.plugin.AgentTool;
import com.tinybrain.rag.service.LLMApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 管理服务
 * <p>
 * 管理 Skill 的创建、编辑、删除、蒸馏等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final AgentEngine agentEngine;
    private final LLMApiClient llmClient;
    private final ObjectMapper mapper;

    /**
     * Skill 存储（id -> info）
     */
    private final Map<String, SkillInfo> skillStore = new ConcurrentHashMap<>();

    /**
     * Skill 市场（预置的可用 Skill）
     */
    private final Map<String, SkillInfo> marketSkills = new ConcurrentHashMap<>();

    /**
     * 应用启动后自动初始化
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        initBuiltinSkills();
    }

    /**
     * 初始化预置 Skill
     */
    public void initBuiltinSkills() {
        // 注册内置工具为 Skill
        Map<String, AgentTool> tools = agentEngine.getTools();
        for (Map.Entry<String, AgentTool> entry : tools.entrySet()) {
            SkillInfo info = new SkillInfo();
            info.setId("builtin_" + entry.getKey());
            info.setName(entry.getKey());
            info.setDescription(entry.getValue().getDescription());
            info.setType("builtin");
            info.setToolName(entry.getKey());
            info.setToolDescription(entry.getValue().getDescription());
            info.setEnabled(true);
            info.setSource("builtin");
            info.setVersion("1.0.0");
            info.setAuthor("TinyBrain");
            info.setCreatedAt(LocalDateTime.now());
            info.setUpdatedAt(LocalDateTime.now());
            skillStore.put(info.getId(), info);
        }

        // 初始化 Skill 市场
        initMarketSkills();
    }

    /**
     * 初始化 Skill 市场
     */
    private void initMarketSkills() {
        // 预置一些示例 Skill
        addMarketSkill("web_scraper", "网页抓取", "抓取网页内容并提取关键信息", "web_search");
        addMarketSkill("data_analyzer", "数据分析", "分析数据并生成统计报告", "calculator");
        addMarketSkill("code_generator", "代码生成", "根据需求生成代码片段", "knowledge_search");
        addMarketSkill("translator", "翻译助手", "多语言翻译工具", "web_search");
        addMarketSkill("summarizer", "摘要生成", "生成文档或对话的摘要", "knowledge_search");
    }

    /**
     * 添加市场 Skill
     */
    private void addMarketSkill(String id, String name, String description, String toolName) {
        SkillInfo info = new SkillInfo();
        info.setId(id);
        info.setName(name);
        info.setDescription(description);
        info.setType("market");
        info.setToolName(toolName);
        info.setToolDescription(description);
        info.setEnabled(true);
        info.setSource("market");
        info.setVersion("1.0.0");
        info.setAuthor("TinyBrain");
        info.setDownloads((int) (Math.random() * 1000));
        info.setRating(4.0 + Math.random() * 1.0);
        info.setCreatedAt(LocalDateTime.now());
        info.setUpdatedAt(LocalDateTime.now());
        marketSkills.put(id, info);
    }

    /**
     * 获取所有 Skill
     */
    public List<SkillInfo> listSkills() {
        return new ArrayList<>(skillStore.values());
    }

    /**
     * 获取 Skill 详情
     */
    public SkillInfo getSkill(String id) {
        SkillInfo info = skillStore.get(id);
        if (info == null) {
            throw new RuntimeException("Skill 不存在: " + id);
        }
        return info;
    }

    /**
     * 创建 Skill
     */
    public SkillInfo createSkill(SkillConfig config) {
        String id = UUID.randomUUID().toString().substring(0, 8);

        SkillInfo info = new SkillInfo();
        info.setId(id);
        info.setName(config.getName());
        info.setDescription(config.getDescription());
        info.setType(config.getType());
        info.setToolName(config.getToolName());
        info.setToolDescription(config.getToolDescription());
        info.setParametersSchema(config.getParametersSchema());
        info.setTriggers(config.getTriggers());
        info.setConfig(config.getConfig());
        info.setEnabled(config.isEnabled());
        info.setPriority(config.getPriority());
        info.setTags(config.getTags());
        info.setSource("custom");
        info.setVersion("1.0.0");
        info.setAuthor("User");
        info.setCreatedAt(LocalDateTime.now());
        info.setUpdatedAt(LocalDateTime.now());

        skillStore.put(id, info);
        log.info("Skill 创建成功: {} - {}", info.getName(), info.getId());
        return info;
    }

    /**
     * 更新 Skill
     */
    public SkillInfo updateSkill(String id, SkillConfig config) {
        SkillInfo info = skillStore.get(id);
        if (info == null) {
            throw new RuntimeException("Skill 不存在: " + id);
        }

        info.setName(config.getName());
        info.setDescription(config.getDescription());
        info.setType(config.getType());
        info.setToolName(config.getToolName());
        info.setToolDescription(config.getToolDescription());
        info.setParametersSchema(config.getParametersSchema());
        info.setTriggers(config.getTriggers());
        info.setConfig(config.getConfig());
        info.setEnabled(config.isEnabled());
        info.setPriority(config.getPriority());
        info.setTags(config.getTags());
        info.setUpdatedAt(LocalDateTime.now());

        log.info("Skill 更新成功: {} - {}", info.getName(), info.getId());
        return info;
    }

    /**
     * 删除 Skill
     */
    public void deleteSkill(String id) {
        SkillInfo info = skillStore.remove(id);
        if (info == null) {
            throw new RuntimeException("Skill 不存在: " + id);
        }
        log.info("Skill 删除成功: {} - {}", info.getName(), info.getId());
    }

    /**
     * 启用/禁用 Skill
     */
    public SkillInfo toggleSkill(String id) {
        SkillInfo info = skillStore.get(id);
        if (info == null) {
            throw new RuntimeException("Skill 不存在: " + id);
        }

        info.setEnabled(!info.isEnabled());
        info.setUpdatedAt(LocalDateTime.now());

        log.info("Skill {} 已{}", info.getName(), info.isEnabled() ? "启用" : "禁用");
        return info;
    }

    /**
     * 蒸馏 Skill
     */
    public SkillInfo distillSkill(SkillDistillRequest request) {
        // 使用 LLM 从内容中提取 Skill 定义
        String prompt = buildDistillPrompt(request);

        try {
            // 调用 LLM 进行蒸馏
            com.tinybrain.rag.dto.LLMChatRequest llmRequest = new com.tinybrain.rag.dto.LLMChatRequest();
            List<com.tinybrain.rag.dto.LLMChatRequest.Message> messages = new ArrayList<>();
            com.tinybrain.rag.dto.LLMChatRequest.Message msg = new com.tinybrain.rag.dto.LLMChatRequest.Message();
            msg.setRole("user");
            msg.setContent(prompt);
            messages.add(msg);
            llmRequest.setMessages(messages);
            llmRequest.setTemperature(0.3);

            var llmResponse = llmClient.chat(llmRequest);
            String response = llmResponse.getReplyText();

            // 解析 LLM 响应，提取 Skill 定义
            SkillConfig config = parseDistillResponse(response, request);

            // 创建 Skill
            SkillInfo info = createSkill(config);
            info.setSource("distilled");
            info.setUpdatedAt(LocalDateTime.now());

            log.info("Skill 蒸馏成功: {} - {}", info.getName(), info.getId());
            return info;
        } catch (Exception e) {
            log.error("Skill 蒸馏失败", e);
            throw new RuntimeException("Skill 蒸馏失败: " + e.getMessage());
        }
    }

    /**
     * 构建蒸馏提示词
     */
    private String buildDistillPrompt(SkillDistillRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请从以下内容中提取一个可复用的 Skill 定义。\n\n");
        sb.append("Skill 名称: ").append(request.getSkillName()).append("\n");
        if (request.getSkillDescription() != null) {
            sb.append("Skill 描述: ").append(request.getSkillDescription()).append("\n");
        }
        sb.append("\n来源类型: ").append(request.getSourceType()).append("\n");

        if (request.getSourceContent() != null) {
            sb.append("\n内容:\n").append(request.getSourceContent()).append("\n");
        }

        sb.append("\n请返回 JSON 格式的 Skill 定义，包含以下字段:\n");
        sb.append("- name: Skill 名称\n");
        sb.append("- description: 详细描述\n");
        sb.append("- toolName: 工具名称\n");
        sb.append("- parametersSchema: 参数 Schema (JSON Schema 格式)\n");
        sb.append("- triggers: 触发条件列表\n");

        return sb.toString();
    }

    /**
     * 解析蒸馏响应
     */
    private SkillConfig parseDistillResponse(String response, SkillDistillRequest request) {
        try {
            // 尝试从响应中提取 JSON
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}") + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String json = response.substring(jsonStart, jsonEnd);
                return mapper.readValue(json, SkillConfig.class);
            }
        } catch (Exception e) {
            log.warn("解析蒸馏响应失败，使用默认配置", e);
        }

        // 使用默认配置
        SkillConfig config = new SkillConfig();
        config.setName(request.getSkillName());
        config.setDescription(request.getSkillDescription() != null ?
                request.getSkillDescription() : "从内容中蒸馏的 Skill");
        config.setType("custom");
        config.setToolName("knowledge_search");
        config.setTriggers(request.getTriggerHints());
        config.setEnabled(true);
        return config;
    }

    /**
     * 安装 Skill
     */
    public SkillInfo installSkill(String skillId) {
        SkillInfo marketInfo = marketSkills.get(skillId);
        if (marketInfo == null) {
            throw new RuntimeException("市场中不存在该 Skill: " + skillId);
        }

        // 检查是否已安装
        for (SkillInfo info : skillStore.values()) {
            if (info.getName().equals(marketInfo.getName())) {
                throw new RuntimeException("Skill 已安装: " + marketInfo.getName());
            }
        }

        // 创建副本
        String id = UUID.randomUUID().toString().substring(0, 8);
        SkillInfo info = new SkillInfo();
        info.setId(id);
        info.setName(marketInfo.getName());
        info.setDescription(marketInfo.getDescription());
        info.setType("installed");
        info.setToolName(marketInfo.getToolName());
        info.setToolDescription(marketInfo.getToolDescription());
        info.setEnabled(true);
        info.setSource("market");
        info.setVersion(marketInfo.getVersion());
        info.setAuthor(marketInfo.getAuthor());
        info.setCreatedAt(LocalDateTime.now());
        info.setUpdatedAt(LocalDateTime.now());

        skillStore.put(id, info);

        // 更新下载次数
        marketInfo.setDownloads(marketInfo.getDownloads() + 1);

        log.info("Skill 安装成功: {} - {}", info.getName(), info.getId());
        return info;
    }

    /**
     * 获取 Skill 市场列表
     */
    public List<SkillInfo> getMarketSkills() {
        return new ArrayList<>(marketSkills.values());
    }
}
