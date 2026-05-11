-- Миграция V9: Подписочная модель (subscription + period + trial_usage + participant).
--
-- Тарифы:
--   PERSONAL (1:1), GROUP_S (до 10 чел.), GROUP_M (до 25), GROUP_L (до 50).
-- Статусы:
--   PENDING_CONSENT, TRIAL, ACTIVE, EXPIRED, BLOCKED_CONSENT, CANCELLED.
-- Источники периодов:
--   TRIAL, MANUAL_GRANT, PAYMENT, REFERRAL_BONUS.

CREATE TABLE IF NOT EXISTS subscription (
    id                                  BIGSERIAL PRIMARY KEY,
    owner_user_id                       BIGINT       NOT NULL REFERENCES users(id),
    bot_id                              BIGINT       NOT NULL REFERENCES bot(id),
    plan                                VARCHAR(32)  NOT NULL,
    scope_chat_id                       BIGINT,
    max_participants                    INTEGER,
    status                              VARCHAR(32)  NOT NULL DEFAULT 'PENDING_CONSENT',
    started_at                          TIMESTAMP WITH TIME ZONE,
    expires_at                          TIMESTAMP WITH TIME ZONE,
    requires_consent_reacceptance_until TIMESTAMP WITH TIME ZONE,
    notes                               TEXT,
    created_at                          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

COMMENT ON TABLE  subscription                                IS 'Подписки (персональные и групповые)';
COMMENT ON COLUMN subscription.owner_user_id                  IS 'Владелец подписки (для personal — сам пользователь, для group — ведущий)';
COMMENT ON COLUMN subscription.scope_chat_id                  IS 'Telegram chat_id группы (NULL для PERSONAL)';
COMMENT ON COLUMN subscription.max_participants               IS 'Лимит участников (NULL для PERSONAL)';
COMMENT ON COLUMN subscription.expires_at                     IS 'Кэш max(subscription_period.period_ends_at)';
COMMENT ON COLUMN subscription.requires_consent_reacceptance_until IS 'До этой даты — грейс на принятие новой версии документа (MATERIAL)';

-- Один personal-план на пользователя в рамках бота; одна group-подписка на чат в рамках бота.
CREATE UNIQUE INDEX IF NOT EXISTS idx_subscription_personal_unique
    ON subscription (owner_user_id, bot_id)
    WHERE scope_chat_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_subscription_group_unique
    ON subscription (bot_id, scope_chat_id)
    WHERE scope_chat_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_subscription_owner ON subscription(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_subscription_bot   ON subscription(bot_id);
CREATE INDEX IF NOT EXISTS idx_subscription_chat  ON subscription(scope_chat_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status_expires ON subscription(status, expires_at);

-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_period (
    id                  BIGSERIAL PRIMARY KEY,
    subscription_id     BIGINT       NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    source              VARCHAR(32)  NOT NULL,
    period_started_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    period_ends_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    granted_by_user_id  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    note                TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

COMMENT ON TABLE  subscription_period         IS 'История выдач/продлений подписки';
COMMENT ON COLUMN subscription_period.source  IS 'TRIAL | MANUAL_GRANT | PAYMENT | REFERRAL_BONUS';

CREATE INDEX IF NOT EXISTS idx_subscription_period_sub ON subscription_period(subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscription_period_ends ON subscription_period(period_ends_at);

-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trial_usage (
    id               BIGSERIAL PRIMARY KEY,
    plan             VARCHAR(32) NOT NULL,
    owner_user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scope_chat_id    BIGINT      NOT NULL DEFAULT 0,
    used_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

COMMENT ON TABLE trial_usage IS 'Учёт использованных триалов (один раз на (plan, owner, scope_chat))';
CREATE UNIQUE INDEX IF NOT EXISTS idx_trial_usage_unique
    ON trial_usage (plan, owner_user_id, scope_chat_id);

-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_participant (
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    telegram_id     BIGINT NOT NULL,
    first_seen_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_seen_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_subscription_participant_unique
    ON subscription_participant (subscription_id, telegram_id);

CREATE INDEX IF NOT EXISTS idx_subscription_participant_last_seen
    ON subscription_participant (subscription_id, last_seen_at);
