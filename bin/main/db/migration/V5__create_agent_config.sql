-- Agent configuration (LLM provider, model, hyperparameters) separate from Telegram bot row.
-- Legacy bot.prompt / bot.mem_window remain for fallback.

CREATE TABLE IF NOT EXISTS agent_config (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(64)  NOT NULL,
    role               VARCHAR(32)  NOT NULL DEFAULT 'CONVERSATION',
    provider           VARCHAR(32)  NOT NULL DEFAULT 'OPENAI',
    model              VARCHAR(64)  NOT NULL DEFAULT 'gpt-4o',
    temperature        DOUBLE PRECISION,
    top_p              DOUBLE PRECISION,
    frequency_penalty  DOUBLE PRECISION,
    presence_penalty   DOUBLE PRECISION,
    system_prompt      TEXT,
    mem_window         INTEGER DEFAULT 100,
    created_at         TIMESTAMP DEFAULT now(),
    updated_at         TIMESTAMP DEFAULT now()
);

COMMENT ON TABLE agent_config IS 'Per-agent LLM settings (model, prompt, sampling parameters)';
CREATE UNIQUE INDEX IF NOT EXISTS idx_agent_config_name ON agent_config(name);

-- One agent_config per existing bot; name is stable and unique (agent_<id>)
INSERT INTO agent_config (name, role, model, system_prompt, mem_window)
SELECT 'agent_' || b.id::text,
       'CONVERSATION',
       'gpt-4o',
       b.prompt,
       COALESCE(b.mem_window, 100)
FROM bot b;

ALTER TABLE bot ADD COLUMN IF NOT EXISTS agent_config_id BIGINT REFERENCES agent_config(id);

UPDATE bot b
SET agent_config_id = a.id
FROM agent_config a
WHERE a.name = 'agent_' || b.id::text;
