package com.tinybrain.agent.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Skill 信息 DTO
 */
@Data
public class SkillInfo {

    /**
     * Skill ID
     */
    private String id;

    /**
     * Skill 名称
     */
    private String name;

    /**
     * Skill 描述
     */
    private String description;

    /**
     * Skill 类型：builtin, custom, mcp
     */
    private String type;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具描述
     */
    private String toolDescription;

    /**
     * 参数 Schema
     */
    private String parametersSchema;

    /**
     * 触发条件
     */
    private List<String> triggers;

    /**
     * 配置参数
     */
    private Map<String, Object> config;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 优先级
     */
    private int priority;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 来源（market, custom, distilled）
     */
    private String source;

    /**
     * 版本
     */
    private String version;

    /**
     * 作者
     */
    private String author;

    /**
     * 下载次数
     */
    private int downloads;

    /**
     * 评分
     */
    private double rating;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
