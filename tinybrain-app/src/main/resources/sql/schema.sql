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
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
    chunk_meta  VARCHAR(1000) COMMENT '块元数据（JSON）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
);

-- ----------------------------
-- 初始数据
-- ----------------------------
INSERT INTO sys_user (username, password, nickname, role) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'ROLE_ADMIN'),
('demo',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '演示用户', 'ROLE_USER');
-- 说明：两个用户的密码都是 "password"，BCrypt 加密
