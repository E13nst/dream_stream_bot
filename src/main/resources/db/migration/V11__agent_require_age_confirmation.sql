-- Требование подтверждения возраста 18+ при онбординге (по выбору для каждого agent_config).

ALTER TABLE agent_config ADD COLUMN IF NOT EXISTS require_age_confirmation BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN agent_config.require_age_confirmation IS 'Если true — в онбординге требуется согласие AGE_18; по умолчанию выключено';
