-- 鉴权与资料：手机号、第三方 ID、昵称、签名、封禁
ALTER TABLE `user`
  ADD COLUMN `phone` VARCHAR(20) NULL UNIQUE COMMENT '手机号',
  ADD COLUMN `wechat_open_id` VARCHAR(64) NULL UNIQUE COMMENT '微信 OpenID',
  ADD COLUMN `apple_id` VARCHAR(128) NULL UNIQUE COMMENT '苹果登录唯一标识',
  ADD COLUMN `nickname` VARCHAR(64) NULL COMMENT '昵称',
  ADD COLUMN `signature` VARCHAR(256) NULL COMMENT '个人签名',
  ADD COLUMN `sealed_until` DATETIME NULL COMMENT '封禁截止时间，NULL 表示未封禁';

-- 故事：管理员强制下架（status 已为 VARCHAR，无需改长度，仅说明新增 TAKEN_DOWN 枚举值）

-- 支付流水幂等：外部订单号防重复入账
ALTER TABLE `wallet_transaction`
  ADD COLUMN `external_order_id` VARCHAR(128) NULL COMMENT '支付渠道订单号，用于回调幂等',
  ADD UNIQUE KEY `uk_external_order` (`external_order_id`);
