ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS loyalty_discount_amount numeric(38,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS loyalty_points_redeemed bigint NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS loyalty_wallets (
    id bigserial PRIMARY KEY,
    customer_id bigint NOT NULL UNIQUE REFERENCES users(id),
    total_points_earned bigint NOT NULL DEFAULT 0,
    available_points bigint NOT NULL DEFAULT 0,
    redeemed_points bigint NOT NULL DEFAULT 0,
    expired_points bigint NOT NULL DEFAULT 0,
    lifetime_points bigint NOT NULL DEFAULT 0,
    pending_points bigint NOT NULL DEFAULT 0,
    tier varchar(20) NOT NULL DEFAULT 'BRONZE',
    frozen boolean NOT NULL DEFAULT false,
    suspicious boolean NOT NULL DEFAULT false,
    fraud_reason varchar(500),
    last_earned_at timestamp,
    last_redeemed_at timestamp,
    tier_updated_at timestamp,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id bigserial PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES users(id),
    order_id bigint REFERENCES orders(id),
    transaction_type varchar(40) NOT NULL,
    points bigint NOT NULL,
    monetary_value numeric(38,2),
    description varchar(700) NOT NULL,
    status varchar(30) NOT NULL,
    reference_key varchar(180) NOT NULL UNIQUE,
    metadata varchar(1000),
    created_at timestamp NOT NULL DEFAULT now(),
    available_at timestamp,
    reversed_at timestamp
);

CREATE TABLE IF NOT EXISTS reward_rules (
    id bigserial PRIMARY KEY,
    name varchar(160) NOT NULL,
    rule_type varchar(40) NOT NULL,
    reward_rate numeric(10,4) NOT NULL,
    multiplier numeric(10,4),
    category varchar(1000),
    seller_id bigint,
    active boolean NOT NULL DEFAULT true,
    priority integer NOT NULL DEFAULT 100,
    starts_at timestamp,
    ends_at timestamp,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS tier_configs (
    id bigserial PRIMARY KEY,
    tier varchar(20) NOT NULL UNIQUE,
    min_lifetime_points bigint NOT NULL,
    reward_multiplier numeric(10,4) NOT NULL,
    benefits varchar(1000) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS redemption_history (
    id bigserial PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES users(id),
    order_id bigint NOT NULL REFERENCES orders(id),
    points_redeemed bigint NOT NULL,
    discount_amount numeric(38,2) NOT NULL,
    restored boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    restored_at timestamp
);

CREATE TABLE IF NOT EXISTS loyalty_expiry_schedule (
    id bigserial PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES users(id),
    transaction_id bigint NOT NULL UNIQUE REFERENCES loyalty_transactions(id),
    points_remaining bigint NOT NULL,
    expires_at timestamp NOT NULL,
    notified boolean NOT NULL DEFAULT false,
    expired boolean NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_loyalty_wallet_user ON loyalty_wallets(customer_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_wallet_tier ON loyalty_wallets(tier);
CREATE INDEX IF NOT EXISTS idx_loyalty_wallet_frozen ON loyalty_wallets(frozen, suspicious);
CREATE INDEX IF NOT EXISTS idx_loyalty_tx_customer_created ON loyalty_transactions(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_loyalty_tx_order_type ON loyalty_transactions(order_id, transaction_type);
CREATE INDEX IF NOT EXISTS idx_loyalty_tx_status ON loyalty_transactions(status);
CREATE INDEX IF NOT EXISTS idx_reward_rules_type_active ON reward_rules(rule_type, active);
CREATE INDEX IF NOT EXISTS idx_reward_rules_category ON reward_rules(category);
CREATE INDEX IF NOT EXISTS idx_reward_rules_seller ON reward_rules(seller_id);
CREATE INDEX IF NOT EXISTS idx_redemption_customer_created ON redemption_history(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_redemption_order ON redemption_history(order_id);
CREATE INDEX IF NOT EXISTS idx_expiry_customer ON loyalty_expiry_schedule(customer_id);
CREATE INDEX IF NOT EXISTS idx_expiry_due ON loyalty_expiry_schedule(expires_at, expired, notified);

INSERT INTO tier_configs(tier, min_lifetime_points, reward_multiplier, benefits)
VALUES
    ('BRONZE', 0, 1.00, 'Base rewards and member offers.'),
    ('SILVER', 500, 1.10, '10% bonus rewards and early promotions.'),
    ('GOLD', 2000, 1.25, '25% bonus rewards, priority support, and premium campaigns.'),
    ('PLATINUM', 5000, 1.50, '50% bonus rewards, VIP support, and exclusive launches.')
ON CONFLICT (tier) DO NOTHING;

INSERT INTO reward_rules(name, rule_type, reward_rate, category, priority)
VALUES
    ('Base reward', 'BASE', 0.0100, NULL, 100),
    ('Electronics reward', 'CATEGORY', 0.0100, 'Electronics', 20),
    ('Fashion reward', 'CATEGORY', 0.0500, 'Fashion', 20)
ON CONFLICT DO NOTHING;
