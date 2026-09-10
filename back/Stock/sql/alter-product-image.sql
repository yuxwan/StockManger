-- 商品图支持：product 表新增 image 字段
-- 说明：仅需执行一次；若提示 column already exists 说明已加过，可忽略。
ALTER TABLE product ADD COLUMN image VARCHAR(500) DEFAULT NULL COMMENT '商品图URL';
