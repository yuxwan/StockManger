-- 订单明细表增加“改价前原价”列（临时特价标记用）
ALTER TABLE order_item ADD COLUMN original_price DECIMAL(10,2) NULL COMMENT '改价前原价（临时特价才有值）' AFTER price;
