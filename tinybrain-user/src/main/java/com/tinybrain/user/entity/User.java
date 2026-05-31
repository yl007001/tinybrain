package com.tinybrain.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 * <p>
 * 对应表 sys_user，使用 MyBatis-Plus 注解映射。
 * 字段设计：基础信息 + 角色 + 状态 + 逻辑删除 + 时间审计。
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatar;

    /** 角色：ROLE_ADMIN / ROLE_USER */
    private String role;

    /** 状态：1=正常，0=禁用 */
    private Integer status;

    /** 逻辑删除标志 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(this.role);
    }

    /**
     * 账户是否启用
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
}
