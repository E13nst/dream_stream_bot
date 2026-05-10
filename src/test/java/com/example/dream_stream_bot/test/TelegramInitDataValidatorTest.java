package com.example.dream_stream_bot.test;

import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.util.TelegramInitDataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты валидатора initData без поднятия Spring-контекста (избегаем Flyway/БД для профиля dev).
 */
@ExtendWith(MockitoExtension.class)
class TelegramInitDataValidatorTest {

    @Mock
    private BotService botService;

    private TelegramInitDataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TelegramInitDataValidator(botService);
    }

    @Test
    void testParseInitData() {
        System.out.println("🧪 Тест парсинга initData");

        String testInitData = "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%2C%22first_name%22%3A%22Test%22%2C%22last_name%22%3A%22User%22%2C%22username%22%3A%22testuser%22%2C%22language_code%22%3A%22en%22%7D&auth_date=1640995200&hash=test_hash_for_development_only";

        when(botService.findAll()).thenReturn(java.util.Collections.emptyList());

        boolean isValid = validator.validateInitData(testInitData, "NonExistentBot");

        System.out.println("🔍 Результат валидации: " + isValid);

        assertFalse(isValid, "Тестовый initData должен быть невалидным");
    }

    @Test
    void testExtractTelegramId() {
        System.out.println("🧪 Тест извлечения telegram_id");

        String testInitData = "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%2C%22first_name%22%3A%22Test%22%2C%22last_name%22%3A%22User%22%2C%22username%22%3A%22testuser%22%2C%22language_code%22%3A%22en%22%7D&auth_date=1640995200&hash=test_hash_for_development_only";

        Long telegramId = validator.extractTelegramId(testInitData);

        System.out.println("🔍 Извлеченный telegram_id: " + telegramId);

        assertEquals(123456789L, telegramId, "Telegram ID должен быть извлечен корректно");
    }
}
