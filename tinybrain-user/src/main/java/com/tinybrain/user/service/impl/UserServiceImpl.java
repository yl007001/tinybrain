package com.tinybrain.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.common.util.JwtUtil;
import com.tinybrain.user.dto.*;
import com.tinybrain.user.entity.User;
import com.tinybrain.user.mapper.UserMapper;
import com.tinybrain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 * <p>
 * 密码使用 BCrypt 加密存储，保证安全性。
 * Token 使用 JWT 签发，无状态认证。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (getByUsername(request.getUsername()) != null) {
            throw BusinessException.conflict("用户名已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("ROLE_USER");
        user.setStatus(1);

        save(user);
        return toUserVO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = getByUsername(request.getUsername());
        if (user == null) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // 检查账户状态
        if (!user.isEnabled()) {
            throw BusinessException.forbidden("账户已被禁用");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // 生成 Token
        String token = JwtUtil.generateToken(user.getId(), user.getRole());
        return new LoginResponse(token, toUserVO(user));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toUserVO(user);
    }

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    /**
     * 实体 → VO 转换
     */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
