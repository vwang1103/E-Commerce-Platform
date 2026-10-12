CREATE TABLE orders_0 (
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    payment_deadline TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_orders_0_user_idempotency (user_id, idempotency_key),
    KEY idx_orders_0_user_created (user_id, created_at),
    KEY idx_orders_0_expiry (status, payment_deadline),
    CONSTRAINT chk_orders_0_total_nonnegative CHECK (total_minor >= 0)
) ENGINE=InnoDB;

CREATE TABLE orders_1 LIKE orders_0;

CREATE TABLE order_items_0 (
    order_item_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price_minor BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    PRIMARY KEY (order_item_id),
    KEY idx_order_items_0_order (order_id, user_id),
    CONSTRAINT chk_order_items_0_values CHECK (unit_price_minor >= 0 AND quantity > 0)
) ENGINE=InnoDB;

CREATE TABLE order_items_1 LIKE order_items_0;

CREATE TABLE processed_event (
    consumer_name VARCHAR(120) NOT NULL,
    event_id CHAR(36) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (consumer_name, event_id)
) ENGINE=InnoDB;

CREATE TABLE order_outbox (
    event_id CHAR(36) NOT NULL,
    aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    partition_key VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    PRIMARY KEY (event_id),
    KEY idx_order_outbox_unpublished (published_at, occurred_at)
) ENGINE=InnoDB;
