ALTER TABLE product
    CHANGE COLUMN seller_id user_id CHAR(36) NOT NULL;

UPDATE product
SET user_id = CASE user_id
    WHEN '77' THEN '0337c503-6233-385f-ab7b-0a4c48e29513'
    WHEN '78' THEN 'd6c2be7b-8f38-3e6b-957f-984c576b87d4'
    WHEN '79' THEN '8008dafb-1899-3ea0-8eb4-634c558cfb08'
    ELSE LOWER(CONCAT(
        SUBSTRING(MD5(CONCAT('arcade:user:', user_id)), 1, 8), '-',
        SUBSTRING(MD5(CONCAT('arcade:user:', user_id)), 9, 4), '-3',
        SUBSTRING(MD5(CONCAT('arcade:user:', user_id)), 14, 3), '-8',
        SUBSTRING(MD5(CONCAT('arcade:user:', user_id)), 18, 3), '-',
        SUBSTRING(MD5(CONCAT('arcade:user:', user_id)), 21, 12)))
END;

CREATE INDEX idx_product_user_status ON product (user_id, status);
