package com.tinybrain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Skill 配置 DTO
 */
@Data
public class SkillConfig {

    /**
     * Skill 名称
     */
    @NotBlank(message = "Skill 名称不能为空")
    private String name;

    /**
     * Skill 描述
     */
    @NotBlank(message = "Skill 描述不能为空")
    private String description;

    /**
     * Skill 类型：builtin, custom, mcp
     */
    private String type = "custom";

    /**
     * 工具名称（AgentTool 接口的 getName()）
     */
    @NotBlank(message = "工具名称不能为空")
    private String toolName;

    /**
     * 工具描述
     */
    private String toolDescription;

    /**
     * 参数 Schema（JSON 格式）
     */
    private String parametersSchema;

    /**
     * 触发条件（自然语言描述）
     */
    private List<String> triggers;

    /**
     * 配置参数
     */
    private Map<String, Object> config;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 优先级（数值越大优先级越高）
     */
    private int priority = 0;

    /**
     * 标签
     */
    private List<String> tags;
}
