ALTER TABLE inventory_reservation MODIFY COLUMN user_id CHAR(36) NOT NULL;
ALTER TABLE purchase_claim MODIFY COLUMN user_id CHAR(36) NOT NULL;

UPDATE inventory_reservation
SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513'
WHERE user_id = '77';

UPDATE purchase_claim
SET user_id = '0337c503-6233-385f-ab7b-0a4c48e29513'
WHERE user_id = '77';
