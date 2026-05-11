package com.example.dream_stream_bot.service.access;

import com.example.dream_stream_bot.service.settings.SystemSettingsService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory дедупликация заглушек.
 * Хранит пары (ключ → timestamp последней отправки) и решает, прошло ли достаточно времени,
 * чтобы повторно отправить сообщение пользователю.
 *
 * TTL в часах настраивается через {@code system_settings.GATING_DEDUP_HOURS} (default 24).
 */
@Component
public class GatingDedup {

    public static final String KEY_GATING_DEDUP_HOURS = "GATING_DEDUP_HOURS";

    private static final int DEFAULT_HOURS = 24;

    private final SystemSettingsService settingsService;
    private final ConcurrentMap<String, Long> lastSentAt = new ConcurrentHashMap<>();

    public GatingDedup(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Если для ключа в окне {@code ttl} уже отправляли сообщение — возвращает {@code false}
     * и НЕ обновляет timestamp. Если можно отправлять — возвращает {@code true} и сохраняет
     * текущее время.
     */
    public boolean acquire(String key) {
        long now = System.currentTimeMillis();
        long ttlMs = (long) settingsService.getInt(KEY_GATING_DEDUP_HOURS, DEFAULT_HOURS) * 3600_000L;
        Long previous = lastSentAt.get(key);
        if (previous != null && now - previous < ttlMs) {
            return false;
        }
        lastSentAt.put(key, now);
        return true;
    }

    /** Сброс ключа — используется тестами и при ручных операциях. */
    public void reset(String key) {
        lastSentAt.remove(key);
    }

    public static String key(String namespace, Long botId, Long chatId, Long userId, String reason) {
        return namespace + ":" + botId + ":" + chatId + ":" + userId + ":" + reason;
    }
}
