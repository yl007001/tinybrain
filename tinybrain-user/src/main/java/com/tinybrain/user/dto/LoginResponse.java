package com.tinybrain.user.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
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
