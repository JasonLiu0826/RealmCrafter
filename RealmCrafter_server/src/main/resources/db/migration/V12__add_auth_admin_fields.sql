-- 鉴权与管理员：user 的 password、role 已由 V1 基表包含。
-- 若从无 V1 的旧库升级（未含 password/role），请手工执行：
--   ALTER TABLE user ADD COLUMN password VARCHAR(255) NULL COMMENT 'BCrypt';
--   ALTER TABLE user ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT 'USER|ADMIN|SUPER_ADMIN';
-- 封禁时间 sealed_until 等见 V11。

SELECT 1;
