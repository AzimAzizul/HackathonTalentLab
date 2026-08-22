INSERT INTO users (id, name, email, role) VALUES
    (1, 'Alice', 'alice@example.com', 'CUSTOMER'),
    (2, 'Bob', 'bob@example.com', 'CUSTOMER'),
    (3, 'Charlie', 'charlie@example.com', 'CUSTOMER'),
    (4, 'David', 'david@example.com', 'CUSTOMER'),
    (5, 'Eve', 'eve@example.com', 'CUSTOMER'),
    (6, 'Admin', 'admin@shop.test', 'ADMIN'),
    (7, 'Fiona', 'fiona@example.com', 'CUSTOMER'),
    (8, 'George', 'george@example.com', 'CUSTOMER'),
    (9, 'Hannah', 'hannah@example.com', 'CUSTOMER'),
    (10, 'Ian', 'ian@example.com', 'CUSTOMER'),
    (11, 'Julia', 'julia@example.com', 'CUSTOMER'),
    (12, 'Kevin', 'kevin@example.com', 'CUSTOMER'),
    (13, 'Laura', 'laura@example.com', 'CUSTOMER'),
    (14, 'Mark', 'mark@example.com', 'CUSTOMER'),
    (15, 'Nina', 'nina@example.com', 'CUSTOMER'),
    (16, 'Oscar', 'oscar@example.com', 'CUSTOMER'),
    (17, 'Peter', 'peter@example.com', 'CUSTOMER'),
    (18, 'Quentin', 'quentin@example.com', 'CUSTOMER'),
    (19, 'Rachel', 'rachel@example.com', 'CUSTOMER'),
    (20, 'Sam', 'sam@example.com', 'CUSTOMER'),
    (21, 'Tina', 'tina@example.com', 'CUSTOMER'),
    (22, 'Umar', 'umar@example.com', 'CUSTOMER'),
    (23, 'Vera', 'vera@example.com', 'CUSTOMER'),
    (24, 'Will', 'will@example.com', 'CUSTOMER');

INSERT INTO products (id, name, description, price, available_quantity, variant_name, variant_options) VALUES
    (1, 'Mechanical Keyboard', 'Compact mechanical keyboard with tactile switches.', 189.00, 12, 'Switch Type', 'Tactile,Linear'),
    (2, 'Wireless Mouse', 'Wireless mouse for everyday work.', 70.00, 20, 'Color', 'Black,White'),
    (3, 'USB-C Hub', 'USB-C hub with HDMI and USB ports.', 119.00, 8, 'Charging Tier', 'Standard,Fast Charging'),
    (4, 'Cooling Fan Pad', 'cooling fan that cools down your laptop', 97.00, 20, 'Colour', 'Black,Grey'),
    (5, 'Noise Cancelling Headphones', 'Over-ear headphones. Currently out of stock.', 324.00, 0, 'Color', 'Black,Silver'),
    (6, 'Cotton T-Shirt', 'Regular fit cotton t-shirt.', 43.00, 25, 'Size', 'M,L'),
    (7, 'Blue Jeans', 'Classic straight fit blue jeans.', 108.00, 15, 'Size', '32,34'),
    (8, 'Denim Jacket', 'Casual denim jacket.', 162.00, 7, 'Size', 'M,L'),
    (9, 'Running Shoes', 'Lightweight running shoes.', 189.00, 18, 'Size', '9,10'),
    (10, 'Canvas Backpack', 'Everyday backpack with laptop sleeve.', 86.00, 12, 'Color', 'Black,Navy'),
    (11, 'Sweater', 'Cosy knit sweater.', 88.00, 14, 'Size', 'M,L'),
    (12, 'Cargo Pants', 'Relaxed fit cargo pants.', 132.00, 12, 'Size', '32,34'),
    (13, 'Polo Shirt', 'Classic cotton polo shirt.', 49.00, 22, 'Size', 'M,L'),
    (14, 'Leather Belt', 'Genuine leather belt.', 55.00, 30, 'Colour', 'Black,Brown'),
    (15, 'Bluetooth Speaker', 'Portable wireless speaker.', 145.00, 16, 'Colour', 'Black,Navy'),
    (16, 'Smart Watch', 'Fitness-tracking smart watch.', 260.00, 9, 'Colour', 'Black,Silver'),
    (17, 'Wireless Earbuds', 'True wireless earbuds.', 120.00, 18, 'Colour', 'White,Black'),
    (18, 'Webcam', 'HD webcam for calls.', 110.00, 11, 'Colour', 'Black,White'),
    (19, 'Water Bottle', 'Insulated steel water bottle.', 35.00, 40, 'Colour', 'Blue,Green'),
    (20, 'Travel Mug', 'Leak-proof travel mug.', 40.00, 35, 'Colour', 'Black,White'),
    (21, 'Umbrella', 'Compact foldable umbrella.', 45.00, 28, 'Colour', 'Black,Navy'),
    (22, 'Notebook Set', 'A5 notebooks, set of 3.', 25.00, 50, 'Colour', 'Blue,Red');

