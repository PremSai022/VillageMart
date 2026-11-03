-- VillageMart V1 schema
-- Money: DECIMAL(14,2), quantities: DECIMAL(14,3), unit costs: DECIMAL(16,4). Timestamps stored in UTC.

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(60)  NOT NULL,
    mobile          VARCHAR(15),
    full_name       VARCHAR(120) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_mobile UNIQUE (mobile)
);

CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    token_hash      VARCHAR(64)  NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at      DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE business_settings (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_name           VARCHAR(150) NOT NULL,
    address                 VARCHAR(300),
    mobile                  VARCHAR(15),
    email                   VARCHAR(120),
    gst_enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    gstin                   VARCHAR(15),
    state_name              VARCHAR(60),
    currency_code           VARCHAR(3)   NOT NULL DEFAULT 'INR',
    timezone                VARCHAR(40)  NOT NULL DEFAULT 'Asia/Kolkata',
    invoice_footer          VARCHAR(300),
    low_stock_push_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL
);

CREATE TABLE document_sequences (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sequence_name   VARCHAR(40)  NOT NULL,
    prefix          VARCHAR(10)  NOT NULL,
    next_value      BIGINT       NOT NULL,
    CONSTRAINT uk_document_sequences_name UNIQUE (sequence_name)
);

CREATE TABLE idempotency_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idem_key        VARCHAR(80)  NOT NULL,
    operation       VARCHAR(40)  NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    resource_id     BIGINT,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_idempotency_key UNIQUE (operation, idem_key)
);

CREATE TABLE categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(80)  NOT NULL,
    description     VARCHAR(255),
    sort_order      INT          NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE units (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(40)  NOT NULL,
    allows_decimal  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_units_code UNIQUE (code)
);

CREATE TABLE products (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(150)  NOT NULL,
    sku                 VARCHAR(50)   NOT NULL,
    barcode             VARCHAR(50),
    category_id         BIGINT        NOT NULL,
    unit_id             BIGINT        NOT NULL,
    description         VARCHAR(500),
    image_url           VARCHAR(500),
    purchase_price      DECIMAL(14,2) NOT NULL,
    selling_price       DECIMAL(14,2) NOT NULL,
    min_selling_price   DECIMAL(14,2),
    min_stock_quantity  DECIMAL(14,3) NOT NULL DEFAULT 0,
    tax_rate            DECIMAL(5,2)  NOT NULL DEFAULT 0,
    hsn_code            VARCHAR(10),
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT uk_products_barcode UNIQUE (barcode),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_unit FOREIGN KEY (unit_id) REFERENCES units (id)
);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_active ON products (active);

CREATE TABLE suppliers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    mobile          VARCHAR(15),
    address         VARCHAR(300),
    gst_number      VARCHAR(15),
    notes           VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL
);
CREATE INDEX idx_suppliers_name ON suppliers (name);

CREATE TABLE stock_locations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL,
    name            VARCHAR(80)  NOT NULL,
    location_type   VARCHAR(20)  NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_stock_locations_code UNIQUE (code)
);

CREATE TABLE inventory (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id          BIGINT        NOT NULL,
    location_id         BIGINT        NOT NULL,
    quantity            DECIMAL(14,3) NOT NULL DEFAULT 0,
    average_cost        DECIMAL(16,4) NOT NULL DEFAULT 0,
    stock_status        VARCHAR(20)   NOT NULL,
    last_movement_at    DATETIME(6),
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT uk_inventory_product_location UNIQUE (product_id, location_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT ck_inventory_non_negative CHECK (quantity >= 0)
);
CREATE INDEX idx_inventory_status ON inventory (stock_status);

CREATE TABLE stock_movements (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id          BIGINT        NOT NULL,
    location_id         BIGINT        NOT NULL,
    movement_type       VARCHAR(30)   NOT NULL,
    quantity_change     DECIMAL(14,3) NOT NULL,
    previous_quantity   DECIMAL(14,3) NOT NULL,
    new_quantity        DECIMAL(14,3) NOT NULL,
    unit_cost           DECIMAL(16,4),
    average_cost_after  DECIMAL(16,4) NOT NULL,
    reference_type      VARCHAR(30),
    reference_id        BIGINT,
    reference_number    VARCHAR(40),
    reason              VARCHAR(300),
    created_by          BIGINT,
    created_at          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movements_location FOREIGN KEY (location_id) REFERENCES stock_locations (id)
);
CREATE INDEX idx_stock_movements_product ON stock_movements (product_id, created_at);
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference_type, reference_id);

CREATE TABLE purchases (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_number         VARCHAR(30)   NOT NULL,
    supplier_id             BIGINT        NOT NULL,
    supplier_invoice_number VARCHAR(50),
    purchase_date           DATE          NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    subtotal                DECIMAL(14,2) NOT NULL,
    discount_total          DECIMAL(14,2) NOT NULL DEFAULT 0,
    transport_charges       DECIMAL(14,2) NOT NULL DEFAULT 0,
    other_charges           DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_amount            DECIMAL(14,2) NOT NULL,
    returned_amount         DECIMAL(14,2) NOT NULL DEFAULT 0,
    notes                   VARCHAR(500),
    completed_at            DATETIME(6),
    cancelled_at            DATETIME(6),
    cancel_reason           VARCHAR(300),
    version                 BIGINT        NOT NULL DEFAULT 0,
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    CONSTRAINT uk_purchases_number UNIQUE (purchase_number),
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);
CREATE INDEX idx_purchases_date ON purchases (purchase_date);
CREATE INDEX idx_purchases_supplier_invoice ON purchases (supplier_id, supplier_invoice_number);
CREATE INDEX idx_purchases_status ON purchases (status);

