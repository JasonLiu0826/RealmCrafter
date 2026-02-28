-- 故事流量权重：定时任务写库，广场 TRAFFIC 排序用
ALTER TABLE `story` ADD COLUMN `traffic_weight` DOUBLE NULL COMMENT '流量权重=(likes+forks*2)*等级系数';
