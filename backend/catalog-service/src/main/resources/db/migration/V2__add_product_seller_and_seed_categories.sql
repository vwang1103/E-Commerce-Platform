ALTER TABLE product
    ADD COLUMN seller_id BIGINT NOT NULL DEFAULT 1 AFTER category_id,
    ADD KEY idx_product_seller_id (seller_id, product_id);

INSERT IGNORE INTO category (category_id, parent_category_id, name, slug)
VALUES
    (100, NULL, 'Electronics', 'electronics'),
    (200, NULL, 'Home and Kitchen', 'home-and-kitchen'),
    (300, NULL, 'Books', 'books');
