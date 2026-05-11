-- Миграция V10: версионируемые согласия и журнал принятий.
--
-- Коды документов:
--   OFFER          — публичная оферта.
--   PRIVACY_POLICY — политика обработки персональных данных.
--   PERSONAL_DATA  — согласие на обработку ПДн.
--   CROSS_BORDER   — согласие на трансграничную передачу (нужно при CROSS_BORDER агентах).
--   AGE_18         — подтверждение возраста 18+.
--
-- Версии монотонные. При публикации новой версии change_type=MATERIAL запускает
-- 14-дневный грейс на повторное принятие; по его истечении подписка переходит
-- в BLOCKED_CONSENT (см. SubscriptionService).

CREATE TABLE IF NOT EXISTS consent_document (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(32)  NOT NULL,
    version        INTEGER      NOT NULL,
    title          VARCHAR(256) NOT NULL,
    body_markdown  TEXT,
    external_url   TEXT,
    telegraph_path VARCHAR(256),
    change_type    VARCHAR(16)  NOT NULL DEFAULT 'MINOR',
    is_current     BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at   TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

COMMENT ON TABLE  consent_document             IS 'Документы согласий (политика, оферта, согласие на ПД и пр.) с версионированием';
COMMENT ON COLUMN consent_document.code        IS 'OFFER | PRIVACY_POLICY | PERSONAL_DATA | CROSS_BORDER | AGE_18';
COMMENT ON COLUMN consent_document.change_type IS 'MINOR — без перепринятия; MATERIAL — 14-дневный грейс';

CREATE UNIQUE INDEX IF NOT EXISTS idx_consent_document_code_version
    ON consent_document(code, version);

CREATE UNIQUE INDEX IF NOT EXISTS idx_consent_document_current
    ON consent_document(code)
    WHERE is_current = TRUE;

-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_consent (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id         BIGINT       NOT NULL REFERENCES consent_document(id),
    subscription_id     BIGINT       REFERENCES subscription(id) ON DELETE SET NULL,
    chat_id             BIGINT,
    accepted_via        VARCHAR(32),
    telegram_message_id INTEGER,
    accepted_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    revoked_at          TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE  user_consent              IS 'Журнал принятий согласий пользователями';
COMMENT ON COLUMN user_consent.accepted_via IS 'BOT_BUTTON | BOT_TEXT | ADMIN | WEB';

CREATE INDEX IF NOT EXISTS idx_user_consent_user     ON user_consent(user_id);
CREATE INDEX IF NOT EXISTS idx_user_consent_document ON user_consent(document_id);
CREATE INDEX IF NOT EXISTS idx_user_consent_sub      ON user_consent(subscription_id);
