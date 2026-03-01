-- 方案 A：payload 按普通文本存储，避免 MySQL JSON 严格校验导致私信写入失败
-- 若库中 payload 为 JSON 则改为 TEXT；若尚无 payload 列需先执行: ALTER TABLE message ADD COLUMN payload TEXT NULL;
ALTER TABLE message MODIFY COLUMN payload TEXT NULL;
