package com.tinybrain.user.controller;

import com.tinybrain.common.response.R;
import com.tinybrain.user.dto.*;
import com.tinybrain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 * <p>
 * 提供注册、登录、获取当前用户信息等接口。
 * 所有接口路径前缀 /api/auth，不受 Spring Security 拦截。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        UserVO userVO = userService.register(request);
        return R.ok("注册成功", userVO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return R.ok(response);
    }

    /**
     * 获取当前用户信息
     * 从 SecurityContext 中获取用户ID
     */
    @GetMapping("/me")
    public R<UserVO> me(@RequestAttribute Long currentUserId) {
        UserVO userVO = userService.getCurrentUser(currentUserId);
        return R.ok(userVO);
    }
}
