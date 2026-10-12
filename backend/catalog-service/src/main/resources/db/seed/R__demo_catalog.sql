-- Stable, readable local data. Explicit product IDs are shared with the inventory seed.
INSERT INTO category (category_id, parent_category_id, name, slug)
VALUES
    (400, NULL, 'Fitness and Outdoors', 'fitness-and-outdoors'),
    (500, NULL, 'Personal Care', 'personal-care'),
    (600, NULL, 'Toys and Games', 'toys-and-games') AS new_category
ON DUPLICATE KEY UPDATE
    parent_category_id = new_category.parent_category_id,
    name = new_category.name,
    slug = new_category.slug;

UPDATE category SET name = 'Books and Stationery' WHERE category_id = 300 AND name = 'Books';

INSERT INTO product
    (product_id, user_id, category_id, name, description, price_minor, currency, status, version)
VALUES
    (1001, '0337c503-6233-385f-ab7b-0a4c48e29513', 200, 'Luma Modern Table Lamp',
     'A sculptural table lamp with a warm LED glow and three touch-controlled brightness levels.',
     8900, 'USD', 'ACTIVE', 0),
    (1002, '0337c503-6233-385f-ab7b-0a4c48e29513', 100, 'Haven Smart Speaker',
     'A compact fabric smart speaker with room-filling sound and hands-free voice control.',
     7900, 'USD', 'ACTIVE', 0),
    (1003, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 100, 'Pulse Smart Watch',
     'A lightweight fitness watch with heart-rate tracking, sleep insights, and a bright display.',
     12900, 'USD', 'ACTIVE', 0),
    (1004, '0337c503-6233-385f-ab7b-0a4c48e29513', 100, 'Drift Wireless Headphones',
     'Comfortable noise-cancelling headphones with warm sound and thirty hours of battery life.',
     14900, 'USD', 'ACTIVE', 0),
    (1005, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 100, 'Roam Portable Bluetooth Speaker',
     'A rugged portable speaker with a carry loop, colorful accent light, and splash-resistant shell.',
     9900, 'USD', 'ACTIVE', 0),
    (1006, '0337c503-6233-385f-ab7b-0a4c48e29513', 100, 'Nest Home Security Camera',
     'A compact indoor camera with clear night vision, motion alerts, and two-way audio.',
     9900, 'USD', 'ACTIVE', 0),
    (1007, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 100, 'Sketch Smart Stylus',
     'A precise low-latency stylus for drawing, writing, and annotating on compatible tablets.',
     6900, 'USD', 'ACTIVE', 0),
    (1008, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 100, 'Cinema Home Speaker',
     'A powerful wireless speaker tuned for clear dialogue, balanced music, and rich bass.',
     18900, 'USD', 'ACTIVE', 0),
    (1009, '0337c503-6233-385f-ab7b-0a4c48e29513', 600, 'Skyline 1000-Piece Puzzle',
     'A city skyline puzzle printed on thick recycled board with a precise fit.',
     2200, 'USD', 'ACTIVE', 0),
    (1010, '0337c503-6233-385f-ab7b-0a4c48e29513', 100, 'Air Wireless Earbuds',
     'Pocket-size wireless earbuds with a comfortable fit, crisp calls, and a charging case.',
     8900, 'USD', 'ACTIVE', 0),
    (1011, '8008dafb-1899-3ea0-8eb4-634c558cfb08', 100, 'Vector RGB Gaming Mouse',
     'A responsive lightweight gaming mouse with programmable controls and adjustable lighting.',
     5900, 'USD', 'ACTIVE', 0),
    (1012, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 200, 'Orbit Smart Home Cleaner',
     'A compact robotic cleaner with scheduled routines and automatic room navigation.',
     24900, 'USD', 'ACTIVE', 0),
    (1013, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 100, 'Garmin Venu Smart Watch',
     'A round GPS fitness watch with heart-rate tracking, health insights, and a crisp color display.',
     34900, 'USD', 'ACTIVE', 0),
    (1014, '8008dafb-1899-3ea0-8eb4-634c558cfb08', 600, 'PlayStation 5 Console',
     'A next-generation home game console with a DualSense wireless controller and ultra-fast storage.',
     49900, 'USD', 'ACTIVE', 0),
    (1015, '0337c503-6233-385f-ab7b-0a4c48e29513', 100, 'Bose QuietComfort Headphones',
     'Midnight-blue over-ear headphones with powerful noise cancellation and a comfortable padded fit.',
     32900, 'USD', 'ACTIVE', 0),
    (1016, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 100, 'Freestyle Smart Projector',
     'A compact portable smart projector with an adjustable stand for movies on walls and ceilings.',
     59900, 'USD', 'ACTIVE', 0),
    (1017, '0337c503-6233-385f-ab7b-0a4c48e29513', 100, 'Series Sport Smart Watch',
     'A dark sport smart watch with activity rings, workout tracking, and a comfortable silicone band.',
     27900, 'USD', 'ACTIVE', 0),
    (1018, '0337c503-6233-385f-ab7b-0a4c48e29513', 300, 'Field Notes Weekly Planner',
     'A linen-bound weekly planner with undated layouts, project pages, and a smooth black pen.',
     2800, 'USD', 'ACTIVE', 0),
    (1019, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 300, 'Modern Classics Book Set',
     'Three beautifully bound contemporary novels selected for relaxed weekends and thoughtful gifting.',
     4200, 'USD', 'ACTIVE', 0),
    (1020, '8008dafb-1899-3ea0-8eb4-634c558cfb08', 400, 'Grounded Yoga Starter Kit',
     'A supportive non-slip yoga mat paired with two cork blocks and an adjustable woven strap.',
     6800, 'USD', 'ACTIVE', 0),
    (1021, '0337c503-6233-385f-ab7b-0a4c48e29513', 400, 'Summit 22L Hiking Daypack',
     'A lightweight trail backpack with padded straps, organized pockets, and space for a full day outside.',
     8400, 'USD', 'ACTIVE', 0),
    (1022, 'd6c2be7b-8f38-3e6b-957f-984c576b87d4', 500, 'Bright Sonic Toothbrush Set',
     'A rechargeable electric toothbrush with a compact charging base and two soft replacement heads.',
     5400, 'USD', 'ACTIVE', 0),
    (1023, '8008dafb-1899-3ea0-8eb4-634c558cfb08', 500, 'Daily Balance Skincare Set',
     'A gentle three-step routine with cleanser, hydrating serum, and a lightweight everyday moisturizer.',
     7600, 'USD', 'ACTIVE', 0) AS new_product
