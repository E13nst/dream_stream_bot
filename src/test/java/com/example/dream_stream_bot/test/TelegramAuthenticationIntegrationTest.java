package com.example.dream_stream_bot.test;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.util.TelegramInitDataValidator;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Автотесты для проверки аутентификации через Telegram initData
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
public class TelegramAuthenticationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private BotService botService;

    @Autowired
    private TelegramInitDataValidator validator;

    // Тест использует первого доступного бота из базы данных
    private String testBotName;
    private static final Long TEST_USER_ID = 141614461L;
    private static final String TEST_USER_FIRST_NAME = "Andrey";
    private static final String TEST_USER_LAST_NAME = "Mitroshin";
    private static final String TEST_USER_USERNAME = "E13nst";
    private static final String TEST_USER_LANGUAGE_CODE = "ru";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        
        // Получаем первого доступного бота из базы данных
        var bots = botService.findAll();
        if (!bots.isEmpty()) {
            testBotName = bots.get(0).getName();
        } else {
            testBotName = null;
        }
    }

    /**
     * Тест 1: Проверка существования ботов в базе данных
     */
    @Test
    void testBotsExist() {
        System.out.println("🧪 Тест 1: Проверка существования ботов в базе данных");
        
        // Получаем всех ботов
        var bots = botService.findAll();
        System.out.println("📋 Найдено ботов в базе: " + bots.size());
        
        assertFalse(bots.isEmpty(), "В базе данных должен быть хотя бы один бот");
        
        // Проверяем через API
        Response response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bots")
                .then()
                .statusCode(401) // Должен требовать авторизацию
                .extract().response();
        
        System.out.println("✅ API /api/bots требует авторизацию (статус 401)");
    }

    /**
     * Тест 2: Генерация и валидация initData для тестового пользователя
     */
    @Test
    void testInitDataGenerationAndValidation() {
        System.out.println("🧪 Тест 2: Генерация и валидация initData");
        
        if (testBotName == null) {
            System.out.println("⏭️ Пропускаем тест: нет доступных ботов в базе данных");
            return;
        }
        
        // Получаем бота
        var bots = botService.findAll();
        BotEntity testBot = bots.stream()
                .filter(bot -> testBotName.equals(bot.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Тестовый бот не найден"));
        
        // Генерируем initData
        String initData = generateInitData(testBot.getToken());
        System.out.println("🔐 Сгенерирован initData: " + initData.substring(0, Math.min(100, initData.length())) + "...");
        
        // Валидируем initData
        boolean isValid = validator.validateInitData(initData, testBotName);
        assertTrue(isValid, "InitData должен быть валидным");
        System.out.println("✅ InitData валиден");
        
        // Извлекаем telegram_id
        Long extractedTelegramId = validator.extractTelegramId(initData);
        assertEquals(TEST_USER_ID, extractedTelegramId, "Извлеченный telegram_id должен совпадать");
        System.out.println("✅ Извлечен telegram_id: " + extractedTelegramId);
    }

    /**
     * Тест 3: Полная аутентификация
     */
    @Test
    void testFullAuthentication() {
        System.out.println("🧪 Тест 3: Полная аутентификация");
        
        if (testBotName == null) {
            System.out.println("⏭️ Пропускаем тест: нет доступных ботов в базе данных");
            return;
        }
        
        // Получаем бота
        var bots = botService.findAll();
        BotEntity testBot = bots.stream()
                .filter(bot -> testBotName.equals(bot.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Тестовый бот не найден"));
        
        // Генерируем initData
        String initData = generateInitData(testBot.getToken());
        System.out.println("🔐 Используем initData для аутентификации");
        
        // Проверяем статус аутентификации без заголовков
        Response statusResponse = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/auth/status")
                .then()
                .statusCode(200)
                .body("authenticated", equalTo(false))
                .extract().response();
        
        System.out.println("✅ Статус без аутентификации: " + statusResponse.body().asString());
        
        // Проверяем статус аутентификации с заголовками
        Response authStatusResponse = given()
                .contentType(ContentType.JSON)
                .header("X-Telegram-Init-Data", initData)
                .header("X-Telegram-Bot-Name", testBotName)
                .when()
                .get("/auth/status")
                .then()
                .statusCode(200)
                .extract().response();
        
        System.out.println("✅ Статус с аутентификацией: " + authStatusResponse.body().asString());
    }

    /**
     * Генерирует initData для тестового пользователя
     */
    private String generateInitData(String botToken) {
        try {
            // Создаем параметры initData
            TreeMap<String, String> params = new TreeMap<>();
            
            // Добавляем обязательные параметры
            params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
            params.put("auth_date", String.valueOf(Instant.now().getEpochSecond()));
            
            // Добавляем информацию о пользователе
            String userJson = String.format(
                "{\"id\":%d,\"first_name\":\"%s\",\"last_name\":\"%s\",\"username\":\"%s\",\"language_code\":\"%s\"}",
                TEST_USER_ID,
                TEST_USER_FIRST_NAME,
                TEST_USER_LAST_NAME,
                TEST_USER_USERNAME,
                TEST_USER_LANGUAGE_CODE
            );
            params.put("user", userJson);
            
            // Создаем строку для подписи (все параметры кроме hash, отсортированные)
            String dataCheckString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            
            // Вычисляем HMAC-SHA256 подпись согласно документации Telegram
            // Шаг 1: Создаем секретный ключ (secret_key = HMAC-SHA256(bot_token, "WebAppData"))
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec botTokenKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(botTokenKeySpec);
            byte[] secretKey = mac.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));
            
            // Шаг 2: Вычисляем hash (hash = HMAC-SHA256(data_check_string, secret_key))
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHex(hashBytes);
            
            // Добавляем hash к параметрам
            params.put("hash", hash);
            
            // Формируем финальную строку initData
            String initData = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));
            
            return initData;
            
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации initData", e);
        }
    }
    
    /**
     * Конвертирует байты в hex строку
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
