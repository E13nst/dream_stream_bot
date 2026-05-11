-- Тестовые данные для автотестов

DELETE FROM users;
DELETE FROM bot_keyword;
DELETE FROM bot;
DELETE FROM agent_config;

INSERT INTO bot (name, username, token, type, is_active, created_at, updated_at)
VALUES (
    'IntegrationTestBot',
    'integration_test_bot',
    '123456789:AAIntegrationTestTokenForHmacSigningOnly',
    'assistant',
    true,
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

INSERT INTO users (telegram_id, username, first_name, last_name, role, created_at, updated_at)
VALUES (141614461, 'E13nst', 'Andrey', 'Mitroshin', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
