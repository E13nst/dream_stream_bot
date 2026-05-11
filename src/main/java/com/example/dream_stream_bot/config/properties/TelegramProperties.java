package com.example.dream_stream_bot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Типизированный доступ к настройкам Telegram-доставки.
 * Заменяет россыпь {@code @Value("${telegram.*}")} в проекте.
 */
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    /**
     * Режим доставки апдейтов: {@code long-polling} (по умолчанию) или {@code webhook}.
     */
    private String deliveryMode = "long-polling";

    private Webhook webhook = new Webhook();

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    public boolean isLongPolling() {
        return "long-polling".equalsIgnoreCase(normalizedDeliveryMode());
    }

    public boolean isWebhookMode() {
        return "webhook".equalsIgnoreCase(normalizedDeliveryMode());
    }

    private String normalizedDeliveryMode() {
        return deliveryMode == null ? "" : deliveryMode.trim().toLowerCase();
    }

    public static class Webhook {
        /** Базовый URL вида {@code https://example.com} (без слэша на конце). */
        private String baseUrl = "";
        /** Секретный токен, валидируется на TelegramWebhookController. */
        private String secretToken = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSecretToken() {
            return secretToken;
        }

        public void setSecretToken(String secretToken) {
            this.secretToken = secretToken;
        }

        public String normalizedBaseUrl() {
            if (baseUrl == null) {
                return "";
            }
            String trimmed = baseUrl.trim();
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }
    }
}
