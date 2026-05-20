-- LLM prompt and memory window live only on agent_config.
ALTER TABLE bot DROP COLUMN IF EXISTS prompt;
ALTER TABLE bot DROP COLUMN IF EXISTS mem_window;
