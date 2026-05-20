-- Миграция V17: привязки версий документов согласий к конкретному боту.
-- Источник истины для runtime-проверок: bot + consent_code -> document_id.

CREATE TABLE IF NOT EXISTS bot_consent_binding (
    id           BIGSERIAL PRIMARY KEY,
    bot_id       BIGINT NOT NULL REFERENCES bot(id) ON DELETE CASCADE,
    consent_code VARCHAR(32) NOT NULL,
    document_id  BIGINT NOT NULL REFERENCES consent_document(id),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

COMMENT ON TABLE bot_consent_binding IS 'Активные и исторические привязки документов согласий к боту';
COMMENT ON COLUMN bot_consent_binding.consent_code IS 'Дублирует code документа для быстрых выборок по боту';

CREATE INDEX IF NOT EXISTS idx_bot_consent_binding_bot_id
    ON bot_consent_binding(bot_id);

CREATE INDEX IF NOT EXISTS idx_bot_consent_binding_document_id
    ON bot_consent_binding(document_id);

CREATE INDEX IF NOT EXISTS idx_bot_consent_binding_bot_code
    ON bot_consent_binding(bot_id, consent_code);

CREATE INDEX IF NOT EXISTS idx_bot_consent_binding_active_lookup
    ON bot_consent_binding(bot_id, consent_code, is_active);

-- Backfill для существующих данных:
-- для каждого бота и каждой current-версии документа создаем active-привязку.
INSERT INTO bot_consent_binding (bot_id, consent_code, document_id, is_active, created_at, updated_at)
SELECT b.id, d.code, d.id, TRUE, now(), now()
FROM bot b
JOIN consent_document d ON d.is_current = TRUE
LEFT JOIN bot_consent_binding existing
       ON existing.bot_id = b.id
      AND existing.consent_code = d.code
      AND existing.is_active = TRUE
WHERE existing.id IS NULL;
