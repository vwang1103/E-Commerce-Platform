CREATE TABLE inventory (
    product_id BIGINT NOT NULL,
    total_quantity BIGINT NOT NULL,
    available_quantity BIGINT NOT NULL,
    reserved_quantity BIGINT NOT NULL DEFAULT 0,
    sold_quantity BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_id),
    CONSTRAINT chk_inventory_nonnegative CHECK (
        total_quantity >= 0
        AND available_quantity >= 0
        AND reserved_quantity >= 0
        AND sold_quantity >= 0
    ),
    CONSTRAINT chk_inventory_conserved CHECK (
        total_quantity = available_quantity + reserved_quantity + sold_quantity
    )
) ENGINE=InnoDB;

CREATE TABLE inventory_reservation (
    reservation_id CHAR(36) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (reservation_id),
    UNIQUE KEY uk_inventory_reservation_order (order_id),
    KEY idx_inventory_reservation_expiry (status, expires_at),
    CONSTRAINT fk_inventory_reservation_product FOREIGN KEY (product_id) REFERENCES inventory (product_id),
    CONSTRAINT chk_inventory_reservation_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE TABLE purchase_claim (
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id, product_id),
    UNIQUE KEY uk_purchase_claim_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE inventory_outbox (
    event_id CHAR(36) NOT NULL,
    aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload JSON NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    PRIMARY KEY (event_id),
    KEY idx_inventory_outbox_unpublished (published_at, occurred_at)
) ENGINE=InnoDB;
