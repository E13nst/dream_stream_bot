package com.example.dream_stream_bot.config;

import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.Locale;

/**
 * Fail-fast validation for production configuration (resolved from env / config).
 * Invoked from {@link ProductionEnvironmentValidationPostProcessor} before any beans are created.
 */
public final class ProductionEnvironmentValidator {

    private ProductionEnvironmentValidator() {
    }

    /**
     * When the {@code prod} profile is active, ensures required settings are present.
     * No-op for other profiles.
     */
    public static void validateIfProd(Environment environment) {
        if (!isProdActive(environment)) {
            return;
        }
        List<String> errors = new ArrayList<>();

        requireNonBlank(environment, "spring.ai.openai.api-key", "OPENAI_API_KEY", errors);

        requireNonBlank(environment, "DB_HOST", "DB_HOST", errors);
        requireNonBlank(environment, "DB_NAME", "DB_NAME", errors);
        requireNonBlank(environment, "DB_USERNAME", "DB_USERNAME", errors);
        requireNonBlank(environment, "DB_PASSWORD", "DB_PASSWORD", errors);

        requireAdminCredential(environment, errors);

        requireWebhookBaseIfWebhookMode(environment, errors);

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Missing required production configuration:\n");
            for (String line : errors) {
                sb.append("- ").append(line).append('\n');
            }
            throw new IllegalStateException(sb.toString().trim());
        }
    }

    static boolean isProdActive(Environment environment) {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private static void requireNonBlank(Environment environment, String propertyKey, String envHint, List<String> errors) {
        String value = environment.getProperty(Objects.requireNonNull(propertyKey, "propertyKey"));
        if (isBlank(value)) {
            errors.add(envHint + " (property `" + propertyKey + "`)");
        }
    }

    private static void requireAdminCredential(Environment environment, List<String> errors) {
        String password = environment.getProperty("admin.auth.password");
        String passwordHash = environment.getProperty("admin.auth.password-hash");
        if (isBlank(password) && isBlank(passwordHash)) {
            errors.add("ADMIN_AUTH_PASSWORD or ADMIN_AUTH_PASSWORD_HASH (properties `admin.auth.password` / `admin.auth.password-hash`)");
        }
    }

    private static void requireWebhookBaseIfWebhookMode(Environment environment, List<String> errors) {
        String mode = environment.getProperty("telegram.delivery-mode", "webhook");
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        if (!"webhook".equals(normalized)) {
            return;
        }
        String baseUrl = environment.getProperty("telegram.webhook.base-url");
        if (isBlank(baseUrl)) {
            errors.add("TELEGRAM_WEBHOOK_BASE_URL or BOT_WEBHOOK_URL (property `telegram.webhook.base-url`) when telegram.delivery-mode=webhook");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
