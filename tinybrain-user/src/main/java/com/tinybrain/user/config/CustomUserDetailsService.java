package com.tinybrain.user.config;

import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.user.entity.User;
import com.tinybrain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security 用户加载服务
 * <p>
 * 实现 UserDetailsService 接口，供 Spring Security 认证使用。
 * 从数据库加载用户信息并转换为 Security 可识别的 UserDetails。
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (!user.isEnabled()) {
            throw BusinessException.forbidden("账户已被禁用");
        }

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
