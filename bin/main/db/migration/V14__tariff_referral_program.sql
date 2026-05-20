ALTER TABLE subscription_tariff
    ADD COLUMN IF NOT EXISTS referral_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE subscription_tariff
    ADD COLUMN IF NOT EXISTS referral_referrer_days INTEGER;

ALTER TABLE subscription_tariff
    ADD COLUMN IF NOT EXISTS referral_referred_days INTEGER;

ALTER TABLE subscription_tariff
    ADD COLUMN IF NOT EXISTS referral_first_payment_only BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE subscription_tariff
    ADD CONSTRAINT chk_subscription_tariff_referral_days_non_negative
    CHECK (
        referral_referrer_days IS NULL OR referral_referrer_days >= 0
    );

ALTER TABLE subscription_tariff
    ADD CONSTRAINT chk_subscription_tariff_referred_days_non_negative
    CHECK (
        referral_referred_days IS NULL OR referral_referred_days >= 0
    );

ALTER TABLE subscription_tariff
    ADD CONSTRAINT chk_subscription_tariff_referral_enabled_days
    CHECK (
        NOT referral_enabled
        OR COALESCE(referral_referrer_days, 0) + COALESCE(referral_referred_days, 0) > 0
    );

CREATE TABLE IF NOT EXISTS referral_bonus_grant (
    id                        BIGSERIAL PRIMARY KEY,
    bot_id                    BIGINT NOT NULL REFERENCES bot(id) ON DELETE CASCADE,
    tariff_id                 BIGINT NOT NULL REFERENCES subscription_tariff(id) ON DELETE CASCADE,
    payment_period_id         BIGINT NOT NULL REFERENCES subscription_period(id) ON DELETE CASCADE,
    referred_user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referrer_user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referred_subscription_id  BIGINT NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    referrer_subscription_id  BIGINT NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    referred_bonus_days       INTEGER NOT NULL DEFAULT 0,
    referrer_bonus_days       INTEGER NOT NULL DEFAULT 0,
    granted_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_referral_bonus_payment UNIQUE (payment_period_id)
);

CREATE INDEX IF NOT EXISTS idx_referral_bonus_referrer
    ON referral_bonus_grant (referrer_user_id, bot_id);

CREATE INDEX IF NOT EXISTS idx_referral_bonus_referred
    ON referral_bonus_grant (referred_user_id, bot_id);
