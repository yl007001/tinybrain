package com.tinybrain.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应 VO
 * <p>
 * 只暴露非敏感字段，password 等敏感字段不返回给前端。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String role;
    private LocalDateTime createTime;
}
