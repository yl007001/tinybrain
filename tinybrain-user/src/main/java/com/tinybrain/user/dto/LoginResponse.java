package com.tinybrain.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 */
@Data
@NoArgsConstructor
public class LoginResponse {

    /** JWT Token */
    private String token;

    /** Token 类型 */
    private String tokenType = "Bearer";

    /** 用户信息 */
    private UserVO user;

    public LoginResponse(String token, UserVO user) {
        this.token = token;
        this.user = user;
    }
}
