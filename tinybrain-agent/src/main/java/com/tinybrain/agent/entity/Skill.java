package com.tinybrain.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 技能配置实体
 */
@Data
@TableName("ai_skill")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 技能唯一标识 */
    private String skillId;

    /** 技能名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 类型: builtin/custom/distilled */
    private String type;

    /** 关联的工具名称 */
    private String toolName;

    /** 工具描述 */
    private String toolDescription;

    /** 参数Schema（JSON） */
    private String parametersSchema;

    /** 触发关键词（JSON数组） */
    private String triggers;

    /** 配置（JSON对象） */
    private String config;

    /** 是否启用 */
    private Integer enabled;

    /** 优先级 */
    private Integer priority;

    /** 标签（JSON数组） */
    private String tags;

    /** 来源: manual/distilled/marketplace */
    private String source;

    /** 版本 */
    private String version;

    /** 作者 */
    private String author;

    /** 用户ID */
    private Long userId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
