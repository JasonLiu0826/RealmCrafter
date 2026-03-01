-- 一次性修复 JPA schema 校验：与实体对齐（ENUM->VARCHAR、补缺列）
-- 执行：mysql -u root -pLiuYF20050826 -h localhost realmcrafter < scripts/fix_schema_for_jpa.sql
-- 若某列已存在会报 Duplicate column，可忽略，继续执行后续语句。

-- 1. message 表：补 content（TEXT 不能 DEFAULT，先 NULL 再填再改 NOT NULL），msg_type 改为 VARCHAR
ALTER TABLE message ADD COLUMN content TEXT NULL;
UPDATE message SET content = '' WHERE content IS NULL;
ALTER TABLE message MODIFY COLUMN content TEXT NOT NULL;
ALTER TABLE message MODIFY COLUMN msg_type VARCHAR(24) NOT NULL COMMENT 'TEXT|FORWARD_CARD';

-- 2. setting_pack.status ENUM -> VARCHAR(32)
ALTER TABLE setting_pack MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL|PENDING_REVIEW|FROZEN_COPYRIGHT';

-- 3. story.status ENUM -> VARCHAR(32)
ALTER TABLE story MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL|DELETED|TAKEN_DOWN';

-- 4. system_notification：补列 + type 改为 VARCHAR
ALTER TABLE system_notification ADD COLUMN actor_user_id BIGINT NULL;
ALTER TABLE system_notification ADD COLUMN ref_type VARCHAR(32) NULL;
ALTER TABLE system_notification ADD COLUMN ref_id VARCHAR(64) NULL;
ALTER TABLE system_notification ADD COLUMN body TEXT NULL;
ALTER TABLE system_notification MODIFY COLUMN type VARCHAR(32) NOT NULL COMMENT 'SYSTEM|MENTION|INTERACTION|REWARD|LEVEL_UP';
