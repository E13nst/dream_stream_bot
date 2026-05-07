-- Тестовые данные для автотестов

-- Очищаем таблицы
DELETE FROM stickersets;
DELETE FROM users;
DELETE FROM bot;

-- Вставляем тестового пользователя
INSERT INTO users (telegram_id, username, first_name, last_name, role, art_balance, created_at, updated_at) 
VALUES (141614461, 'E13nst', 'Andrey', 'Mitroshin', 'USER', 0, NOW(), NOW());
