-- ============================================
-- RAG-GDGS 数据库初始化脚本
-- ============================================
-- 数据库: rag_gdgs
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 说明: 此脚本在 MySQL 容器首次启动时自动执行
-- ============================================

-- 创建数据库（如果不存在）
-- 注意：docker-compose.yaml 中的 MYSQL_DATABASE 环境变量也会创建数据库
-- 这里使用 IF NOT EXISTS 作为双重保障，避免冲突
CREATE DATABASE IF NOT EXISTS `rag_gdgs`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `rag_gdgs`;

-- ============================================
-- 删除表（如果存在，用于重新初始化）
-- 注意：需要按照外键依赖关系的逆序删除
-- ============================================
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `files`;
DROP TABLE IF EXISTS `session`;
DROP TABLE IF EXISTS `user`;

-- ============================================
-- 1. 用户表 (user)
-- ============================================
CREATE TABLE `user`
(
    `id`       BIGINT UNSIGNED AUTO_INCREMENT COMMENT '用户ID，主键'
        PRIMARY KEY,
    `name`     VARCHAR(50)                        NULL COMMENT '用户名',
    `email`    VARCHAR(100)                       NOT NULL COMMENT '邮箱（最大长度100字符）',
    `password` VARCHAR(60)                        NOT NULL COMMENT '密码（BCrypt加密后固定60字符）',
    `auth`     TINYINT      DEFAULT 0             NULL COMMENT '用户角色：0=普通用户，1=管理员',
    `created`  DATETIME     DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    `updated`  DATETIME     DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `uk_user_email` UNIQUE (`email`)
)
    COMMENT '用户表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- 用户表索引
CREATE INDEX `idx_user_auth` ON `user` (`auth`);
CREATE INDEX `idx_user_created` ON `user` (`created`);

-- ============================================
-- 2. 会话表 (session)
-- ============================================
CREATE TABLE `session`
(
    `id`      BIGINT AUTO_INCREMENT COMMENT '会话ID，主键'
        PRIMARY KEY,
    `user_id` BIGINT       NOT NULL COMMENT '用户ID',
    `title`   VARCHAR(255) NOT NULL COMMENT '会话标题',
    `created` DATETIME     DEFAULT (NOW()) NOT NULL COMMENT '创建时间',
    `updated` DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
)
    COMMENT '聊天会话表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- 会话表索引
CREATE INDEX `idx_session_user_id` ON `session` (`user_id`);
CREATE INDEX `idx_session_created` ON `session` (`created`);

-- ============================================
-- 3. 消息表 (message)
-- ============================================
CREATE TABLE `message`
(
    `id`             BIGINT AUTO_INCREMENT COMMENT '消息ID，主键'
        PRIMARY KEY,
    `session_id`     BIGINT   NOT NULL COMMENT '所属会话ID',
    `sender_type`    TINYINT  NOT NULL COMMENT '发送者类型：0=用户，1=机器人，2=系统',
    `message_type`   INT      NOT NULL COMMENT '消息类型：0=文本，1=图片，2=音频，3=文件，4=JSON',
    `assistant_type` INT      DEFAULT 0 NULL COMMENT '助手类型：0=本地，1=在线',
    `contents`       TEXT     NULL COMMENT '消息内容',
    `created`        DATETIME DEFAULT (NOW()) NULL COMMENT '创建时间'
)
    COMMENT '聊天消息表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- 消息表索引
CREATE INDEX `idx_message_session_id` ON `message` (`session_id`);
CREATE INDEX `idx_message_created` ON `message` (`created`);
CREATE INDEX `idx_message_session_created` ON `message` (`session_id`, `created`);

-- ============================================
-- 4. 文件表 (files)
-- ============================================
CREATE TABLE `files`
(
    `id`         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '文件ID，主键'
        PRIMARY KEY,
    `name`       VARCHAR(255)                       NOT NULL COMMENT '文件名（含扩展名）',
    `parent_id`  BIGINT UNSIGNED                   NULL COMMENT '父目录ID（NULL 表示根目录）',
    `folder`     TINYINT(1)      DEFAULT 0         NOT NULL COMMENT '是否为文件夹：0=文件，1=文件夹',
    `size`       BIGINT UNSIGNED DEFAULT '0'       NULL COMMENT '文件大小（字节）',
    `storage_id` VARCHAR(100)                      NOT NULL COMMENT '文件在服务器的存储位置（UUID格式36字符+扩展名，最大100字符）',
    `created_by` BIGINT                            NULL COMMENT '创建者ID',
    `updated_by` BIGINT                            NULL COMMENT '更新者ID',
    `created`    DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '上传时间',
    `updated`    DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    CONSTRAINT `uk_files_storage_id` UNIQUE (`storage_id`)
)
    COMMENT '文件信息表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- 文件表索引
CREATE INDEX `idx_files_parent_id` ON `files` (`parent_id`);
CREATE INDEX `idx_files_folder` ON `files` (`folder`);
CREATE INDEX `idx_files_created_by` ON `files` (`created_by`);
CREATE INDEX `idx_files_created` ON `files` (`created`);


