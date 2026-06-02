package com.tinybrain.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * Agent 对话响应
 */
@Data
public class AgentResponse {

    /** 最终回复 */
    private String reply;

    /** 工具调用历史 */
    private List<ToolCall> toolCalls;

    /** 总轮数 */
    private int iterations;

    /** 匹配到的 Skill 信息（手动 /skill 或自动触发） */
    private SkillMatch matchedSkill;

    @Data
    public static class ToolCall {
        private String toolName;
        private String args;
        private String result;
    }

    @Data
    public static class SkillMatch {
        /** Skill ID */
        private String id;
        /** Skill 名称 */
        private String name;
        /** Skill 描述 */
        private String description;
        /** 触发方式：manual（/skill 命令）或 auto（自动匹配） */
        private String triggerType;
    }
}
