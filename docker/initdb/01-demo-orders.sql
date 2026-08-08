CREATE TABLE IF NOT EXISTS demo_orders (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM demo_orders
        LIMIT 1
    ) THEN
        INSERT INTO demo_orders (
            customer_id,
            product_name,
            amount
        )
        SELECT
            FLOOR(RANDOM() * 1000)::INT,
            'Product ' || FLOOR(RANDOM() * 100)::INT,
            ROUND((RANDOM() * 500)::NUMERIC, 2)
        FROM generate_series(1, 50000);
    END IF;
END
$$;