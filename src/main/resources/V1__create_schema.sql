-- Миграция V1: Создание схемы базы данных с нуля
-- Дата: 2025-01-22
-- База данных: dalekbot-e13nst.db-msk0.amvera.tech

-- ============================================================================
-- Таблица users - Пользователи системы
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    telegram_id     BIGINT UNIQUE NOT NULL,
    username        VARCHAR(255),
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    avatar_url      VARCHAR(512),
    art_balance     BIGINT NOT NULL DEFAULT 0,
    role            VARCHAR(16) NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Комментарии к таблице users
COMMENT ON TABLE users IS 'Таблица пользователей системы';
COMMENT ON COLUMN users.id IS 'Уникальный идентификатор пользователя';
COMMENT ON COLUMN users.telegram_id IS 'ID пользователя в Telegram';
COMMENT ON COLUMN users.username IS 'Username пользователя в Telegram';
COMMENT ON COLUMN users.first_name IS 'Имя пользователя';
COMMENT ON COLUMN users.last_name IS 'Фамилия пользователя';
COMMENT ON COLUMN users.avatar_url IS 'URL аватара пользователя';
COMMENT ON COLUMN users.art_balance IS 'Баланс арт-кредитов пользователя';
COMMENT ON COLUMN users.role IS 'Роль пользователя в системе (USER, ADMIN)';
COMMENT ON COLUMN users.created_at IS 'Дата создания записи';
COMMENT ON COLUMN users.updated_at IS 'Дата последнего обновления записи';

-- Индексы для таблицы users
CREATE INDEX IF NOT EXISTS idx_users_telegram_id ON users(telegram_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username) WHERE username IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- ============================================================================
-- Таблица bot - Боты Telegram
-- ============================================================================
CREATE TABLE IF NOT EXISTS bot (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    username        VARCHAR(64) NOT NULL,
    token           VARCHAR(128) NOT NULL,
    prompt          TEXT,
    webhook_url     VARCHAR(256),
    type            VARCHAR(32) NOT NULL,
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now(),
    description     VARCHAR(256),
    triggers        VARCHAR(256),
    mem_window      INTEGER DEFAULT 100,
    miniapp         VARCHAR(512)
);

-- Комментарии к таблице bot
COMMENT ON TABLE bot IS 'Таблица ботов Telegram';
COMMENT ON COLUMN bot.id IS 'Уникальный идентификатор бота';
COMMENT ON COLUMN bot.name IS 'Отображаемое имя бота';
COMMENT ON COLUMN bot.username IS 'Username бота в Telegram';
COMMENT ON COLUMN bot.token IS 'Токен бота от BotFather';
COMMENT ON COLUMN bot.prompt IS 'Промпт для AI бота';
COMMENT ON COLUMN bot.webhook_url IS 'URL для webhook бота';
COMMENT ON COLUMN bot.type IS 'Тип бота';
COMMENT ON COLUMN bot.is_active IS 'Флаг активности бота';
COMMENT ON COLUMN bot.created_at IS 'Дата создания записи';
COMMENT ON COLUMN bot.updated_at IS 'Дата последнего обновления записи';
COMMENT ON COLUMN bot.description IS 'Описание бота';
COMMENT ON COLUMN bot.triggers IS 'Триггеры бота (через запятую)';
COMMENT ON COLUMN bot.mem_window IS 'Окно памяти для AI бота';
COMMENT ON COLUMN bot.miniapp IS 'Ссылка на миниприложение Telegram';

-- Индексы для таблицы bot
CREATE INDEX IF NOT EXISTS idx_bot_username ON bot(username);
CREATE INDEX IF NOT EXISTS idx_bot_type ON bot(type);
CREATE INDEX IF NOT EXISTS idx_bot_is_active ON bot(is_active);
CREATE INDEX IF NOT EXISTS idx_bot_miniapp ON bot(miniapp) WHERE miniapp IS NOT NULL;

-- ============================================================================
-- Таблица chat_memory - История чата для AI ботов
-- ============================================================================
CREATE TABLE IF NOT EXISTS chat_memory (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_index   INTEGER NOT NULL,
    role            VARCHAR(50) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT now()
);

-- Комментарии к таблице chat_memory
COMMENT ON TABLE chat_memory IS 'История сообщений чата для AI ботов';
COMMENT ON COLUMN chat_memory.id IS 'Уникальный идентификатор записи';
COMMENT ON COLUMN chat_memory.conversation_id IS 'Идентификатор беседы';
COMMENT ON COLUMN chat_memory.message_index IS 'Индекс сообщения в беседе';
COMMENT ON COLUMN chat_memory.role IS 'Роль отправителя (user, assistant, system)';
COMMENT ON COLUMN chat_memory.content IS 'Содержание сообщения';
COMMENT ON COLUMN chat_memory.created_at IS 'Дата создания записи';

-- Индексы для таблицы chat_memory
CREATE INDEX IF NOT EXISTS idx_chat_memory_conversation_id ON chat_memory(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chat_memory_conversation_index ON chat_memory(conversation_id, message_index);
CREATE INDEX IF NOT EXISTS idx_chat_memory_created_at ON chat_memory(created_at);
