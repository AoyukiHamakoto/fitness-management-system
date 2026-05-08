-- 修复或插入演示用户（密码 demo123）。在 PowerShell 中勿将本文件内容内联到 -e 参数，以免 $ 被误解析。
USE `fitness_mvp`;

INSERT INTO `user` (`username`, `phone`, `password`, `nickname`, `status`)
VALUES (
    'demo',
    '13800138000',
    '$2a$10$MtSAtc62oSBeNRTJPDJEwusPplHKxNxU.nYLquOq5GFIkOPvlGpBu',
    '演示用户',
    1
)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `nickname` = VALUES(`nickname`),
    `status` = VALUES(`status`);
