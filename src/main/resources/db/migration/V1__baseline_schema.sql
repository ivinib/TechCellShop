CREATE TABLE tb_user (
                         id_user BIGSERIAL PRIMARY KEY,
                         name_user VARCHAR(255),
                         email_user VARCHAR(255),
                         password_user VARCHAR(255),
                         phone_user VARCHAR(255),
                         address_user VARCHAR(255),
                         role_user VARCHAR(50)
);

CREATE UNIQUE INDEX uk_tb_user_email_lower
    ON tb_user (LOWER(email_user));

CREATE TABLE tb_device (
                           id_device BIGSERIAL PRIMARY KEY,
                           name_device VARCHAR(255),
                           description_device VARCHAR(255),
                           device_type VARCHAR(50),
                           device_storage VARCHAR(50),
                           device_ram VARCHAR(50),
                           device_color VARCHAR(50),
                           device_price NUMERIC(12,2),
                           device_stock INTEGER,
                           device_condition VARCHAR(50),
                           version BIGINT DEFAULT 0
);

CREATE TABLE tb_order (
                          id_order BIGSERIAL PRIMARY KEY,
                          id_user BIGINT,
                          id_device BIGINT,
                          user_id_snapshot BIGINT,
                          user_name_snapshot VARCHAR(120),
                          user_email_snapshot VARCHAR(150),
                          device_id_snapshot BIGINT,
                          device_name_snapshot VARCHAR(120),
                          unit_price_snapshot NUMERIC(12,2),
                          quantity_order INTEGER,
                          total_price_order NUMERIC(12,2),
                          version BIGINT DEFAULT 0,
                          status_order VARCHAR(20) NOT NULL,
                          order_date VARCHAR(30),
                          delivery_date VARCHAR(30),
                          payment_method VARCHAR(50),
                          payment_status_order VARCHAR(20),
                          canceled_reason_order VARCHAR(255),
                          coupon_code_order VARCHAR(50),
                          discount_amount_order NUMERIC(12,2),
                          final_amount_order NUMERIC(12,2),
                          CONSTRAINT fk_tb_order_user
                              FOREIGN KEY (id_user) REFERENCES tb_user (id_user),
                          CONSTRAINT fk_tb_order_device
                              FOREIGN KEY (id_device) REFERENCES tb_device (id_device)
);

CREATE INDEX idx_tb_order_id_user
    ON tb_order (id_user);

CREATE INDEX idx_tb_order_id_device
    ON tb_order (id_device);

CREATE TABLE tb_coupon (
                           id_coupon BIGSERIAL PRIMARY KEY,
                           code_coupon VARCHAR(50) NOT NULL,
                           type_coupon VARCHAR(20) NOT NULL,
                           value_coupon NUMERIC(12,2) NOT NULL,
                           active_coupon BOOLEAN NOT NULL,
                           starts_at_coupon TIMESTAMP WITH TIME ZONE,
                           ends_at_coupon TIMESTAMP WITH TIME ZONE,
                           min_order_amount_coupon NUMERIC(12,2),
                           max_uses_coupon INTEGER,
                           used_count_coupon INTEGER
);

CREATE UNIQUE INDEX uk_tb_coupon_code_lower
    ON tb_coupon (LOWER(code_coupon));

CREATE TABLE tb_outbox_event (
                                 id_outbox_event BIGSERIAL PRIMARY KEY,
                                 event_id VARCHAR(64) NOT NULL,
                                 event_type VARCHAR(64) NOT NULL,
                                 aggregate_type VARCHAR(64) NOT NULL,
                                 aggregate_id BIGINT NOT NULL,
                                 payload TEXT NOT NULL,
                                 status_outbox VARCHAR(20) NOT NULL,
                                 attempts_outbox INTEGER NOT NULL DEFAULT 0,
                                 next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 created_at_outbox TIMESTAMP WITH TIME ZONE NOT NULL,
                                 sent_at_outbox TIMESTAMP WITH TIME ZONE,
                                 last_error_outbox VARCHAR(512)
);

ALTER TABLE tb_outbox_event
    ADD CONSTRAINT uk_tb_outbox_event_event_id UNIQUE (event_id);

CREATE INDEX idx_tb_outbox_event_status_next_created
    ON tb_outbox_event (status_outbox, next_attempt_at, created_at_outbox);

CREATE INDEX idx_tb_outbox_event_status_created
    ON tb_outbox_event (status_outbox, created_at_outbox);

CREATE TABLE tb_processed_event (
                                    id_processed_event BIGSERIAL PRIMARY KEY,
                                    event_id VARCHAR(64) NOT NULL,
                                    event_type VARCHAR(64) NOT NULL,
                                    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE tb_processed_event
    ADD CONSTRAINT uk_tb_processed_event_event_id UNIQUE (event_id);

CREATE TABLE tb_order_idempondency (
                                       id_order_idempondency BIGSERIAL PRIMARY KEY,
                                       idempotency_key VARCHAR(120) NOT NULL,
                                       request_hash VARCHAR(128) NOT NULL,
                                       id_order BIGINT NOT NULL,
                                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                       CONSTRAINT fk_tb_order_idempondency_order
                                           FOREIGN KEY (id_order) REFERENCES tb_order (id_order)
);

CREATE UNIQUE INDEX uk_order_idempondency_key
    ON tb_order_idempondency (idempotency_key);

CREATE INDEX idx_tb_order_idempondency_order
    ON tb_order_idempondency (id_order);