ON DUPLICATE KEY UPDATE
    user_id = new_product.user_id,
    category_id = new_product.category_id,
    name = new_product.name,
    description = new_product.description,
    price_minor = new_product.price_minor,
    currency = new_product.currency,
    status = new_product.status;

INSERT INTO catalog_outbox
    (event_id, aggregate_type, aggregate_id, event_type, payload, occurred_at)
SELECT UUID(), 'product', CAST(p.product_id AS CHAR), 'product.created',
       JSON_OBJECT(
           'productId', p.product_id,
           'userId', p.user_id,
           'categoryId', p.category_id,
           'name', p.name,
           'description', p.description,
           'priceMinor', p.price_minor,
           'currency', p.currency,
           'status', p.status,
           'version', p.version),
       p.created_at
FROM product p
WHERE p.product_id BETWEEN 1001 AND 1023
  AND NOT EXISTS (
      SELECT 1 FROM catalog_outbox o
      WHERE o.aggregate_type = 'product'
        AND o.aggregate_id = p.product_id
        AND JSON_UNQUOTE(JSON_EXTRACT(o.payload, '$.name')) = p.name
        AND JSON_UNQUOTE(JSON_EXTRACT(o.payload, '$.description')) = p.description
        AND CAST(JSON_UNQUOTE(JSON_EXTRACT(o.payload, '$.priceMinor')) AS UNSIGNED) = p.price_minor);
