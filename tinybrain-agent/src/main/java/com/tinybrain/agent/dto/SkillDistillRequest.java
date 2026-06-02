package com.tinybrain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Skill 蒸馏请求 DTO
 */
@Data
public class SkillDistillRequest {

    /**
     * 蒸馏来源类型：conversation, document, code
     */
    @NotBlank(message = "蒸馏来源类型不能为空")
    private String sourceType;

    /**
     * 来源 ID（对话 ID、文档 ID 等）
     */
    private String sourceId;

    /**
     * 来源内容（直接提供内容）
     */
    private String sourceContent;

    /**
     * Skill 名称
     */
    @NotBlank(message = "Skill 名称不能为空")
    private String skillName;

    /**
     * Skill 描述
     */
    private String skillDescription;

    /**
     * 触发条件提示
     */
    private List<String> triggerHints;

    /**
     * 是否自动生成参数 Schema
     */
    private boolean autoGenerateSchema = true;
}
