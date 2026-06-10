-- ============================================
-- TinyBrain 数据库初始化脚本
-- 兼容: H2 (MODE=MySQL) + MySQL 8
-- ============================================

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname    VARCHAR(50)  COMMENT '昵称',
    email       VARCHAR(100) COMMENT '邮箱',
    phone       VARCHAR(20)  COMMENT '手机号',
    avatar      VARCHAR(500) COMMENT '头像URL',
    role        VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=禁用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删, 1=已删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_username UNIQUE (username)
);

-- ----------------------------
-- 文档表
-- ----------------------------
CREATE TABLE IF NOT EXISTS kb_document (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL COMMENT '文档标题',
    summary     VARCHAR(1000) COMMENT '文档摘要',
    content     MEDIUMTEXT    COMMENT '文档内容（Markdown/纯文本）',
    content_type VARCHAR(20)  NOT NULL DEFAULT 'markdown' COMMENT '内容类型: markdown/text/html',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0=草稿, 1=已发布, 2=已归档',
    tags        VARCHAR(500)  COMMENT '标签（JSON数组）',
    user_id     BIGINT        NOT NULL COMMENT '创建者ID',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
);

-- ----------------------------
-- 文档分块表（RAG 阶段使用）
-- ----------------------------
CREATE TABLE IF NOT EXISTS kb_document_chunk (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT        NOT NULL COMMENT '所属文档ID',
    chunk_index INT           NOT NULL COMMENT '块序号',
    content     TEXT          NOT NULL COMMENT '块内容',
    embedding   BLOB          COMMENT '向量嵌入（二进制存储）',
    keywords    TEXT          COMMENT 'LLM提取的关键词（逗号分隔）',
    chunk_meta  VARCHAR(1000) COMMENT '块元数据（JSON）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
);

-- ----------------------------
-- 会话表（Agent/RAG 对话历史）
-- ----------------------------
CREATE TABLE IF NOT EXISTS ai_session (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(50)   NOT NULL COMMENT '会话唯一标识',
    title       VARCHAR(200)  NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    type        VARCHAR(20)   NOT NULL DEFAULT 'agent' COMMENT '类型: agent/rag',
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_session_id UNIQUE (session_id)
);

-- ----------------------------
-- 对话消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS ai_message (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(50)   NOT NULL COMMENT '会话ID',
    role        VARCHAR(20)   NOT NULL COMMENT '角色: user/assistant/system',
    content     MEDIUMTEXT    NOT NULL COMMENT '消息内容',
    tool_calls  MEDIUMTEXT    COMMENT '工具调用记录（JSON）',
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- MCP 服务器配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS ai_mcp_server (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id   VARCHAR(50)   NOT NULL COMMENT '服务器唯一标识',
    name        VARCHAR(100)  NOT NULL COMMENT '服务器名称',
    description VARCHAR(500)  COMMENT '描述',
    transport   VARCHAR(20)   NOT NULL DEFAULT 'stdio' COMMENT '传输方式: stdio/sse',
    command     VARCHAR(500)  COMMENT '启动命令',
    args        VARCHAR(1000) COMMENT '命令参数（JSON数组）',
    env_vars    TEXT          COMMENT '环境变量（JSON对象）',
    url         VARCHAR(500)  COMMENT 'SSE 服务器 URL',
    enabled     TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用',
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_server_id UNIQUE (server_id)
);

-- ----------------------------
-- Skill 技能配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS ai_skill (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id        VARCHAR(50)   NOT NULL COMMENT '技能唯一标识',
    name            VARCHAR(100)  NOT NULL COMMENT '技能名称',
    description     VARCHAR(500)  COMMENT '描述',
    type            VARCHAR(20)   NOT NULL DEFAULT 'custom' COMMENT '类型: builtin/custom/distilled',
    tool_name       VARCHAR(100)  COMMENT '关联的工具名称',
    tool_description VARCHAR(500) COMMENT '工具描述',
    parameters_schema TEXT        COMMENT '参数Schema（JSON）',
    triggers        VARCHAR(500)  COMMENT '触发关键词（JSON数组）',
    config          TEXT          COMMENT '配置（JSON对象）',
    enabled         TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用',
    priority        INT           NOT NULL DEFAULT 0 COMMENT '优先级',
    tags            VARCHAR(200)  COMMENT '标签（JSON数组）',
    source          VARCHAR(20)   NOT NULL DEFAULT 'manual' COMMENT '来源: manual/distilled/marketplace',
    version         VARCHAR(20)   DEFAULT '1.0.0' COMMENT '版本',
    author          VARCHAR(50)   COMMENT '作者',
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_skill_id UNIQUE (skill_id)
);

-- ----------------------------
-- 初始数据
-- ----------------------------
MERGE INTO sys_user (username, password, nickname, role) KEY(username) VALUES
('admin', '$2a$10$ZyIEJtkizUvFjq9/7RzKvubcw6IXvxkXkBw1nPFVVHC7xvlyJil8i', '管理员', 'ROLE_ADMIN');
MERGE INTO sys_user (username, password, nickname, role) KEY(username) VALUES
('demo',  '$2a$10$ZyIEJtkizUvFjq9/7RzKvubcw6IXvxkXkBw1nPFVVHC7xvlyJil8i', '演示用户', 'ROLE_USER');
-- 说明：两个用户的密码都是 "password"，BCrypt 加密
