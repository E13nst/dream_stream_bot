-- Читаемые имена вместо agent_<n>. Суффикс #id сохраняет уникальность при совпадении username.
UPDATE agent_config a
SET name = LEFT('Диалог · ' || sub.username || ' #' || a.id::text, 64)
    FROM (
             SELECT b.agent_config_id AS aid,
                    MIN(b.username)   AS username
             FROM bot b
             WHERE b.agent_config_id IS NOT NULL
             GROUP BY b.agent_config_id
         ) sub
WHERE a.id = sub.aid
  AND a.name ~ '^agent_[0-9]+$';

UPDATE agent_config
SET name = LEFT('Агент #' || id::text, 64)
WHERE name ~ '^agent_[0-9]+$';
