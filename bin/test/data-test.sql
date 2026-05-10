-- Тестовые данные для автотестов

DELETE FROM users;
DELETE FROM bot_keyword;
DELETE FROM bot;
DELETE FROM agent_config;

INSERT INTO bot (name, username, token, type, is_active, mem_window, created_at, updated_at)
VALUES (
    'IntegrationTestBot',
    'integration_test_bot',
    '123456789:AAIntegrationTestTokenForHmacSigningOnly',
    'assistant',
    true,
    100,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO agent_config (name, role, provider, model, system_prompt, mem_window, created_at, updated_at)
SELECT 'agent_' || CAST(id AS VARCHAR), 'CONVERSATION', 'OPENAI', 'gpt-4o', 'Test system prompt', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bot WHERE username = 'integration_test_bot';

UPDATE bot b
SET agent_config_id = (
    SELECT a.id FROM agent_config a WHERE a.name = ('agent_' || CAST(b.id AS VARCHAR))
)
WHERE b.username = 'integration_test_bot';

INSERT INTO users (telegram_id, username, first_name, last_name, role, created_at, updated_at)
VALUES (141614461, 'E13nst', 'Andrey', 'Mitroshin', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
