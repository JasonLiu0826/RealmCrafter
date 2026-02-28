-- 鉴权与管理员：用户密码（BCrypt）、角色；封禁 sealed_until 见 V11
-- 若表已含 password/role 列（如由 JPA 生成），请勿重复执行或先手工删除列再执行

ALTER TABLE `user`
  ADD COLUMN `password` VARCHAR(255) NULL COMMENT 'BCrypt 加密存储，第三方登录用户可为空';
ALTER TABLE `user`
  ADD COLUMN `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT 'USER | ADMIN | SUPER_ADMIN';
