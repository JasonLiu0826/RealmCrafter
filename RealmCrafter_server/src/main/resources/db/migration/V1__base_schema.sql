-- ============================================================
-- RealmCrafter 基表：用户、资金流水、设定集、故事、章节
-- 与 数据库.md 最初结构对齐；V3～V12 在此基础上增量变更。
-- 若本地已有该库表，可 Flyway baseline 至 V2 后仅执行 V3～V12。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `realmcrafter` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `realmcrafter`;

-- --------------------------------------------------------
-- 1. 用户核心表 (user)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户账号ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码哈希(BCrypt)',
  `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT 'USER | ADMIN | SUPER_ADMIN',
  `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `gender` VARCHAR(16) DEFAULT 'UNKNOWN' COMMENT 'MALE | FEMALE | UNKNOWN',
  `age` TINYINT UNSIGNED DEFAULT NULL COMMENT '年龄',
  `signature` VARCHAR(256) DEFAULT NULL COMMENT '个人签名',
  `level` INT NOT NULL DEFAULT 1 COMMENT '贡献评级(1-5级)',
  `current_theme_id` VARCHAR(32) DEFAULT 'classic_white' COMMENT '当前主题ID',
  `token_balance` BIGINT NOT NULL DEFAULT 100000 COMMENT 'AI算力余额',
  `crystal_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '虚拟币(灵能水晶)',
  `vip_expire_time` DATETIME DEFAULT NULL COMMENT 'VIP到期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号表';

-- --------------------------------------------------------
-- 2. 资金流水对账表 (wallet_transaction)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wallet_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '变动水晶数量(正负值)',
  `type` VARCHAR(32) NOT NULL COMMENT 'RECHARGE|BUY_ASSET|CREATOR_REVENUE|PENALTY_DEDUCT',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '账单明细描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟币流水表';

-- --------------------------------------------------------
-- 3. 设定集表 (setting_pack)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `setting_pack` (
  `id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT 'sd+用户ID后4位+8位随机数',
  `user_id` BIGINT NOT NULL COMMENT '创作者ID',
  `source_setting_id` VARCHAR(32) DEFAULT NULL COMMENT '血统:源设定集ID',
  `title` VARCHAR(128) NOT NULL COMMENT '标题',
  `cover` VARCHAR(512) DEFAULT NULL COMMENT '封面',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '简介',
  `content` JSON NOT NULL COMMENT '五大维度JSON数据',
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '定价(水晶)',
  `allow_modify` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许Fork后修改',
  `is_public` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否发布到广场',
  `status` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL|PENDING_REVIEW|FROZEN_COPYRIGHT',
  `version_id` BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `device_hash` VARCHAR(64) DEFAULT NULL COMMENT '最后修改设备指纹',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_setting_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设定集图纸表';

-- --------------------------------------------------------
-- 4. 故事包表 (story)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `story` (
  `id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT 'gs+用户ID后4位+8位随机数',
  `user_id` BIGINT NOT NULL COMMENT '所有者ID',
  `setting_pack_id` VARCHAR(32) NOT NULL COMMENT '关联设定集',
  `source_story_id` VARCHAR(32) DEFAULT NULL COMMENT '血统:源故事ID',
  `title` VARCHAR(128) NOT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '定价(水晶)',
  `allow_download` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '允许克隆下载',
  `clone_includes_settings` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '克隆是否带设定修改权',
  `is_public` TINYINT(1) NOT NULL DEFAULT 1,
  `status` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL|DELETED|TAKEN_DOWN',
  `version_id` BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `device_hash` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_story_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_story_setting` FOREIGN KEY (`setting_pack_id`) REFERENCES `setting_pack` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动故事表';

-- --------------------------------------------------------
-- 5. 章节内容表 (chapter)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chapter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `story_id` VARCHAR(32) NOT NULL,
  `chapter_index` INT NOT NULL COMMENT '章节序号',
  `title` VARCHAR(255) DEFAULT NULL,
  `content` TEXT NOT NULL COMMENT '净化后的正文',
  `branches_data` JSON NOT NULL COMMENT 'AI生成的JSON分支数据',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_story_index` (`story_id`, `chapter_index`),
  CONSTRAINT `fk_chapter_story` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事章节记录';
