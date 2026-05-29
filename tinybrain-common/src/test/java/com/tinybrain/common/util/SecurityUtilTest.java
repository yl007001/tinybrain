package com.tinybrain.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全工具类单元测试
 */
class SecurityUtilTest {

    @Test
    void getCurrentUserId_shouldReturnNullWhenNoAuth() {
        SecurityContextHolder.clearContext();
        assertNull(SecurityUtil.getCurrentUserId());
    }

    @Test
    void getCurrentUserRole_shouldReturnNullWhenNoAuth() {
        SecurityContextHolder.clearContext();
        assertNull(SecurityUtil.getCurrentUserRole());
    }

    @Test
    void isAdmin_shouldReturnFalseWhenNoAuth() {
        SecurityContextHolder.clearContext();
        assertFalse(SecurityUtil.isAdmin());
    }

    @Test
    void getCurrentUserId_shouldReturnUserIdWhenAuthenticated() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new Authentication() {
            @Override
            public String getName() { return "1"; }
            @Override
            public Object getPrincipal() { return 1L; }
            @Override
            public Object getCredentials() { return null; }
            @Override
            public Object getDetails() { return null; }
            @Override
            public boolean isAuthenticated() { return true; }
            @Override
            public void setAuthenticated(boolean isAuthenticated) {}
            @Override
            public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }
        };
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        assertEquals(1L, SecurityUtil.getCurrentUserId());
        assertEquals("ROLE_USER", SecurityUtil.getCurrentUserRole());
        assertFalse(SecurityUtil.isAdmin());

        SecurityContextHolder.clearContext();
    }

    @Test
    void isAdmin_shouldReturnTrueForAdminRole() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new Authentication() {
            @Override
            public String getName() { return "admin"; }
            @Override
            public Object getPrincipal() { return 1L; }
            @Override
            public Object getCredentials() { return null; }
            @Override
            public Object getDetails() { return null; }
            @Override
            public boolean isAuthenticated() { return true; }
            @Override
            public void setAuthenticated(boolean isAuthenticated) {}
            @Override
            public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
        };
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        assertTrue(SecurityUtil.isAdmin());
        assertEquals("ROLE_ADMIN", SecurityUtil.getCurrentUserRole());

        SecurityContextHolder.clearContext();
    }
}
