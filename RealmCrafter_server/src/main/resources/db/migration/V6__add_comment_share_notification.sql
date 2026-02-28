-- 多态评论表：段落/选项/评论锚点 + 楼中楼
CREATE TABLE IF NOT EXISTS `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `story_id` VARCHAR(32) NOT NULL,
  `chapter_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `likes_count` INT NOT NULL DEFAULT 0,
  `reply_count` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  `target_type` VARCHAR(24) NOT NULL COMMENT 'PARAGRAPH|OPTION|COMMENT',
  `target_ref` VARCHAR(64) NOT NULL,
  `root_comment_id` BIGINT NULL,
  `parent_comment_id` BIGINT NULL,
  KEY `idx_comment_story_chapter_anchor` (`story_id`, `chapter_id`, `target_type`, `target_ref`),
  KEY `idx_comment_root` (`root_comment_id`),
  KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='段评/选项评/楼中楼';

-- 分享记录：短链与锚点映射（站外深度链接）
CREATE TABLE IF NOT EXISTS `share_record` (
  `short_code` VARCHAR(16) NOT NULL PRIMARY KEY,
  `type` VARCHAR(24) NOT NULL COMMENT 'PARAGRAPH|OPTION|COMMENT',
  `story_id` VARCHAR(32) NOT NULL,
  `chapter_id` BIGINT NOT NULL,
  `target_ref` VARCHAR(64) NULL,
  `excerpt` VARCHAR(512) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_share_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转发短链映射';

-- 系统通知：@提及 等
CREATE TABLE IF NOT EXISTS `system_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'MENTION 等',
  `title` VARCHAR(128) NULL,
  `body` TEXT NULL,
  `ref_type` VARCHAR(32) NULL,
  `ref_id` VARCHAR(64) NULL,
  `actor_user_id` BIGINT NULL,
  `is_read` TINYINT(1) NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_notification_user_unread` (`user_id`, `is_read`),
  KEY `idx_notification_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知';
