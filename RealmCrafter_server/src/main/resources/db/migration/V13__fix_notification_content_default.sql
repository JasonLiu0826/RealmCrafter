-- 物理解决 content/body 无默认值导致的 SQL 1364 错误（直接改库）
-- 1. message 表：content 必填，给默认空串（MySQL 8.0.13+ 支持 TEXT DEFAULT ''）
ALTER TABLE message MODIFY COLUMN content TEXT NOT NULL DEFAULT '';

-- 2. system_notification：body 允许 NULL 并明确默认
ALTER TABLE system_notification MODIFY COLUMN body TEXT NULL DEFAULT NULL;

-- 3. system_notification.content：若列存在则改为带默认值；若不存在则新增（避免 1364）
DELIMITER //
DROP PROCEDURE IF EXISTS fix_system_notification_content//
CREATE PROCEDURE fix_system_notification_content()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_notification' AND COLUMN_NAME = 'content') = 0 THEN
    ALTER TABLE system_notification ADD COLUMN content VARCHAR(1000) NOT NULL DEFAULT '';
  ELSE
    ALTER TABLE system_notification MODIFY COLUMN content VARCHAR(1000) NOT NULL DEFAULT '';
  END IF;
END //
DELIMITER ;
CALL fix_system_notification_content();
DROP PROCEDURE IF EXISTS fix_system_notification_content;
