-- ============================================================
-- 用户引擎配置表、广告观看流水表（应用依赖，未在 数据库.md 中）
-- ============================================================

-- 用户高阶 AI 参数配置（与 user 一对一）
CREATE TABLE IF NOT EXISTS `user_config` (
  `user_id` BIGINT NOT NULL PRIMARY KEY COMMENT '用户ID',
  `preferred_model` VARCHAR(64) DEFAULT 'realm_crafter_v1' COMMENT '首选大模型名称',
  `chaos_level` DOUBLE NOT NULL DEFAULT 0.70 COMMENT '混沌阈值(Temperature)',
  `memory_depth` INT NOT NULL DEFAULT 4000 COMMENT '记忆溯源深度(上下文Token数)',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_user_config_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户引擎配置表';

-- 广告观看激励流水（对账与奖励追踪）
CREATE TABLE IF NOT EXISTS `ad_view_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `ad_platform` VARCHAR(16) NOT NULL COMMENT 'ADMOB|PANGLE|UNITY',
  `ad_type` VARCHAR(32) NOT NULL COMMENT 'REWARD_VIDEO|INTERSTITIAL',
  `reward_granted` VARCHAR(64) NOT NULL COMMENT '发放奖励(如5000_TOKEN/1_HOUR_AD_FREE)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user_time` (`user_id`, `create_time`),
  CONSTRAINT `fk_ad_view_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告观看流水表';
