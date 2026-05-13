-- Текст инструкции после активации группового тарифа (права бота и т.д.)
ALTER TABLE subscription_tariff
    ADD COLUMN IF NOT EXISTS activation_instruction TEXT;

-- Групповый триал 3 дня: не default_group, не default_personal
INSERT INTO subscription_tariff (
    bot_id, code, title, scope, access_mode, trial_days, max_participants, sort_order, active,
    default_personal, default_group, activation_instruction
)
SELECT
    b.id,
    'GROUP_TRIAL',
    'Группа (пробный период 3 дня)',
    'GROUP',
    'TRIAL_ONBOARDING',
    3,
    50,
    5,
    TRUE,
    FALSE,
    FALSE,
    'Добавьте бота в группу как администратора с правами: удаление сообщений (по необходимости), закрепление, приглашение пользователей — в зависимости от настроек вашего сценария. Участники должны один раз принять условия в личке с ботом по ссылке из сообщения после активации.'
FROM bot b
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_tariff t
    WHERE t.bot_id = b.id AND t.code = 'GROUP_TRIAL'
);
