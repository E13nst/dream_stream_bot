package com.example.dream_stream_bot.service.settings;

import com.example.dream_stream_bot.model.settings.SystemSetting;
import com.example.dream_stream_bot.model.settings.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Доступ к key-value таблице system_settings.
 * Поддерживает типизированные геттеры для часто используемых параметров.
 */
@Service
public class SystemSettingsService {

    /** Срок хранения данных после окончания подписки (в днях). */
    public static final String KEY_RETENTION_DAYS_AFTER_EXPIRY = "RETENTION_DAYS_AFTER_EXPIRY";

    /** Если true — retention job не удаляет данные (для роли оператора ПД с бессрочным хранением). */
    public static final String KEY_RETENTION_UNLIMITED = "RETENTION_UNLIMITED";

    private static final int DEFAULT_RETENTION_DAYS = 90;

    private final SystemSettingsRepository repository;

    public SystemSettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    public Optional<String> get(String key) {
        return repository.findById(key).map(SystemSetting::getValue);
    }

    public String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return get(key)
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public boolean getBool(String key, boolean defaultValue) {
        return get(key)
                .map(value -> Boolean.parseBoolean(value.trim()))
                .orElse(defaultValue);
    }

    @Transactional
    public void set(String key, String value) {
        SystemSetting setting = repository.findById(key).orElseGet(() -> new SystemSetting(key, value));
        setting.setValue(value);
        repository.save(setting);
    }

    public int getRetentionDaysAfterExpiry() {
        return getInt(KEY_RETENTION_DAYS_AFTER_EXPIRY, DEFAULT_RETENTION_DAYS);
    }

    public boolean isRetentionUnlimited() {
        return getBool(KEY_RETENTION_UNLIMITED, false);
    }
}
