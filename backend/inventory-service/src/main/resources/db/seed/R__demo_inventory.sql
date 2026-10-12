-- Product IDs match the catalog demo seed. Every row preserves total = available + reserved + sold.
INSERT INTO inventory
    (product_id, total_quantity, available_quantity, reserved_quantity, sold_quantity, version)
VALUES
    (1001, 48, 48, 0, 0, 0),
    (1002, 32, 32, 0, 0, 0),
    (1003, 60, 42, 0, 18, 0),
    (1004, 24, 24, 0, 0, 0),
    (1005, 90, 90, 0, 0, 0),
    (1006, 60, 60, 0, 0, 0),
    (1007, 38, 24, 0, 14, 0),
    (1008, 65, 45, 0, 20, 0),
    (1009, 85, 58, 0, 27, 0),
    (1010, 110, 62, 0, 48, 0),
    (1011, 12, 3, 0, 9, 0),
    (1012, 20, 0, 0, 20, 0),
    (1013, 34, 28, 0, 6, 0),
    (1014, 18, 11, 0, 7, 0),
    (1015, 26, 19, 0, 7, 0),
    (1016, 14, 8, 0, 6, 0),
    (1017, 40, 31, 0, 9, 0),
    (1018, 55, 55, 0, 0, 0),
    (1019, 36, 36, 0, 0, 0),
    (1020, 42, 42, 0, 0, 0),
    (1021, 30, 30, 0, 0, 0),
    (1022, 64, 64, 0, 0, 0),
    (1023, 48, 48, 0, 0, 0) AS new_inventory
ON DUPLICATE KEY UPDATE product_id = new_inventory.product_id;
