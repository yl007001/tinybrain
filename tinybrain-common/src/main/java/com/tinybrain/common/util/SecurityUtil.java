package com.tinybrain.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * 安全工具类
 * <p>
 * 封装 Spring SecurityContext 常用操作，避免各处重复代码。
 */
public class SecurityUtil {

    private SecurityUtil() {}

    /**
     * 获取当前登录用户 ID
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * 当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return "ROLE_ADMIN".equals(getCurrentUserRole());
    }
}
