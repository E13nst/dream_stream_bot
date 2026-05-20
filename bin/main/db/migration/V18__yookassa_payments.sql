-- ЮKassa: платежи, поля тарифа для checkout, креды и чек по боту, email для чека у пользователя.

ALTER TABLE bot
    ADD COLUMN IF NOT EXISTS yookassa_shop_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS yookassa_secret_key VARCHAR(512),
    ADD COLUMN IF NOT EXISTS yookassa_receipt_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE subscription_tariff
    ADD COLUMN IF NOT EXISTS price_amount_minor BIGINT,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(8) NOT NULL DEFAULT 'RUB',
    ADD COLUMN IF NOT EXISTS paid_term_days INTEGER,
    ADD COLUMN IF NOT EXISTS checkout_description TEXT;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS billing_email VARCHAR(255);

CREATE TABLE IF NOT EXISTS subscription_payment (
    id                  BIGSERIAL PRIMARY KEY,
    subscription_id     BIGINT NOT NULL REFERENCES subscription(id),
    tariff_id           BIGINT NOT NULL REFERENCES subscription_tariff(id),
    bot_id              BIGINT NOT NULL REFERENCES bot(id),
    owner_user_id       BIGINT NOT NULL REFERENCES users(id),
    provider            VARCHAR(32) NOT NULL DEFAULT 'yookassa',
    provider_payment_id VARCHAR(128),
    idempotency_key     VARCHAR(64) NOT NULL UNIQUE,
    amount_minor        BIGINT NOT NULL,
    currency            VARCHAR(8) NOT NULL DEFAULT 'RUB',
    status              VARCHAR(32) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_subscription_payment_subscription ON subscription_payment (subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscription_payment_bot ON subscription_payment (bot_id);
CREATE INDEX IF NOT EXISTS idx_subscription_payment_owner ON subscription_payment (owner_user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_payment_provider_id
    ON subscription_payment (provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;
