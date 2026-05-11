-- Перенос флага подтверждения возраста 18+ на уровень конкретного бота.

ALTER TABLE bot ADD COLUMN IF NOT EXISTS require_age_confirmation BOOLEAN NOT NULL DEFAULT FALSE;

-- Переносим текущее значение из agent_config в связанные боты (если колонка в agent_config уже есть).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'agent_config' AND column_name = 'require_age_confirmation'
    ) THEN
        UPDATE bot b
        SET require_age_confirmation = COALESCE(a.require_age_confirmation, FALSE)
        FROM agent_config a
        WHERE b.agent_config_id = a.id;
    END IF;
END $$;

COMMENT ON COLUMN bot.require_age_confirmation IS 'Если true — в онбординге требуется согласие AGE_18 для этого бота';
