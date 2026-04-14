-- 若库中 punch_record 已存在但缺少打卡实做字段，可执行本脚本（MySQL 8.0）
ALTER TABLE `punch_record`
    ADD COLUMN `actual_sets` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '实际完成组数' AFTER `punch_date`,
    ADD COLUMN `actual_weight` DECIMAL(6, 2) NOT NULL DEFAULT 0.00 COMMENT '实际使用重量（kg）' AFTER `actual_sets`;

ALTER TABLE `punch_record`
    ADD UNIQUE KEY `uk_punch_user_detail_date` (`user_id`, `plan_detail_id`, `punch_date`);