INSERT INTO orders (id, user_id, status, total_amount, created_at) VALUES
    (1, 1, 'PLACED', 259.00, CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (2, 1, 'PLACED', 43.00, CURRENT_TIMESTAMP - INTERVAL '12 days'),
    (3, 1, 'PLACED', 189.00, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (4, 2, 'PLACED', 119.00, CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (5, 4, 'PLACED', 86.00, CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (6, 4, 'PLACED', 108.00, CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (7, 18, 'PLACED', 43.00, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (8, 19, 'PLACED', 49.00, CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (9, 19, 'PLACED', 132.00, CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (10, 20, 'PLACED', 40.00, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (11, 20, 'PLACED', 43.00, CURRENT_TIMESTAMP - INTERVAL '12 days'),
    (12, 20, 'PLACED', 25.00, CURRENT_TIMESTAMP - INTERVAL '20 days');

INSERT INTO order_items (id, order_id, product_id, product_name, price, quantity) VALUES
    (1, 1, 1, 'Mechanical Keyboard', 189.00, 1),
    (2, 1, 2, 'Wireless Mouse', 70.00, 1),
    (3, 2, 6, 'Cotton T-Shirt', 43.00, 1),
    (4, 3, 9, 'Running Shoes', 189.00, 1),
    (5, 4, 3, 'USB-C Hub', 119.00, 1),
    (6, 5, 10, 'Canvas Backpack', 86.00, 1),
    (7, 6, 7, 'Blue Jeans', 108.00, 1),
    (8, 7, 6, 'Cotton T-Shirt', 43.00, 1),
    (9, 8, 13, 'Polo Shirt', 49.00, 1),
    (10, 9, 12, 'Cargo Pants', 132.00, 1),
    (11, 10, 20, 'Travel Mug', 40.00, 1),
    (12, 11, 6, 'Cotton T-Shirt', 43.00, 1),
    (13, 12, 22, 'Notebook Set', 25.00, 1);

INSERT INTO carts (id, user_id, status, created_at, last_activity_at, abandoned_at) VALUES
    (1, 1, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL),
    (2, 2, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (3, 3, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (4, 7, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (5, 8, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (6, 9, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (7, 10, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (8, 11, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (9, 12, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (10, 13, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '13 days', CURRENT_TIMESTAMP - INTERVAL '11 days', CURRENT_TIMESTAMP - INTERVAL '11 days'),
    (11, 14, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '17 days', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (12, 15, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '21 days', CURRENT_TIMESTAMP - INTERVAL '19 days', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    (13, 16, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '23 days', CURRENT_TIMESTAMP - INTERVAL '23 days'),
    (14, 17, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '29 days', CURRENT_TIMESTAMP - INTERVAL '27 days', CURRENT_TIMESTAMP - INTERVAL '27 days');

INSERT INTO cart_items (id, cart_id, product_id, quantity, variant) VALUES
    (1, 1, 8, 1, 'M'),
    (2, 2, 1, 1, 'Tactile'),
    (3, 2, 2, 1, 'Black'),
    (4, 3, 5, 1, 'Black'),
    (5, 4, 14, 1, 'Black'),
    (6, 5, 11, 1, 'M'),
    (7, 6, 16, 1, 'Black'),
    (8, 7, 17, 1, 'White'),
    (9, 8, 18, 1, 'Black'),
    (10, 9, 15, 1, 'Black'),
    (11, 10, 19, 2, 'Blue'),
    (12, 11, 20, 1, 'Black'),
    (13, 12, 21, 1, 'Black'),
    (14, 13, 22, 2, 'Blue'),
    (15, 14, 13, 2, 'M');

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('order_items_id_seq', (SELECT MAX(id) FROM order_items));
SELECT setval('carts_id_seq', (SELECT MAX(id) FROM carts));
SELECT setval('cart_items_id_seq', (SELECT MAX(id) FROM cart_items));