CREATE TABLE purchase_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id         BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    product_name        VARCHAR(150)  NOT NULL,
    unit_code           VARCHAR(20)   NOT NULL,
    quantity            DECIMAL(14,3) NOT NULL,
    unit_price          DECIMAL(14,2) NOT NULL,
    discount_amount     DECIMAL(14,2) NOT NULL DEFAULT 0,
    line_total          DECIMAL(14,2) NOT NULL,
    allocated_charges   DECIMAL(14,2) NOT NULL DEFAULT 0,
    landed_unit_cost    DECIMAL(16,4) NOT NULL,
    returned_quantity   DECIMAL(14,3) NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT fk_purchase_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);
CREATE INDEX idx_purchase_items_product ON purchase_items (product_id);

CREATE TABLE villages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    district        VARCHAR(100),
    state           VARCHAR(60)  NOT NULL,
    notes           VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_villages_name_district UNIQUE (name, district)
);

CREATE TABLE customers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    mobile          VARCHAR(15),
    village_id      BIGINT,
    address         VARCHAR(300),
    notes           VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_customers_mobile UNIQUE (mobile),
    CONSTRAINT fk_customers_village FOREIGN KEY (village_id) REFERENCES villages (id)
);
CREATE INDEX idx_customers_name ON customers (name);

CREATE TABLE sales (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number      VARCHAR(30)   NOT NULL,
    sale_date           DATE          NOT NULL,
    sale_type           VARCHAR(20)   NOT NULL,
    customer_id         BIGINT,
    village_id          BIGINT,
    status              VARCHAR(25)   NOT NULL,
    payment_mode        VARCHAR(20)   NOT NULL,
    subtotal            DECIMAL(14,2) NOT NULL,
    item_discount_total DECIMAL(14,2) NOT NULL DEFAULT 0,
    bill_discount       DECIMAL(14,2) NOT NULL DEFAULT 0,
    taxable_amount      DECIMAL(14,2) NOT NULL,
    tax_amount          DECIMAL(14,2) NOT NULL DEFAULT 0,
    round_off           DECIMAL(6,2)  NOT NULL DEFAULT 0,
    total_amount        DECIMAL(14,2) NOT NULL,
    cost_total          DECIMAL(14,2) NOT NULL,
    paid_amount         DECIMAL(14,2) NOT NULL DEFAULT 0,
    outstanding_amount  DECIMAL(14,2) NOT NULL DEFAULT 0,
    returned_amount     DECIMAL(14,2) NOT NULL DEFAULT 0,
    returned_revenue    DECIMAL(14,2) NOT NULL DEFAULT 0,
    returned_cost       DECIMAL(14,2) NOT NULL DEFAULT 0,
    notes               VARCHAR(500),
    idempotency_key     VARCHAR(80),
    cancelled_at        DATETIME(6),
    cancel_reason       VARCHAR(300),
    created_by          BIGINT,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT uk_sales_invoice UNIQUE (invoice_number),
    CONSTRAINT uk_sales_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sales_village FOREIGN KEY (village_id) REFERENCES villages (id)
);
CREATE INDEX idx_sales_date ON sales (sale_date, status);
CREATE INDEX idx_sales_customer ON sales (customer_id);
CREATE INDEX idx_sales_village ON sales (village_id, sale_date);

CREATE TABLE sale_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id             BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    product_name        VARCHAR(150)  NOT NULL,
    sku                 VARCHAR(50)   NOT NULL,
    category_name       VARCHAR(80),
    unit_code           VARCHAR(20)   NOT NULL,
    quantity            DECIMAL(14,3) NOT NULL,
    list_price          DECIMAL(14,2) NOT NULL,
    unit_price          DECIMAL(14,2) NOT NULL,
    price_overridden    BOOLEAN       NOT NULL DEFAULT FALSE,
    discount_amount     DECIMAL(14,2) NOT NULL DEFAULT 0,
    bill_discount_share DECIMAL(14,2) NOT NULL DEFAULT 0,
    taxable_amount      DECIMAL(14,2) NOT NULL,
    tax_rate            DECIMAL(5,2)  NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(14,2) NOT NULL DEFAULT 0,
    line_total          DECIMAL(14,2) NOT NULL,
    unit_cost           DECIMAL(16,4) NOT NULL,
    cost_total          DECIMAL(14,2) NOT NULL,
    returned_quantity   DECIMAL(14,3) NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_sale_items_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sale_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);
CREATE INDEX idx_sale_items_product ON sale_items (product_id);

