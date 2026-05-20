-- Longer user-facing bot description for /start intro.
ALTER TABLE bot ALTER COLUMN description TYPE TEXT;

COMMENT ON COLUMN bot.description IS 'User-facing bot description shown on /start before onboarding/payment';

WITH prompt_hints AS (
    SELECT
        b.id AS bot_id,
        COALESCE(NULLIF(BTRIM(a.display_name), ''), b.name) AS bot_label,
        NULLIF(BTRIM(a.short_description), '') AS short_description,
        LOWER(COALESCE(a.system_prompt, '')) AS prompt_text
    FROM bot b
    LEFT JOIN agent_config a ON a.id = b.agent_config_id
)
UPDATE bot b
SET description = LEFT(
        'Привет! Я ' || h.bot_label || '.' || E'\n\n' ||
        COALESCE(
            h.short_description,
            CASE
                WHEN h.prompt_text LIKE '%сон%' OR h.prompt_text LIKE '%dream%' THEN
                    'Помогаю разбирать сны: находить повторяющиеся образы, возможные смыслы и бережные вопросы для саморефлексии.'
                WHEN h.prompt_text LIKE '%псих%' OR h.prompt_text LIKE '%терап%' OR h.prompt_text LIKE '%эмоц%' THEN
                    'Помогаю в поддерживающем диалоге: разложить переживания по полочкам, заметить эмоции и подобрать следующий спокойный шаг.'
                WHEN h.prompt_text LIKE '%таро%' OR h.prompt_text LIKE '%карты%' THEN
                    'Помогаю исследовать ситуацию через символы и вопросы для размышления, без обещаний предсказаний или гарантий.'
                WHEN h.prompt_text LIKE '%коуч%' OR h.prompt_text LIKE '%цель%' OR h.prompt_text LIKE '%план%' THEN
                    'Помогаю прояснять цели, формулировать варианты действий и двигаться небольшими понятными шагами.'
                ELSE
                    'Помогаю в личном диалоге в формате, заданном настройками этого бота.'
            END
        ) || E'\n\n' ||
        'Нажмите «Начать», чтобы активировать доступ. Оферту покажем только при переходе к оплате.',
        2048
    )
FROM prompt_hints h
WHERE b.id = h.bot_id
  AND (b.description IS NULL OR BTRIM(b.description) = '');
