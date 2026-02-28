-- 创作者等级与经验：user 表增加 exp、金牌标志；经验流水表用于每日防刷
ALTER TABLE `user` ADD COLUMN `exp` BIGINT NOT NULL DEFAULT 0 COMMENT '经验值';
ALTER TABLE `user` ADD COLUMN `is_golden_creator` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '金牌签约创作者';

CREATE TABLE IF NOT EXISTS `user_exp_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `action_type` VARCHAR(32) NOT NULL COMMENT 'PUBLISH_STORY, BE_LIKED, ...',
  `exp_gained` BIGINT NOT NULL,
  `create_date` DATE NOT NULL COMMENT '按日统计防刷',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user_exp_log_user_date` (`user_id`, `create_date`),
  KEY `idx_user_exp_log_user_date_action` (`user_id`, `create_date`, `action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户经验流水';
