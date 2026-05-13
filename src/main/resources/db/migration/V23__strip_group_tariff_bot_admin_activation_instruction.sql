-- Убрать из activation_instruction типовой текст про права администратора у бота (V22 / старый Java-дефолт).
UPDATE subscription_tariff
SET activation_instruction = NULL
WHERE activation_instruction IS NOT NULL
  AND (
    activation_instruction LIKE '%Добавьте бота в группу как администратора с правами:%'
    OR activation_instruction LIKE '%Добавьте бота в группу как администратора с правами,%'
    OR activation_instruction LIKE '%Добавьте бота в группу как администратора с правами (например,%'
    OR activation_instruction LIKE '%Добавьте бота в группу как администратора с правами —%'
  );
