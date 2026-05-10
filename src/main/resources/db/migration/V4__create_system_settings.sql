-- Миграция V4: Таблица системных настроек (key-value хранилище)
-- Используется для хранения admin password hash и других системных параметров.

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(128) PRIMARY KEY,
    setting_value TEXT         NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

COMMENT ON TABLE system_settings IS 'Системные настройки приложения (key-value)';
COMMENT ON COLUMN system_settings.setting_key IS 'Ключ настройки';
COMMENT ON COLUMN system_settings.setting_value IS 'Значение настройки';
COMMENT ON COLUMN system_settings.updated_at IS 'Время последнего обновления';
