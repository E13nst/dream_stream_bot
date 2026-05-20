-- Журнал факта удаления данных пользователя по /forget_me (в рамках одного бота).
-- telegram_id_hash = SHA-256(bot_id + ':' + telegram_user_id + salt) — сырой telegram_id не хранится.

CREATE TABLE IF NOT EXISTS bot_user_erasure (
    id                 BIGSERIAL PRIMARY KEY,
    bot_id             BIGINT       NOT NULL REFERENCES bot (id) ON DELETE CASCADE,
    telegram_id_hash   VARCHAR(64)  NOT NULL,
    erased_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_bot_user_erasure UNIQUE (bot_id, telegram_id_hash)
);

CREATE INDEX IF NOT EXISTS idx_bot_user_erasure_bot ON bot_user_erasure (bot_id);

COMMENT ON TABLE bot_user_erasure IS 'Факт удаления ПДн пользователя по запросу /forget_me (обезличенный идентификатор)';
COMMENT ON COLUMN bot_user_erasure.telegram_id_hash IS 'SHA-256 hex; salt задаётся app.privacy.erasure-salt';
