-- Тестовые данные для автотестов

DELETE FROM trial_usage;
DELETE FROM subscription_participant;
DELETE FROM subscription_period;
DELETE FROM subscription;
DELETE FROM subscription_tariff;

DELETE FROM users;
DELETE FROM bot_keyword;
DELETE FROM bot;
DELETE FROM agent_config;

INSERT INTO bot (name, username, token, type, is_active, require_age_confirmation, created_at, updated_at)
VALUES (
    'IntegrationTestBot',
    'integration_test_bot',
    '123456789:AAIntegrationTestTokenForHmacSigningOnly',
    'assistant',
    true,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO agent_config (
    name, display_name, short_description, role, provider, model, system_prompt, mem_window,
    data_locality, is_public, created_at, updated_at
)
VALUES (
    'Диалог · integration_test_bot',
    'Диалог · integration_test_bot',
    NULL,
    'CONVERSATION',
    'OPENAI',
    'gpt-4o',
    'Test system prompt',
    100,
    'CROSS_BORDER',
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

UPDATE bot
SET agent_config_id = (SELECT id FROM agent_config WHERE name = 'Диалог · integration_test_bot' LIMIT 1)
WHERE username = 'integration_test_bot';

INSERT INTO subscription_tariff (bot_id, code, title, scope, access_mode, trial_days, max_participants, sort_order, active, default_personal, default_group, created_at, updated_at)
SELECT b.id, 'PERSONAL_TRIAL', 'Персональный (пробный период)', 'PERSONAL', 'TRIAL_ONBOARDING', 3, NULL, 0, TRUE, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bot b WHERE b.username = 'integration_test_bot';
INSERT INTO subscription_tariff (bot_id, code, title, scope, access_mode, trial_days, max_participants, sort_order, active, default_personal, default_group, created_at, updated_at)
SELECT b.id, 'PERSONAL_FREE', 'Персональный (бесплатно)', 'PERSONAL', 'FREE_UNLIMITED', NULL, NULL, 1, TRUE, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bot b WHERE b.username = 'integration_test_bot';
INSERT INTO subscription_tariff (bot_id, code, title, scope, access_mode, trial_days, max_participants, sort_order, active, default_personal, default_group, created_at, updated_at)
SELECT b.id, 'GROUP_S', 'Группа (до 10)', 'GROUP', 'PAID_TERM', NULL, 10, 10, TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bot b WHERE b.username = 'integration_test_bot';
INSERT INTO subscription_tariff (bot_id, code, title, scope, access_mode, trial_days, max_participants, sort_order, active, default_personal, default_group, created_at, updated_at)
SELECT b.id, 'GROUP_M', 'Группа (до 25)', 'GROUP', 'PAID_TERM', NULL, 25, 20, TRUE, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bot b WHERE b.username = 'integration_test_bot';
INSERT INTO subscription_tariff (bot_id, code, title, scope, access_mode, trial_days, max_participants, sort_order, active, default_personal, default_group, created_at, updated_at)
SELECT b.id, 'GROUP_L', 'Группа (до 50)', 'GROUP', 'PAID_TERM', NULL, 50, 30, TRUE, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bot b WHERE b.username = 'integration_test_bot';

INSERT INTO users (telegram_id, username, first_name, last_name, role, created_at, updated_at)
VALUES (141614461, 'E13nst', 'Andrey', 'Mitroshin', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
