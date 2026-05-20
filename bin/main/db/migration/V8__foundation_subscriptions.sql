-- Миграция V8: Фундамент для подписочной модели и compliance.
--
-- 1. Расширяем agent_config (display_name, short_description, data_locality, is_public).
-- 2. Добавляем users.referred_by_user_id для будущей реферальной программы.
-- 3. В chat_memory добавляем telegram_message_id и message_thread_id для поддержки
--    форум-топиков, edit_message и точечного удаления.
-- 4. Добавляем параметры RETENTION_DAYS_AFTER_EXPIRY и RETENTION_UNLIMITED в system_settings.

-- ----------------------------------------------------------------------------
-- agent_config: новые поля для UX и compliance
-- ----------------------------------------------------------------------------
ALTER TABLE agent_config ADD COLUMN IF NOT EXISTS display_name        VARCHAR(128);
ALTER TABLE agent_config ADD COLUMN IF NOT EXISTS short_description   VARCHAR(512);
ALTER TABLE agent_config ADD COLUMN IF NOT EXISTS data_locality       VARCHAR(32) NOT NULL DEFAULT 'CROSS_BORDER';
ALTER TABLE agent_config ADD COLUMN IF NOT EXISTS is_public           BOOLEAN     NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN agent_config.display_name      IS 'Отображаемое имя агента для пользователя при онбординге';
COMMENT ON COLUMN agent_config.short_description IS 'Короткое описание агента (модель, локализация, особенности)';
COMMENT ON COLUMN agent_config.data_locality    IS 'LOCAL_RU — обработка в РФ, CROSS_BORDER — трансграничная передача';
COMMENT ON COLUMN agent_config.is_public         IS 'Разрешено ли подключать этого агента к публичным ботам';

-- Backfill display_name из name, чтобы старые агенты остались отображаемыми.
UPDATE agent_config SET display_name = name WHERE display_name IS NULL;

-- ----------------------------------------------------------------------------
-- users: реферальная связь
-- ----------------------------------------------------------------------------
ALTER TABLE users ADD COLUMN IF NOT EXISTS referred_by_user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_referred_by'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_referred_by
            FOREIGN KEY (referred_by_user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_referred_by ON users(referred_by_user_id);

COMMENT ON COLUMN users.referred_by_user_id IS 'Пользователь, по чьей реферальной ссылке зарегистрирован';

-- ----------------------------------------------------------------------------
-- chat_memory: telegram_message_id и message_thread_id
-- ----------------------------------------------------------------------------
ALTER TABLE chat_memory ADD COLUMN IF NOT EXISTS telegram_message_id INTEGER;
ALTER TABLE chat_memory ADD COLUMN IF NOT EXISTS message_thread_id   INTEGER;

CREATE INDEX IF NOT EXISTS idx_chat_memory_conv_tg_msg
    ON chat_memory(conversation_id, telegram_message_id)
    WHERE telegram_message_id IS NOT NULL;

COMMENT ON COLUMN chat_memory.telegram_message_id IS 'message_id в Telegram (для обработки edited_message и /forget_last)';
COMMENT ON COLUMN chat_memory.message_thread_id   IS 'message_thread_id в Telegram (топик в форум-группе)';

-- ----------------------------------------------------------------------------
-- system_settings: значения по умолчанию для retention
-- ----------------------------------------------------------------------------
INSERT INTO system_settings (setting_key, setting_value, updated_at)
VALUES ('RETENTION_DAYS_AFTER_EXPIRY', '90', now())
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, updated_at)
VALUES ('RETENTION_UNLIMITED', 'false', now())
ON CONFLICT (setting_key) DO NOTHING;
