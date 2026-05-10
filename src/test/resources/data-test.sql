-- Тестовые данные для автотестов

-- Очищаем таблицы
DELETE FROM users;
DELETE FROM bot_keyword;
DELETE FROM bot;

-- Тестовый бот для интеграционных тестов (BotService.findAll и т.п.)
INSERT INTO bot (name, username, token, type, is_active, mem_window, created_at, updated_at)
VALUES (
    'IntegrationTestBot',
    'integration_test_bot',
    '123456789:AAIntegrationTestTokenForHmacSigningOnly',
    'assistant',
    true,
    100,
    NOW(),
    NOW()
);

-- Вставляем тестового пользователя
INSERT INTO users (telegram_id, username, first_name, last_name, role, art_balance, created_at, updated_at) 
VALUES (141614461, 'E13nst', 'Andrey', 'Mitroshin', 'USER', 0, NOW(), NOW());
