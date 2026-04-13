INSERT INTO tb_user (id_user, name_user, email_user, password_user, phone_user, address_user, role_user)
VALUES
    (1, 'Ana Silva', 'ana@techcellshop.com', '123456', '+55 11 90000-0001', 'Sao Paulo - SP', 'USER'),
    (2, 'Carlos Souza', 'carlos@techcellshop.com', '123456', '+55 11 90000-0002', 'Campinas - SP', 'ADMIN');

INSERT INTO tb_device (
    id_device,
    name_device,
    description_device,
    device_type,
    device_storage,
    device_ram,
    device_color,
    device_price,
    device_stock,
    device_condition
)
VALUES
    (1, 'Galaxy S24', 'Samsung smartphone 256GB', 'SMARTPHONE', '256GB', '8GB', 'Black', 3999.90, 10, 'NEW'),
    (2, 'ThinkPad X1 Carbon', 'Lenovo laptop 1TB SSD', 'LAPTOP', '1TB', '16GB', 'Gray', 8999.00, 5, 'REFURBISHED');

INSERT INTO tb_order (
    id_order,
    id_user,
    id_device,
    user_id_snapshot,
    user_name_snapshot,
    user_email_snapshot,
    device_id_snapshot,
    device_name_snapshot,
    unit_price_snapshot,
    quantity_order,
    total_price_order,
    status_order,
    order_date,
    delivery_date,
    payment_method,
    payment_status_order,
    discount_amount_order,
    final_amount_order
)
VALUES
    (
        1,
        1,
        1,
        1,
        'Ana Silva',
        'ana@techcellshop.com',
        1,
        'Galaxy S24',
        3999.90,
        1,
        3999.90,
        'CREATED',
        '2026-03-23',
        '2026-03-27',
        'PIX',
        'CONFIRMED',
        0.00,
        3999.90
    ),
    (
        2,
        2,
        2,
        2,
        'Carlos Souza',
        'carlos@techcellshop.com',
        2,
        'ThinkPad X1 Carbon',
        8999.00,
        1,
        8999.00,
        'PAID',
        '2026-03-23',
        '2026-03-29',
        'CREDIT_CARD',
        'CONFIRMED',
        0.00,
        8999.00
    );

SELECT setval(
               pg_get_serial_sequence('tb_user', 'id_user'),
               GREATEST((SELECT COALESCE(MAX(id_user), 0) FROM tb_user), 9),
               true
       );

SELECT setval(
               pg_get_serial_sequence('tb_device', 'id_device'),
               GREATEST((SELECT COALESCE(MAX(id_device), 0) FROM tb_device), 9),
               true
       );

SELECT setval(
               pg_get_serial_sequence('tb_order', 'id_order'),
               GREATEST((SELECT COALESCE(MAX(id_order), 0) FROM tb_order), 9),
               true
       );