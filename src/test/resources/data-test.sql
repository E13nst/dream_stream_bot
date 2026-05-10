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

INSERT INTO agent_config (name, role, provider, model, system_prompt, mem_window, created_at, updated_at)
SELECT SUBSTRING('Диалог · ' || username || ' #' || CAST(id AS VARCHAR), 1, 64),
       'CONVERSATION', 'OPENAI', 'gpt-4o', 'Test system prompt', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bot WHERE username = 'integration_test_bot';

UPDATE bot
SET agent_config_id = (SELECT id FROM agent_config WHERE name LIKE 'Диалог · integration_test_bot%' FETCH FIRST 1 ROW ONLY);

INSERT INTO users (telegram_id, username, first_name, last_name, role, created_at, updated_at)
VALUES (141614461, 'E13nst', 'Andrey', 'Mitroshin', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
