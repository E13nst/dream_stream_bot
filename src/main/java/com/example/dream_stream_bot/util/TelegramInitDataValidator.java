package com.example.dream_stream_bot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class TelegramInitDataValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramInitDataValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long MAX_AUTH_AGE_SECONDS = 3600; // 1 час для тестирования

    private final BotService botService;

    @Autowired
    public TelegramInitDataValidator(BotService botService) {
        this.botService = botService;
    }

    public boolean validateInitData(String initData, String botName) {
        LOGGER.info("🔍 Начинаем валидацию initData для бота: {}", botName);
        
        if (initData == null || initData.trim().isEmpty()) {
            LOGGER.warn("❌ InitData пустая или null");
            return false;
        }
        
        if (botName == null || botName.trim().isEmpty()) {
            LOGGER.warn("❌ Имя бота не указано");
            return false;
        }
        
        try {
            // Получаем токен бота из базы данных
            String botToken = getBotToken(botName);
            if (botToken == null) {
                LOGGER.warn("❌ Токен для бота '{}' не найден", botName);
                return false;
            }
            LOGGER.debug("✅ Токен бота получен (длина: {})", botToken.length());
            
            // Подробное логирование входных данных
            LOGGER.debug("Полный initData: {}", initData);
            
            // Парсим параметры
            Map<String, String> params = parseInitData(initData);
            
            // Проверяем наличие обязательных полей
            if (!params.containsKey("auth_date")) {
                LOGGER.warn("❌ Отсутствует обязательное поле auth_date");
                return false;
            }
            
            String hash = params.get("hash");
            String signature = params.get("signature");
            String authDateStr = params.get("auth_date");
            
            if (hash == null && signature == null) {
                LOGGER.warn("❌ Отсутствуют поля подписи (hash или signature)");
                return false;
            }
            
            // Проверяем время auth_date
            if (!validateAuthDate(authDateStr)) {
                LOGGER.warn("❌ Auth date слишком старая: {}", authDateStr);
                return false;
            }
            
            // Проверяем подпись (поддерживаем оба формата)
            boolean signatureValid = false;
            if (hash != null) {
                signatureValid = validateHash(params, hash, botToken);
                if (!signatureValid) {
                    LOGGER.warn("❌ Неверная подпись hash для бота '{}'", botName);
                }
            } else if (signature != null) {
                signatureValid = validateSignature(params, signature, botToken);
                if (!signatureValid) {
                    LOGGER.warn("❌ Неверная подпись signature для бота '{}'", botName);
                }
            }
            
            if (signatureValid) {
                LOGGER.info("✅ InitData валидна для бота '{}'", botName);
            }
            
            return signatureValid;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка валидации initData для бота '{}': {}", botName, e.getMessage(), e);
            return false;
        }
    }

    private String getBotToken(String botName) {
        try {
            var bots = botService.findAll();
            return bots.stream()
                    .filter(bot -> botName.equals(bot.getName()))
                    .findFirst()
                    .map(BotEntity::getToken)
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка получения токена бота '{}': {}", botName, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> parseInitData(String initData) {
        return Arrays.stream(initData.split("&"))
                .map(param -> param.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> {
                            try {
                                // URL-декодируем значение параметра
                                return java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                LOGGER.warn("⚠️ Ошибка URL-декодирования параметра {}: {}", parts[0], e.getMessage());
                                return parts[1]; // Возвращаем исходное значение если декодирование не удалось
                            }
                        },
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));
    }

    private boolean validateAuthDate(String authDateStr) {
        try {
            long authDate = Long.parseLong(authDateStr);
            long currentTime = Instant.now().getEpochSecond();
            long age = currentTime - authDate;
            return age <= MAX_AUTH_AGE_SECONDS;
        } catch (NumberFormatException e) {
            LOGGER.error("❌ Некорректный формат auth_date: {}", authDateStr);
            return false;
        }
    }

    /**
     * Правильное создание dataCheckString согласно документации Telegram
     * Параметры должны быть отсортированы в лексикографическом порядке
     */
    private String buildDataCheckString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> !"hash".equals(entry.getKey()))  // Исключаем только hash
                .sorted(Map.Entry.comparingByKey())  // Лексикографическая сортировка
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Исправленная версия validateHash
     */
    private boolean validateHash(Map<String, String> params, String expectedHash, String botToken) {
        try {
            // Правильное создание dataCheckString с сортировкой
            String dataCheckString = params.entrySet().stream()
                    .filter(entry -> !"hash".equals(entry.getKey()))  // Исключаем только hash
                    .sorted(Map.Entry.comparingByKey())  // Лексикографическая сортировка
                    .map(entry -> {
                        try {
                            // URL decode значений :cite[1]:cite[6]
                            String decodedValue = java.net.URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8);
                            return entry.getKey() + "=" + decodedValue;
                        } catch (Exception e) {
                            return entry.getKey() + "=" + entry.getValue();
                        }
                    })
                    .collect(Collectors.joining("\n"));

            // Правильный алгоритм HMAC согласно документации :cite[2]
            // 1. secret_key = HMAC-SHA256(bot_token, "WebAppData")
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec botTokenKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(botTokenKeySpec);
            byte[] secretKey = mac.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));

            // 2. hash = HMAC-SHA256(data_check_string, secret_key)
            mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            String calculatedHash = bytesToHex(hashBytes);

            LOGGER.debug("Сравнение хешей: ожидаемый={}, вычисленный={}", expectedHash, calculatedHash);
            LOGGER.debug("Полный токен бота: {}", botToken);
            LOGGER.debug("Полная data check string: {}", dataCheckString.replace("\n", "\\n"));

            return calculatedHash.equals(expectedHash);

        } catch (Exception e) {
            LOGGER.error("Ошибка валидации hash: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Упрощенная и исправленная версия validateSignature
     */
    private boolean validateSignature(Map<String, String> params, String expectedSignature, String botToken) {
        try {
            String dataCheckString = buildDataCheckString(params);
            
            LOGGER.debug("Data check string для signature: {}", dataCheckString);
            
            // Создаем секретный ключ (botToken как ключ, "WebAppData" как данные)
            Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec botTokenKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmacSha256.init(botTokenKeySpec);
            byte[] secretKey = hmacSha256.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));
            
            // Подписываем данные
            hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec dataKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            hmacSha256.init(dataKeySpec);
            byte[] signatureBytes = hmacSha256.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            
            // Только URL-safe Base64 без padding
            String calculatedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            
            LOGGER.debug("Сравнение signature: ожидаемый={}, вычисленный={}", expectedSignature, calculatedSignature);
            
            return calculatedSignature.equals(expectedSignature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.error("❌ Ошибка вычисления signature: {}", e.getMessage(), e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public Long extractTelegramId(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            String userStr = params.get("user");

            if (userStr != null) {
                // Для надежности лучше использовать JSON парсер
                int idIndex = userStr.indexOf("\"id\":");
                if (idIndex != -1) {
                    int start = userStr.indexOf(":", idIndex) + 1;
                    int end = userStr.indexOf(",", start);
                    if (end == -1) end = userStr.indexOf("}", start);
                    if (end != -1) {
                        String idStr = userStr.substring(start, end).trim();
                        return Long.parseLong(idStr.replaceAll("\"", ""));
                    }
                }
            }

            return null;

        } catch (Exception e) {
            LOGGER.error("❌ Ошибка извлечения telegram_id: {}", e.getMessage(), e);
            return null;
        }
    }
}