package com.tinybrain.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tinybrain.user.dto.*;
import com.tinybrain.user.entity.User;

/**
 * 用户服务接口
 * <p>
 * 继承 MyBatis-Plus IService，自动获得标准 CRUD。
 * 扩展业务方法：认证注册相关。
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户信息
     */
    UserVO register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（Token + 用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);
}
