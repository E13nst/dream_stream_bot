package com.example.dream_stream_bot.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionEnvironmentValidatorTest {

    @Test
    void skipsValidationWhenProdProfileInactive() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("default");
        ProductionEnvironmentValidator.validateIfProd(env);
    }

    @Test
    void prodMissingSeveralVariables_reportsAggregatedMessage() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validateIfProd(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required production configuration")
                .hasMessageContaining("OPENAI_API_KEY")
                .hasMessageContaining("DB_HOST")
                .hasMessageContaining("TELEGRAM_API_TOKEN")
                .hasMessageContaining("ADMIN_AUTH_PASSWORD or ADMIN_AUTH_PASSWORD_HASH")
                .hasMessageContaining("TELEGRAM_WEBHOOK_BASE_URL or BOT_WEBHOOK_URL");
    }

    @Test
    void prodWithPasswordHashAndWebhookLongPolling_doesNotRequireWebhookBaseUrl() {
        MockEnvironment env = minimalProdEnv();
        env.setProperty("telegram.delivery-mode", "long-polling");
        env.setProperty("admin.auth.password", "");
        env.setProperty("admin.auth.password-hash", "$2a$10$abcdefghijklmnopqrstuv");

        ProductionEnvironmentValidator.validateIfProd(env);
    }

    @Test
    void prodWithPlainPassword_succeeds() {
        MockEnvironment env = minimalProdEnv();
        env.setProperty("admin.auth.password", "secret");
        env.setProperty("admin.auth.password-hash", "");

        ProductionEnvironmentValidator.validateIfProd(env);
    }

    @Test
    void prodWebhookMode_requiresBaseUrl() {
        MockEnvironment env = minimalProdEnv();
        env.setProperty("admin.auth.password", "secret");
        env.setProperty("telegram.delivery-mode", "webhook");
        env.setProperty("telegram.webhook.base-url", "");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validateIfProd(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TELEGRAM_WEBHOOK_BASE_URL or BOT_WEBHOOK_URL");
    }

    @Test
    void isProdActive_isCaseInsensitive() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("PROD");
        assertThat(ProductionEnvironmentValidator.isProdActive(env)).isTrue();
    }

    private static MockEnvironment minimalProdEnv() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("spring.ai.openai.api-key", "sk-test");
        env.setProperty("DB_HOST", "db");
        env.setProperty("DB_NAME", "mindbase");
        env.setProperty("DB_USERNAME", "u");
        env.setProperty("DB_PASSWORD", "p");
        env.setProperty("telegram.bot.token", "123:abc");
        env.setProperty("telegram.bot.name", "MyBot");
        env.setProperty("telegram.delivery-mode", "webhook");
        env.setProperty("telegram.webhook.base-url", "https://example.com");
        return env;
    }
}
