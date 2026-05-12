-- Удалить устаревшие привязки согласий с ботов (личные коды PERSONAL_DATA, CROSS_BORDER, AGE_18).
DELETE FROM bot_consent_binding WHERE consent_code IN ('PERSONAL_DATA', 'CROSS_BORDER', 'AGE_18');

-- Настройка возраста 18+ удалена из карточки бота (не используется в онбординге).
ALTER TABLE bot DROP COLUMN IF EXISTS require_age_confirmation;
