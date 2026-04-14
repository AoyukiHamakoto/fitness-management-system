-- =============================================================================
-- fitness_mvp 数据库表结构（MVP 核心表）
-- MySQL 8.0 | utf8mb4 / utf8mb4_unicode_ci | InnoDB
-- 执行前请确认已安装 MySQL 8.0+，并具有建库权限
-- =============================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `fitness_mvp`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `fitness_mvp`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `body_data_record`;
DROP TABLE IF EXISTS `ai_dialog`;
DROP TABLE IF EXISTS `punch_record`;
DROP TABLE IF EXISTS `plan_detail`;
DROP TABLE IF EXISTS `fitness_plan`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 用户表
-- -----------------------------------------------------------------------------
CREATE TABLE `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(50)     NOT NULL COMMENT '登录账号',
    `phone`           VARCHAR(20)     NOT NULL COMMENT '手机号',
    `password`        VARCHAR(255)    NOT NULL COMMENT '密码（加密存储）',
    `nickname`        VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '昵称',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------------------------
-- 健身计划表
-- -----------------------------------------------------------------------------
CREATE TABLE `fitness_plan` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    `plan_name`       VARCHAR(100)    NOT NULL COMMENT '计划名称',
    `plan_desc`       VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '计划说明',
    `start_date`      DATE            NOT NULL COMMENT '开始日期',
    `end_date`        DATE            NOT NULL COMMENT '结束日期',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0草稿 1进行中 2已完成',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_fitness_plan_user_id` (`user_id`),
    CONSTRAINT `fk_fitness_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健身计划表';

-- -----------------------------------------------------------------------------
-- 计划详情表（按天/条目拆解训练内容）
-- -----------------------------------------------------------------------------
CREATE TABLE `plan_detail` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id`          BIGINT UNSIGNED NOT NULL COMMENT '所属计划ID',
    `day_index`        INT UNSIGNED NOT NULL COMMENT '计划内第几天（从1开始）',
    `exercise_name`    VARCHAR(100)    NOT NULL COMMENT '训练项目名称',
    `duration_minutes` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '建议时长（分钟）',
    `sort_order`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '当日动作排序，升序',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan_detail_plan_id` (`plan_id`),
    CONSTRAINT `fk_plan_detail_plan` FOREIGN KEY (`plan_id`) REFERENCES `fitness_plan` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划详情表';

-- -----------------------------------------------------------------------------
-- 打卡记录表
-- -----------------------------------------------------------------------------
CREATE TABLE `punch_record` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `plan_id`         BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
    `plan_detail_id`  BIGINT UNSIGNED          DEFAULT NULL COMMENT '计划明细ID，空表示当日计划级打卡',
    `punch_date`      DATE            NOT NULL COMMENT '打卡日期',
    `actual_sets`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '实际完成组数',
    `actual_weight`   DECIMAL(6, 2)   NOT NULL DEFAULT 0.00 COMMENT '实际使用重量（kg）',
    `remark`          VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '备注',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    PRIMARY KEY (`id`),
    KEY `idx_punch_user_id` (`user_id`),
    KEY `idx_punch_plan_id` (`plan_id`),
    KEY `idx_punch_plan_detail_id` (`plan_detail_id`),
    KEY `idx_punch_date` (`punch_date`),
    UNIQUE KEY `uk_punch_user_detail_date` (`user_id`, `plan_detail_id`, `punch_date`),
    CONSTRAINT `fk_punch_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_punch_plan` FOREIGN KEY (`plan_id`) REFERENCES `fitness_plan` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_punch_plan_detail` FOREIGN KEY (`plan_detail_id`) REFERENCES `plan_detail` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡记录表';

-- -----------------------------------------------------------------------------
-- AI 对话表（会话内多轮消息）
-- -----------------------------------------------------------------------------
CREATE TABLE `ai_dialog` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `session_id`  VARCHAR(64)     NOT NULL COMMENT '会话ID，同一会话多轮相同',
    `role`        VARCHAR(16)     NOT NULL COMMENT '角色：user 用户 / assistant 助手',
    `content`     TEXT            NOT NULL COMMENT '消息内容',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_dialog_user_id` (`user_id`),
    KEY `idx_ai_dialog_session_id` (`session_id`),
    CONSTRAINT `fk_ai_dialog_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话表';

-- -----------------------------------------------------------------------------
-- 身体数据记录表
-- -----------------------------------------------------------------------------
CREATE TABLE `body_data_record` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `record_date`  DATE            NOT NULL COMMENT '记录日期',
    `weight`       DECIMAL(5, 2)   NOT NULL COMMENT '体重（kg）',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_user_date` (`user_id`, `record_date`),
    KEY `idx_body_user_id` (`user_id`),
    CONSTRAINT `fk_body_data_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='身体数据记录表';
