-- Нормализация триггеров: отдельная таблица bot_keyword, удаление bot.triggers

CREATE TABLE IF NOT EXISTS bot_keyword (
    id          BIGSERIAL PRIMARY KEY,
    bot_id      BIGINT NOT NULL REFERENCES bot(id) ON DELETE CASCADE,
    keyword     VARCHAR(256) NOT NULL,
    created_at  TIMESTAMP DEFAULT now(),
    CONSTRAINT uq_bot_keyword_bot_id_keyword UNIQUE (bot_id, keyword)
);

CREATE INDEX IF NOT EXISTS idx_bot_keyword_bot_id ON bot_keyword(bot_id);

-- Перенос из CSV triggers (PostgreSQL)
INSERT INTO bot_keyword (bot_id, keyword, created_at)
SELECT DISTINCT b.id, TRIM(x.word), now()
FROM bot b
CROSS JOIN LATERAL unnest(string_to_array(COALESCE(b.triggers, ''), ',')) AS x(word)
WHERE TRIM(x.word) <> ''
ON CONFLICT (bot_id, keyword) DO NOTHING;

ALTER TABLE bot DROP COLUMN IF EXISTS triggers;

COMMENT ON TABLE bot_keyword IS 'Ключевые слова-триггеры для ответа бота в группах';
COMMENT ON COLUMN bot_keyword.bot_id IS 'Ссылка на бота';
COMMENT ON COLUMN bot_keyword.keyword IS 'Ключевое слово (подстрока в тексте сообщения, без учёта регистра при проверке)';
