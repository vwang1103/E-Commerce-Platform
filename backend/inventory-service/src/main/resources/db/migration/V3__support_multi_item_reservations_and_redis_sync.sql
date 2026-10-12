ALTER TABLE inventory_reservation
    DROP INDEX uk_inventory_reservation_order,
    ADD UNIQUE KEY uk_inventory_reservation_order_product (order_id, product_id);

CREATE TABLE inventory_redis_sync (
    sync_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    applied_at TIMESTAMP(6) NULL,
    PRIMARY KEY (sync_id),
    UNIQUE KEY uk_inventory_redis_sync_operation (order_id, product_id, operation),
    KEY idx_inventory_redis_sync_pending (applied_at, sync_id),
    CONSTRAINT chk_inventory_redis_sync_operation CHECK (operation IN ('CONFIRM', 'CANCEL'))
) ENGINE=InnoDB;
