package com.tinybrain.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.SkillConfig;
import com.tinybrain.agent.dto.SkillDistillRequest;
import com.tinybrain.agent.dto.SkillInfo;
import com.tinybrain.agent.entity.Skill;
import com.tinybrain.agent.mapper.SkillMapper;
import com.tinybrain.agent.plugin.AgentTool;
import com.tinybrain.rag.dto.LLMChatRequest;
import com.tinybrain.rag.service.LLMApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Skill 管理服务
 * <p>
 * 管理 Skill 的创建、编辑、删除、蒸馏等功能。数据持久化到数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final AgentEngine agentEngine;
    private final LLMApiClient llmClient;
    private final ObjectMapper objectMapper;
    private final SkillMapper skillMapper;

    // ========== 初始化 ==========

    /**
     * 启动时初始化内置 Skill 和市场 Skill
     */
    @PostConstruct
    public void initBuiltinSkills() {
        // 检查数据库是否已有 builtin 技能
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getType, "builtin");
        Long count = skillMapper.selectCount(wrapper);

        if (count > 0) {
            log.info("数据库已有 {} 个内置 Skill，跳过初始化", count);
        } else {
            // 注册内置工具为 Skill
            Map<String, AgentTool> tools = agentEngine.getTools();
            for (Map.Entry<String, AgentTool> entry : tools.entrySet()) {
                Skill skill = new Skill();
                skill.setSkillId(UUID.randomUUID().toString());
                skill.setName(entry.getKey());
                skill.setDescription(entry.getValue().getDescription());
                skill.setType("builtin");
                skill.setToolName(entry.getKey());
                skill.setToolDescription(entry.getValue().getDescription());
                skill.setEnabled(1);
                skill.setSource("builtin");
                skill.setVersion("1.0.0");
                skill.setAuthor("TinyBrain");
                skill.setUserId(1L); // 系统内置，归属 admin
                skillMapper.insert(skill);
            }
            log.info("初始化 {} 个内置 Skill", tools.size());
        }

        // 初始化市场 Skill
        initMarketSkills();
    }

    /**
     * 初始化市场 Skill
     */
    private void initMarketSkills() {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getSource, "marketplace");
        Long count = skillMapper.selectCount(wrapper);

        if (count > 0) {
            log.info("数据库已有 {} 个市场 Skill，跳过初始化", count);
            return;
        }

        addMarketSkill("web_scraper", "网页抓取", "抓取网页内容并提取关键信息", "web_search");
        addMarketSkill("data_analyzer", "数据分析", "分析数据并生成统计报告", "calculator");
        addMarketSkill("code_generator", "代码生成", "根据需求生成代码片段", "knowledge_search");
        addMarketSkill("translator", "翻译助手", "多语言翻译工具", "web_search");
        addMarketSkill("summarizer", "摘要生成", "生成文档或对话的摘要", "knowledge_search");

        log.info("初始化 5 个市场 Skill");
    }

    /**
     * 添加市场 Skill
     */
    private void addMarketSkill(String id, String name, String description, String toolName) {
        Skill skill = new Skill();
        skill.setSkillId(id);
        skill.setName(name);
        skill.setDescription(description);
        skill.setType("market");
        skill.setToolName(toolName);
        skill.setToolDescription(description);
        skill.setEnabled(1);
        skill.setSource("marketplace");
        skill.setVersion("1.0.0");
        skill.setAuthor("TinyBrain");
        skill.setUserId(1L); // 系统内置，归属 admin
        skillMapper.insert(skill);
    }

    // ========== 公开 API ==========

    /**
     * 获取所有 Skill
     */
    public List<SkillInfo> listSkills() {
        List<Skill> skills = skillMapper.selectList(null);
        List<SkillInfo> result = new ArrayList<>();
        for (Skill skill : skills) {
            result.add(toInfo(skill));
        }
        return result;
    }

    /**
     * 获取 Skill 详情
     */
    public SkillInfo getSkill(String skillId) {
        Skill skill = getBySkillId(skillId);
        return toInfo(skill);
    }

    /**
     * 创建 Skill
     */
    public SkillInfo createSkill(SkillConfig config, Long userId) {
        String skillId = UUID.randomUUID().toString();

        Skill skill = new Skill();
        skill.setSkillId(skillId);
        skill.setName(config.getName());
        skill.setDescription(config.getDescription());
        skill.setType(config.getType());
        skill.setToolName(config.getToolName());
        skill.setToolDescription(config.getToolDescription());
        skill.setParametersSchema(config.getParametersSchema());
        skill.setTriggers(toJson(config.getTriggers()));
        skill.setConfig(toJson(config.getConfig()));
        skill.setEnabled(config.isEnabled() ? 1 : 0);
        skill.setPriority(config.getPriority());
        skill.setTags(toJson(config.getTags()));
        skill.setSource("custom");
        skill.setVersion("1.0.0");
        skill.setAuthor("User");
        skill.setUserId(userId);

        skillMapper.insert(skill);
        log.info("Skill 创建成功: {} - {}", skill.getName(), skill.getSkillId());
        return toInfo(skill);
    }

    /**
     * 更新 Skill
     */
    public SkillInfo updateSkill(String skillId, SkillConfig config) {
        Skill skill = getBySkillId(skillId);

        skill.setName(config.getName());
        skill.setDescription(config.getDescription());
        skill.setType(config.getType());
        skill.setToolName(config.getToolName());
        skill.setToolDescription(config.getToolDescription());
        skill.setParametersSchema(config.getParametersSchema());
        skill.setTriggers(toJson(config.getTriggers()));
        skill.setConfig(toJson(config.getConfig()));
        skill.setEnabled(config.isEnabled() ? 1 : 0);
        skill.setPriority(config.getPriority());
        skill.setTags(toJson(config.getTags()));

        skillMapper.updateById(skill);
        log.info("Skill 更新成功: {} - {}", skill.getName(), skill.getSkillId());
        return toInfo(skill);
    }

    /**
     * 删除 Skill
     */
    public void deleteSkill(String skillId) {
        Skill skill = getBySkillId(skillId);
        skillMapper.deleteById(skill.getId());
        log.info("Skill 删除成功: {} - {}", skill.getName(), skill.getSkillId());
    }

    /**
     * 启用/禁用 Skill
     */
    public SkillInfo toggleSkill(String skillId) {
        Skill skill = getBySkillId(skillId);
        skill.setEnabled(skill.getEnabled() == 1 ? 0 : 1);
        skillMapper.updateById(skill);
        log.info("Skill {} 已{}", skill.getName(), skill.getEnabled() == 1 ? "启用" : "禁用");
        return toInfo(skill);
    }

    /**
     * 蒸馏 Skill
     */
    public SkillInfo distillSkill(SkillDistillRequest request, Long userId) {
        String prompt = buildDistillPrompt(request);

        try {
            // 调用 LLM 进行蒸馏
            LLMChatRequest llmRequest = new LLMChatRequest();
            List<LLMChatRequest.Message> messages = new ArrayList<>();
            LLMChatRequest.Message msg = new LLMChatRequest.Message();
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
            SkillInfo info = createSkill(config, userId);

            // 更新 source 为 distilled
            Skill skill = getBySkillId(info.getId());
            skill.setSource("distilled");
            skillMapper.updateById(skill);

            log.info("Skill 蒸馏成功: {} - {}", info.getName(), info.getId());
            return toInfo(skill);
        } catch (Exception e) {
            log.error("Skill 蒸馏失败", e);
            throw new RuntimeException("Skill 蒸馏失败: " + e.getMessage());
        }
    }

    /**
     * 获取 Skill 市场列表
     */
    public List<SkillInfo> getMarketSkills() {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getSource, "marketplace");
        List<Skill> skills = skillMapper.selectList(wrapper);
        List<SkillInfo> result = new ArrayList<>();
        for (Skill skill : skills) {
            result.add(toInfo(skill));
        }
        return result;
    }

    /**
     * 安装 Skill
     */
    public SkillInfo installSkill(String skillId, Long userId) {
        Skill marketSkill = getBySkillId(skillId);
        if (!"marketplace".equals(marketSkill.getSource())) {
            throw new RuntimeException("该 Skill 不是市场 Skill: " + skillId);
        }

        // 检查是否已安装
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getName, marketSkill.getName())
               .ne(Skill::getSource, "marketplace");
        Long count = skillMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("Skill 已安装: " + marketSkill.getName());
        }

        // 创建副本
        String newSkillId = UUID.randomUUID().toString();
        Skill skill = new Skill();
        skill.setSkillId(newSkillId);
        skill.setName(marketSkill.getName());
        skill.setDescription(marketSkill.getDescription());
        skill.setType("installed");
        skill.setToolName(marketSkill.getToolName());
        skill.setToolDescription(marketSkill.getToolDescription());
        skill.setParametersSchema(marketSkill.getParametersSchema());
        skill.setTriggers(marketSkill.getTriggers());
        skill.setConfig(marketSkill.getConfig());
        skill.setEnabled(1);
        skill.setPriority(marketSkill.getPriority());
        skill.setTags(marketSkill.getTags());
        skill.setSource("marketplace");
        skill.setVersion(marketSkill.getVersion());
        skill.setAuthor(marketSkill.getAuthor());
        skill.setUserId(userId);

        skillMapper.insert(skill);
        log.info("Skill 安装成功: {} - {}", skill.getName(), skill.getSkillId());
        return toInfo(skill);
    }

    // ========== 内部方法 ==========

    /**
     * 根据 skillId 查询数据库
     */
    private Skill getBySkillId(String skillId) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getSkillId, skillId);
        Skill skill = skillMapper.selectOne(wrapper);
        if (skill == null) {
            throw new RuntimeException("Skill 不存在: " + skillId);
        }
        return skill;
    }

    /**
     * 实体 -> DTO
     */
    private SkillInfo toInfo(Skill skill) {
        SkillInfo info = new SkillInfo();
        info.setId(skill.getSkillId());
        info.setName(skill.getName());
        info.setDescription(skill.getDescription());
        info.setType(skill.getType());
        info.setToolName(skill.getToolName());
        info.setToolDescription(skill.getToolDescription());
        info.setParametersSchema(skill.getParametersSchema());
        info.setTriggers(parseList(skill.getTriggers()));
        info.setConfig(parseMap(skill.getConfig()));
        info.setEnabled(skill.getEnabled() == 1);
        info.setPriority(skill.getPriority() != null ? skill.getPriority() : 0);
        info.setTags(parseList(skill.getTags()));
        info.setSource(skill.getSource());
        info.setVersion(skill.getVersion());
        info.setAuthor(skill.getAuthor());
        info.setDownloads(0);
        info.setRating(0.0);
        info.setCreatedAt(skill.getCreateTime());
        info.setUpdatedAt(skill.getUpdateTime());
        return info;
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
                return objectMapper.readValue(json, SkillConfig.class);
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

    // ========== JSON 工具方法 ==========

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
