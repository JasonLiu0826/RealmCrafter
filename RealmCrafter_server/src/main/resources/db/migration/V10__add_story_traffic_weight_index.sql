-- 广场 TRAFFIC 排序：按 traffic_weight 降序查询时使用
CREATE INDEX idx_story_traffic_weight ON story (traffic_weight DESC);
