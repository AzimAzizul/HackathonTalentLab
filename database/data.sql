INSERT INTO users (id, name, email) VALUES
    (1, 'Alice', 'alice@example.com'),
    (2, 'Bob', 'bob@example.com'),
    (3, 'Charlie', 'charlie@example.com'),
    (4, 'David', 'david@example.com'),
    (5, 'Eve', 'eve@example.com');

INSERT INTO products (id, name, description, price, available_quantity) VALUES
    (1, 'Mechanical Keyboard', 'Compact mechanical keyboard with tactile switches.', 3499.00, 12),
    (2, 'Wireless Mouse', 'Wireless mouse for everyday work.', 1299.00, 20),
    (3, 'USB-C Hub', 'USB-C hub with HDMI and USB ports.', 2199.00, 8),
    (4, 'Laptop Stand', 'Aluminium laptop stand with adjustable height.', 1799.00, 10),
    (5, 'Noise Cancelling Headphones', 'Over-ear headphones. Currently out of stock.', 5999.00, 0),
    (6, 'Cotton T-Shirt', 'Regular fit cotton t-shirt.', 799.00, 25),
    (7, 'Blue Jeans', 'Classic straight fit blue jeans.', 1999.00, 15),
    (8, 'Denim Jacket', 'Casual denim jacket.', 2999.00, 7),
    (9, 'Running Shoes', 'Lightweight running shoes.', 3499.00, 18),
    (10, 'Canvas Backpack', 'Everyday backpack with laptop sleeve.', 1599.00, 12);

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
