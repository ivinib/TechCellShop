MERGE INTO tb_user (id_user, name_user, email_user, password_user, phone_user, address_user, role_user)
    KEY (id_user)
    VALUES (1, 'Ana Silva', 'ana@techcellshop.com', '123456', '+55 11 90000-0001', 'Sao Paulo - SP', 'CUSTOMER');

MERGE INTO tb_user (id_user, name_user, email_user, password_user, phone_user, address_user, role_user)
    KEY (id_user)
    VALUES (2, 'Carlos Souza', 'carlos@techcellshop.com', '123456', '+55 11 90000-0002', 'Campinas - SP', 'ADMIN');

MERGE INTO tb_device (id_device, name_device, description_device, device_type, device_storage, device_ram, device_color, device_price, device_stock, device_condition)
    KEY (id_device)
    VALUES (1, 'Galaxy S24', 'Samsung smartphone 256GB', 'SMARTPHONE', '256GB', '8GB', 'Black', 3999.90, 10, 'NEW');

MERGE INTO tb_device (id_device, name_device, description_device, device_type, device_storage, device_ram, device_color, device_price, device_stock, device_condition)
    KEY (id_device)
    VALUES (2, 'ThinkPad X1 Carbon', 'Lenovo laptop 1TB SSD', 'LAPTOP', '1TB', '16GB', 'Gray', 8999.00, 5, 'REFURBISHED');

MERGE INTO tb_order (id_order, id_user, id_device, quantity_order, total_price_order, status_order, order_date, delivery_date, payment_method, payment_status)
    KEY (id_order)
    VALUES (1, 1, 1, 1, 3999.90, 'CREATED', '2026-03-23', '2026-03-27', 'PIX', 'PAID');

MERGE INTO tb_order (id_order, id_user, id_device, quantity_order, total_price_order, status_order, order_date, delivery_date, payment_method, payment_status)
    KEY (id_order)
    VALUES (2, 2, 2, 1, 8999.00, 'PROCESSING', '2026-03-23', '2026-03-29', 'CREDIT_CARD', 'AUTHORIZED');
