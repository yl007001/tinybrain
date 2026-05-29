package com.tinybrain.common.constant;

/**
 * 系统通用常量
 */
public interface CommonConstant {

    /** 当前用户（ThreadLocal 中存储的 key） */
    String CURRENT_USER = "currentUser";
    /** 用户ID（请求头中传递的 key） */
    String USER_ID_HEADER = "X-User-Id";
    /** Token 前缀 */
    String TOKEN_PREFIX = "Bearer ";
    /** Token 请求头 */
    String TOKEN_HEADER = "Authorization";

    // ========== 角色常量 ==========
    String ROLE_ADMIN = "ROLE_ADMIN";
    String ROLE_USER = "ROLE_USER";

    // ========== 知识库常量 ==========
    /** 文档状态：草稿 */
    int DOC_STATUS_DRAFT = 0;
    /** 文档状态：已发布 */
    int DOC_STATUS_PUBLISHED = 1;
    /** 文档状态：已归档 */
    int DOC_STATUS_ARCHIVED = 2;

    /** 默认分页大小 */
    int DEFAULT_PAGE_SIZE = 10;
    /** 最大分页大小 */
    int MAX_PAGE_SIZE = 100;

    /** 日期格式 */
    String DATE_FORMAT = "yyyy-MM-dd";
    /** 日期时间格式 */
    String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
