package com.tinybrain.user.controller;

import com.tinybrain.common.response.R;
import com.tinybrain.common.util.SecurityUtil;
import com.tinybrain.user.dto.*;
import com.tinybrain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 * <p>
 * 提供注册、登录、获取当前用户信息等接口。
 * 所有接口路径前缀 /api/auth，不受 Spring Security 拦截。
 */
@Tag(name = "01-用户认证", description = "用户注册、登录、当前用户信息查询")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册", description = "使用用户名和密码注册新用户，密码自动 BCrypt 加密")
    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        UserVO userVO = userService.register(request);
        return R.ok("注册成功", userVO);
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 JWT Token（有效期24小时）")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return R.ok(response);
    }

    @Operation(summary = "获取当前用户", description = "从请求头中的 JWT Token 解析当前用户信息")
    @GetMapping("/me")
    public R<UserVO> me() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        UserVO userVO = userService.getCurrentUser(currentUserId);
        return R.ok(userVO);
    }
}