CREATE TABLE sales_returns (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_number       VARCHAR(30)   NOT NULL,
    sale_id             BIGINT        NOT NULL,
    return_date         DATE          NOT NULL,
    reason              VARCHAR(300)  NOT NULL,
    refund_amount       DECIMAL(14,2) NOT NULL,
    revenue_reversed    DECIMAL(14,2) NOT NULL,
    cost_reversed       DECIMAL(14,2) NOT NULL,
    credit_adjusted     DECIMAL(14,2) NOT NULL DEFAULT 0,
    cash_refunded       DECIMAL(14,2) NOT NULL DEFAULT 0,
    refund_method       VARCHAR(20),
    created_by          BIGINT,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT uk_sales_returns_number UNIQUE (return_number),
    CONSTRAINT fk_sales_returns_sale FOREIGN KEY (sale_id) REFERENCES sales (id)
);
CREATE INDEX idx_sales_returns_date ON sales_returns (return_date);

CREATE TABLE sales_return_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sales_return_id     BIGINT        NOT NULL,
    sale_item_id        BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            DECIMAL(14,3) NOT NULL,
    saleable            BOOLEAN       NOT NULL,
    refund_amount       DECIMAL(14,2) NOT NULL,
    revenue_reversed    DECIMAL(14,2) NOT NULL,
    unit_cost           DECIMAL(16,4) NOT NULL,
    cost_reversed       DECIMAL(14,2) NOT NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_sri_return FOREIGN KEY (sales_return_id) REFERENCES sales_returns (id),
    CONSTRAINT fk_sri_sale_item FOREIGN KEY (sale_item_id) REFERENCES sale_items (id),
    CONSTRAINT fk_sri_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE purchase_returns (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_number       VARCHAR(30)   NOT NULL,
    purchase_id         BIGINT        NOT NULL,
    return_date         DATE          NOT NULL,
    reason              VARCHAR(300)  NOT NULL,
    total_amount        DECIMAL(14,2) NOT NULL,
    supplier_reference  VARCHAR(60),
    created_by          BIGINT,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT uk_purchase_returns_number UNIQUE (return_number),
    CONSTRAINT fk_purchase_returns_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id)
);

CREATE TABLE purchase_return_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_return_id  BIGINT        NOT NULL,
    purchase_item_id    BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            DECIMAL(14,3) NOT NULL,
    unit_cost           DECIMAL(16,4) NOT NULL,
    amount              DECIMAL(14,2) NOT NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_pri_return FOREIGN KEY (purchase_return_id) REFERENCES purchase_returns (id),
    CONSTRAINT fk_pri_purchase_item FOREIGN KEY (purchase_item_id) REFERENCES purchase_items (id),
    CONSTRAINT fk_pri_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE payments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_number      VARCHAR(30)   NOT NULL,
    sale_id             BIGINT,
    customer_id         BIGINT,
    sales_return_id     BIGINT,
    payment_type        VARCHAR(30)   NOT NULL,
    method              VARCHAR(20)   NOT NULL,
    direction           VARCHAR(10)   NOT NULL,
    amount              DECIMAL(14,2) NOT NULL,
    payment_date        DATE          NOT NULL,
    reference           VARCHAR(80),
    notes               VARCHAR(300),
    reversed            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by          BIGINT,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT uk_payments_number UNIQUE (payment_number),
    CONSTRAINT fk_payments_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_payments_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_payments_return FOREIGN KEY (sales_return_id) REFERENCES sales_returns (id),
    CONSTRAINT ck_payments_positive CHECK (amount > 0)
);
CREATE INDEX idx_payments_date ON payments (payment_date, method);
CREATE INDEX idx_payments_sale ON payments (sale_id);
CREATE INDEX idx_payments_customer ON payments (customer_id);

CREATE TABLE expense_categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(60)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_expense_categories_name UNIQUE (name)
);

CREATE TABLE expenses (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id         BIGINT        NOT NULL,
    amount              DECIMAL(14,2) NOT NULL,
    expense_date        DATE          NOT NULL,
    description         VARCHAR(300),
    receipt_image_url   VARCHAR(500),
    village_id          BIGINT,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES expense_categories (id),
    CONSTRAINT fk_expenses_village FOREIGN KEY (village_id) REFERENCES villages (id),
    CONSTRAINT ck_expenses_positive CHECK (amount > 0)
);
CREATE INDEX idx_expenses_date ON expenses (expense_date, deleted);

CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(30)  NOT NULL,
    severity        VARCHAR(10)  NOT NULL,
    title           VARCHAR(150) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    product_id      BIGINT,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at         DATETIME(6),
    alert_active    BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_at     DATETIME(6),
    push_status     VARCHAR(15)  NOT NULL,
    push_attempts   INT          NOT NULL DEFAULT 0,
    last_push_error VARCHAR(300),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_notifications_product FOREIGN KEY (product_id) REFERENCES products (id)
);
CREATE INDEX idx_notifications_read ON notifications (is_read, created_at);
CREATE INDEX idx_notifications_alert ON notifications (product_id, alert_active);
CREATE INDEX idx_notifications_push ON notifications (push_status);

CREATE TABLE device_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    token           VARCHAR(512) NOT NULL,
    platform        VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
