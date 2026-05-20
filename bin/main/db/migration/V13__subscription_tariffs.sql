-- Тарифы по боту: персональный/групповой, режим FREE / триал после онбординга / оплатной срок.

CREATE TABLE IF NOT EXISTS subscription_tariff (
    id                  BIGSERIAL PRIMARY KEY,
    bot_id              BIGINT        NOT NULL REFERENCES bot(id) ON DELETE CASCADE,
    code                VARCHAR(64)   NOT NULL,
    title               VARCHAR(255)  NOT NULL,
    scope               VARCHAR(16)   NOT NULL,
    access_mode         VARCHAR(32)   NOT NULL,
    trial_days          INTEGER,
    max_participants    INTEGER,
    sort_order          INTEGER       NOT NULL DEFAULT 0,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    default_personal    BOOLEAN       NOT NULL DEFAULT FALSE,
    default_group       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscription_tariff_bot_code UNIQUE (bot_id, code)
);

CREATE INDEX IF NOT EXISTS idx_subscription_tariff_bot ON subscription_tariff (bot_id);

INSERT INTO subscription_tariff (
    bot_id, code, title, scope, access_mode, trial_days, max_participants, sort_order, active,
    default_personal, default_group
)
SELECT
    b.id,
    v.code,
    v.title,
    v.scope,
    v.access_mode,
    v.trial_days,
    v.max_participants,
    v.sort_order,
    TRUE,
    v.default_personal,
    v.default_group
FROM bot b
CROSS JOIN (
    VALUES
        ('PERSONAL_TRIAL', 'Персональный (пробный период)', 'PERSONAL', 'TRIAL_ONBOARDING', 3::integer, NULL::integer, 0, TRUE, FALSE),
        ('PERSONAL_FREE', 'Персональный (бесплатно)', 'PERSONAL', 'FREE_UNLIMITED', NULL::integer, NULL::integer, 1, FALSE, FALSE),
        ('GROUP_S', 'Группа (до 10)', 'GROUP', 'PAID_TERM', NULL::integer, 10, 10, FALSE, TRUE),
        ('GROUP_M', 'Группа (до 25)', 'GROUP', 'PAID_TERM', NULL::integer, 25, 20, FALSE, FALSE),
        ('GROUP_L', 'Группа (до 50)', 'GROUP', 'PAID_TERM', NULL::integer, 50, 30, FALSE, FALSE)
) AS v(code, title, scope, access_mode, trial_days, max_participants, sort_order, default_personal, default_group);

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS tariff_id BIGINT REFERENCES subscription_tariff(id);

UPDATE subscription s
SET tariff_id = t.id
FROM subscription_tariff t
WHERE s.tariff_id IS NULL
  AND s.bot_id = t.bot_id
  AND (
    (s.plan = 'PERSONAL' AND t.code = 'PERSONAL_TRIAL')
    OR (s.plan = 'GROUP_S' AND t.code = 'GROUP_S')
    OR (s.plan = 'GROUP_M' AND t.code = 'GROUP_M')
    OR (s.plan = 'GROUP_L' AND t.code = 'GROUP_L')
);

ALTER TABLE subscription ALTER COLUMN tariff_id SET NOT NULL;

ALTER TABLE trial_usage ADD COLUMN IF NOT EXISTS bot_id BIGINT REFERENCES bot(id) ON DELETE CASCADE;
ALTER TABLE trial_usage ADD COLUMN IF NOT EXISTS tariff_id BIGINT REFERENCES subscription_tariff(id);

UPDATE trial_usage tu
SET bot_id = s.bot_id,
    tariff_id = s.tariff_id
FROM subscription s
WHERE tu.bot_id IS NULL
  AND s.owner_user_id = tu.owner_user_id
  AND COALESCE(s.scope_chat_id, 0) = tu.scope_chat_id
  AND s.plan = tu.plan;

DELETE FROM trial_usage WHERE tariff_id IS NULL OR bot_id IS NULL;

DROP INDEX IF EXISTS idx_trial_usage_unique;
ALTER TABLE trial_usage DROP COLUMN IF EXISTS plan;
ALTER TABLE trial_usage ALTER COLUMN bot_id SET NOT NULL;
ALTER TABLE trial_usage ALTER COLUMN tariff_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_trial_usage_unique
    ON trial_usage (tariff_id, owner_user_id, scope_chat_id);

ALTER TABLE subscription DROP COLUMN plan;
