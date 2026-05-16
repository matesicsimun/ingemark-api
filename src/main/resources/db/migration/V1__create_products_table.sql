CREATE TABLE products (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(10)   NOT NULL,
    name         VARCHAR(255)  NOT NULL,
    price_eur    NUMERIC(19,2) NOT NULL,
    price_usd    NUMERIC(19,2) NOT NULL,
    is_available BOOLEAN       NOT NULL,
    CONSTRAINT uq_products_code UNIQUE (code),
    CONSTRAINT ck_products_code_length CHECK (char_length(code) = 10),
    CONSTRAINT ck_products_price_eur_nonneg CHECK (price_eur >= 0),
    CONSTRAINT ck_products_price_usd_nonneg CHECK (price_usd >= 0)
);
