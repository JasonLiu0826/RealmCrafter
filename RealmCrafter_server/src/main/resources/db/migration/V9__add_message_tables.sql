-- 私信消息表
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `sender_id` BIGINT NOT NULL,
  `receiver_id` BIGINT NOT NULL,
  `msg_type` VARCHAR(24) NOT NULL COMMENT 'TEXT|FORWARD_CARD',
  `content` TEXT NOT NULL,
  `is_read` TINYINT(1) NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_message_sender_receiver_time` (`sender_id`, `receiver_id`, `create_time`),
  KEY `idx_message_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信消息';

-- 会话摘要表：最近聊天列表
CREATE TABLE IF NOT EXISTS `message_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '当前用户（会话归属视角）',
  `peer_id` BIGINT NOT NULL COMMENT '对方用户',
  `last_message_id` BIGINT NULL,
  `last_message_preview` VARCHAR(256) NULL,
  `last_message_at` DATETIME NULL,
  `unread_count` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_session_user_peer` (`user_id`, `peer_id`),
  KEY `idx_session_user_updated` (`user_id`, `last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信会话摘要';
