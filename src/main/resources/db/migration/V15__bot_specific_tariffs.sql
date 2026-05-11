-- Настройка тарифов для целевых ботов:
-- 1) einst_gpt_bot: только персональный FREE.
-- 2) ImaginalSpaceBot: personal trial + group 10/25/50 (без PERSONAL_FREE).

-- Создать отсутствующие базовые тарифы для einst_gpt_bot.
INSERT INTO subscription_tariff (
    bot_id, code, title, scope, access_mode, trial_days, max_participants,
    sort_order, active, default_personal, default_group,
    referral_enabled, referral_referrer_days, referral_referred_days, referral_first_payment_only
)
SELECT b.id, 'PERSONAL_FREE', 'Персональный (бесплатно)', 'PERSONAL', 'FREE_UNLIMITED', NULL, NULL,
       0, TRUE, TRUE, FALSE,
       FALSE, NULL, NULL, TRUE
FROM bot b
WHERE b.username = 'einst_gpt_bot'
  AND NOT EXISTS (
      SELECT 1 FROM subscription_tariff t
      WHERE t.bot_id = b.id AND t.code = 'PERSONAL_FREE'
  );

-- Для einst_gpt_bot оставить только PERSONAL_FREE активным default-тарифом.
UPDATE subscription_tariff t
SET active = (t.code = 'PERSONAL_FREE'),
    default_personal = (t.code = 'PERSONAL_FREE'),
    default_group = FALSE,
    sort_order = CASE WHEN t.code = 'PERSONAL_FREE' THEN 0 ELSE t.sort_order END
FROM bot b
WHERE t.bot_id = b.id
  AND b.username = 'einst_gpt_bot';

-- Создать отсутствующие базовые тарифы для ImaginalSpaceBot.
INSERT INTO subscription_tariff (
    bot_id, code, title, scope, access_mode, trial_days, max_participants,
    sort_order, active, default_personal, default_group,
    referral_enabled, referral_referrer_days, referral_referred_days, referral_first_payment_only
)
SELECT b.id, v.code, v.title, v.scope, v.access_mode, v.trial_days, v.max_participants,
       v.sort_order, v.active, v.default_personal, v.default_group,
       v.referral_enabled, v.referral_referrer_days, v.referral_referred_days, TRUE
FROM bot b
CROSS JOIN (
    VALUES
        ('PERSONAL_TRIAL', 'Персональный (пробный период)', 'PERSONAL', 'TRIAL_ONBOARDING', 3::integer, NULL::integer, 0, TRUE, TRUE, FALSE, TRUE, 7::integer, 7::integer),
        ('GROUP_S',        'Группа (до 10)',               'GROUP',    'PAID_TERM',         NULL::integer, 10, 10, TRUE, FALSE, TRUE, TRUE, 7::integer, 7::integer),
        ('GROUP_M',        'Группа (до 25)',               'GROUP',    'PAID_TERM',         NULL::integer, 25, 20, TRUE, FALSE, FALSE, TRUE, 7::integer, 7::integer),
        ('GROUP_L',        'Группа (до 50)',               'GROUP',    'PAID_TERM',         NULL::integer, 50, 30, TRUE, FALSE, FALSE, TRUE, 7::integer, 7::integer)
) AS v(code, title, scope, access_mode, trial_days, max_participants, sort_order, active, default_personal, default_group, referral_enabled, referral_referrer_days, referral_referred_days)
WHERE b.username = 'ImaginalSpaceBot'
  AND NOT EXISTS (
      SELECT 1 FROM subscription_tariff t
      WHERE t.bot_id = b.id AND t.code = v.code
  );

-- Для ImaginalSpaceBot активны только trial+group тарифы; free отключен.
UPDATE subscription_tariff t
SET active = CASE WHEN t.code IN ('PERSONAL_TRIAL', 'GROUP_S', 'GROUP_M', 'GROUP_L') THEN TRUE ELSE FALSE END,
    default_personal = (t.code = 'PERSONAL_TRIAL'),
    default_group = (t.code = 'GROUP_S'),
    referral_enabled = CASE WHEN t.code IN ('PERSONAL_TRIAL', 'GROUP_S', 'GROUP_M', 'GROUP_L') THEN TRUE ELSE FALSE END,
    referral_referrer_days = CASE WHEN t.code IN ('PERSONAL_TRIAL', 'GROUP_S', 'GROUP_M', 'GROUP_L') THEN COALESCE(t.referral_referrer_days, 7) ELSE NULL END,
    referral_referred_days = CASE WHEN t.code IN ('PERSONAL_TRIAL', 'GROUP_S', 'GROUP_M', 'GROUP_L') THEN COALESCE(t.referral_referred_days, 7) ELSE NULL END,
    referral_first_payment_only = TRUE
FROM bot b
WHERE t.bot_id = b.id
  AND b.username = 'ImaginalSpaceBot';
