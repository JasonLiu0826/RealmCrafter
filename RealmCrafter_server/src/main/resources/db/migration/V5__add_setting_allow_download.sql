-- 设定集 Fork 权限矩阵：allow_download 控制是否允许克隆下载
ALTER TABLE `setting_pack` ADD COLUMN `allow_download` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '允许克隆下载，false 时仅支持云端引用';
