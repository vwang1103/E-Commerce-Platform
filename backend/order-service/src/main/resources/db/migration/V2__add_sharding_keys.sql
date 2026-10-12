ALTER TABLE processed_event
    ADD COLUMN user_id BIGINT NULL FIRST;

DELETE FROM processed_event;

ALTER TABLE processed_event
    DROP PRIMARY KEY,
    MODIFY COLUMN user_id BIGINT NOT NULL,
    ADD PRIMARY KEY (user_id, consumer_name, event_id);

ALTER TABLE order_outbox
    ADD COLUMN user_id BIGINT NULL AFTER event_id;

UPDATE order_outbox AS outbox
JOIN orders_0 AS customer_order ON customer_order.order_id = CAST(outbox.aggregate_id AS UNSIGNED)
SET outbox.user_id = customer_order.user_id
WHERE outbox.user_id IS NULL;

UPDATE order_outbox AS outbox
JOIN orders_1 AS customer_order ON customer_order.order_id = CAST(outbox.aggregate_id AS UNSIGNED)
SET outbox.user_id = customer_order.user_id
WHERE outbox.user_id IS NULL;

DELETE FROM order_outbox WHERE user_id IS NULL;

ALTER TABLE order_outbox
    MODIFY COLUMN user_id BIGINT NOT NULL,
    ADD KEY idx_order_outbox_user_publish (user_id, published_at, occurred_at);
