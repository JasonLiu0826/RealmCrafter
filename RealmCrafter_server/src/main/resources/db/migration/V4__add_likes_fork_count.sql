-- 故事与设定集：广场统计字段（避免大表实时 COUNT 性能问题）
ALTER TABLE `story` ADD COLUMN `likes_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数';
ALTER TABLE `story` ADD COLUMN `fork_count` INT NOT NULL DEFAULT 0 COMMENT '克隆/Fork 数';
ALTER TABLE `story` ADD COLUMN `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数';

ALTER TABLE `setting_pack` ADD COLUMN `likes_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数';
ALTER TABLE `setting_pack` ADD COLUMN `fork_count` INT NOT NULL DEFAULT 0 COMMENT '克隆/Fork 数';
ALTER TABLE `setting_pack` ADD COLUMN `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数';

-- 点赞/收藏记录表（幂等加减 DO 计数）
CREATE TABLE IF NOT EXISTS `asset_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `asset_type` VARCHAR(16) NOT NULL,
  `asset_id` VARCHAR(32) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_asset` (`user_id`, `asset_type`, `asset_id`),
  KEY `idx_asset` (`asset_type`, `asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产点赞记录';

CREATE TABLE IF NOT EXISTS `asset_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `asset_type` VARCHAR(16) NOT NULL,
  `asset_id` VARCHAR(32) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_asset` (`user_id`, `asset_type`, `asset_id`),
  KEY `idx_asset` (`asset_type`, `asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产收藏记录';
