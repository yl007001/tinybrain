package com.tinybrain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.user.dto.*;
import com.tinybrain.user.entity.User;
import com.tinybrain.user.mapper.UserMapper;
import com.tinybrain.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * <p>
 * 覆盖：注册、登录、查询当前用户
 * 使用 Mockito mock 依赖，聚焦 Service 层逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 需要手动注入（Mockito 无法自动注入父类字段）
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setNickname("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRole("ROLE_USER");
        testUser.setStatus(1);
    }

    // ==================== 注册测试 ====================

    @Test
    void register_shouldCreateUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setNickname("New User");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        UserVO result = userService.register(request);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(request));
        assertTrue(ex.getMessage().contains("已被注册"));
    }

    // ==================== 登录测试 ====================

    @Test
    void login_shouldReturnTokenWhenValid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(true);

        LoginResponse response = userService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("testuser", response.getUser().getUsername());
    }

    @Test
    void login_shouldThrowWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(request));
        assertTrue(ex.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void login_shouldThrowWhenPasswordWrong() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "$2a$10$encodedPassword")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(request));
        assertTrue(ex.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void login_shouldThrowWhenAccountDisabled() {
        testUser.setStatus(0); // 禁用

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(request));
        assertTrue(ex.getMessage().contains("禁用"));
    }

    // ==================== 查询当前用户 ====================

    @Test
    void getCurrentUser_shouldReturnUserVO() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        UserVO result = userService.getCurrentUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getCurrentUser_shouldThrowWhenNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getCurrentUser(999L));
        assertTrue(ex.getMessage().contains("不存在"));
    }
}
