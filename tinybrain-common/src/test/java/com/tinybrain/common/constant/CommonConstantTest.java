package com.tinybrain.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 常量定义单元测试
 */
class CommonConstantTest {

    @Test
    void constants_shouldHaveCorrectValues() {
        assertEquals("Bearer ", CommonConstant.TOKEN_PREFIX);
        assertEquals("Authorization", CommonConstant.TOKEN_HEADER);
        assertEquals("X-User-Id", CommonConstant.USER_ID_HEADER);
        assertEquals("currentUser", CommonConstant.CURRENT_USER);
    }

    @Test
    void constants_shouldBeAccessibleAsStaticFields() {
        // CommonConstant is an interface, only field access is needed
        assertEquals("ROLE_ADMIN", CommonConstant.ROLE_ADMIN);
        assertEquals("ROLE_USER", CommonConstant.ROLE_USER);
    }
}
