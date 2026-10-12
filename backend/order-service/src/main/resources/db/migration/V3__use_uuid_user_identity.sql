ALTER TABLE orders_0 MODIFY COLUMN user_id CHAR(36) NOT NULL;
ALTER TABLE orders_1 MODIFY COLUMN user_id CHAR(36) NOT NULL;
ALTER TABLE order_items_0 MODIFY COLUMN user_id CHAR(36) NOT NULL;
ALTER TABLE order_items_1 MODIFY COLUMN user_id CHAR(36) NOT NULL;
ALTER TABLE processed_event MODIFY COLUMN user_id CHAR(36) NOT NULL;
ALTER TABLE order_outbox MODIFY COLUMN user_id CHAR(36) NOT NULL;

UPDATE orders_0 SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513' WHERE user_id = '77';
UPDATE orders_1 SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513' WHERE user_id = '77';
UPDATE order_items_0 SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513' WHERE user_id = '77';
UPDATE order_items_1 SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513' WHERE user_id = '77';
UPDATE processed_event SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513' WHERE user_id = '77';
UPDATE order_outbox SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513' WHERE user_id = '77';
