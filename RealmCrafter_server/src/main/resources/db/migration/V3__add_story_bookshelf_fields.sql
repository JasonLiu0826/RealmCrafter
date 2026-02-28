-- 书架功能补强：故事表增加标签字段（用于模糊搜索与展示）
ALTER TABLE story ADD COLUMN tags VARCHAR(1024) NULL COMMENT '故事标签，逗号分隔或JSON';
