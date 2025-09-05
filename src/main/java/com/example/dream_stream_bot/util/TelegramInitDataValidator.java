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

/**
 * Утилита для валидации Telegram Web App initData
 */
@Component
public class TelegramInitDataValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramInitDataValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long MAX_AUTH_AGE_SECONDS = 600; // 10 минут для разработки
    
    private final BotService botService;
    
    @Autowired
    public TelegramInitDataValidator(BotService botService) {
        this.botService = botService;
    }
    
    /**
     * Валидирует Telegram initData для конкретного бота
     * 
     * @param initData строка initData от Telegram Web App
     * @param botName имя бота для получения токена
     * @return true если валидна, false иначе
     */
    public boolean validateInitData(String initData, String botName) {
        LOGGER.info("🔍 Начинаем валидацию initData для бота: {}", botName);
        LOGGER.debug("🔍 InitData (первые 100 символов): {}", 
                initData != null && initData.length() > 100 ? initData.substring(0, 100) + "..." : initData);
        
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
            LOGGER.debug("🔍 Получаем токен бота из базы данных");
            String botToken = getBotToken(botName);
            if (botToken == null) {
                LOGGER.warn("❌ Токен для бота '{}' не найден", botName);
                return false;
            }
            LOGGER.debug("✅ Токен бота получен (длина: {})", botToken.length());
            
            // Парсим параметры
            LOGGER.debug("🔍 Парсим параметры initData");
            Map<String, String> params = parseInitData(initData);
            LOGGER.debug("🔍 Парсированные параметры: {}", params.keySet());
            
            // Проверяем наличие обязательных полей (поддерживаем как hash, так и signature)
            if (!params.containsKey("auth_date")) {
                LOGGER.warn("❌ Отсутствует обязательное поле auth_date");
                LOGGER.debug("🔍 Доступные поля: {}", params.keySet());
                return false;
            }
            
            // Определяем тип подписи (старый hash или новый signature)
                            String hash = params.get("hash");
                String signature = params.get("signature");
                String authDateStr = params.get("auth_date");
                
                if (hash == null && signature == null) {
                    LOGGER.warn("❌ Отсутствуют поля подписи (hash или signature)");
                    LOGGER.debug("🔍 Доступные поля: {}", params.keySet());
                    return false;
                }
                
                // ПРИОРИТЕТ: сначала пробуем hash (для владельцев бота), потом signature (для третьих сторон)
                if (hash != null) {
                    LOGGER.debug("🔍 Используем алгоритм hash (для владельцев бота): {} | AuthDate: {}", hash, authDateStr);
                } else if (signature != null) {
                    LOGGER.debug("🔍 Используем алгоритм signature (для третьих сторон): {} | AuthDate: {}", signature, authDateStr);
                }
            
            // Проверяем время auth_date
            LOGGER.debug("🔍 Проверяем время auth_date");
            if (!validateAuthDate(authDateStr)) {
                LOGGER.warn("❌ Auth date слишком старая: {}", authDateStr);
                return false;
            }
            LOGGER.debug("✅ Auth date валидна");
            
            // Проверяем подпись (поддерживаем оба формата)
                            boolean signatureValid = false;
                // ПРИОРИТЕТ: сначала пробуем hash (рекомендуется для владельцев бота)
                if (hash != null) {
                    LOGGER.debug("🔍 Проверяем подпись hash (для владельцев бота)");
                    signatureValid = validateHash(params, hash, botToken);
                    if (!signatureValid) {
                        LOGGER.warn("❌ Неверная подпись hash для бота '{}'", botName);
                    }
                } else if (signature != null) {
                    LOGGER.debug("🔍 Проверяем подпись signature (для третьих сторон)");
                    signatureValid = validateSignature(params, signature, botToken);
                    if (!signatureValid) {
                        LOGGER.warn("❌ Неверная подпись signature для бота '{}'", botName);
                    }
                }
            
            if (!signatureValid) {
                return false;
            }
            LOGGER.debug("✅ Подпись валидна");
            
            LOGGER.info("✅ InitData валидна для бота '{}'", botName);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка валидации initData для бота '{}': {}", botName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получает токен бота из базы данных
     */
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
    
    /**
     * Парсит строку initData в Map параметров
     */
    private Map<String, String> parseInitData(String initData) {
        return Arrays.stream(initData.split("&"))
                .map(param -> param.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts[1],
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));
    }
    
    /**
     * Проверяет время auth_date (не старше 1 минуты)
     */
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
     * Проверяет HMAC-SHA256 подпись (старый формат с hash)
     */
    private boolean validateHash(Map<String, String> params, String expectedHash, String botToken) {
        LOGGER.info("🔍 НАЧАЛО validateHash - ожидаемый hash: {}", expectedHash);
        try {
            // Создаем строку для подписи (все параметры кроме hash и signature, отсортированные)
            String dataCheckString = params.entrySet().stream()
                    .filter(entry -> !"hash".equals(entry.getKey()) && !"signature".equals(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            
            LOGGER.info("🔍 Data check string для hash: {}", dataCheckString);
            
            // Вычисляем подпись согласно документации Telegram:
            // secret_key = HMAC-SHA256(bot_token, "WebAppData")
            // hash = HMAC-SHA256(data_check_string, secret_key)
            
            // Шаг 1: Создаем секретный ключ
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec webAppDataKeySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(webAppDataKeySpec);
            byte[] secretKey = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
            
            // Шаг 2: Подписываем данные секретным ключом
            mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec dataKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            mac.init(dataKeySpec);
            byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String calculatedHash = bytesToHex(hashBytes);
            
            LOGGER.debug("🔍 Сравнение хешей: ожидаемый={}, вычисленный={}", expectedHash, calculatedHash);
            
            return calculatedHash.equals(expectedHash);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.error("❌ Ошибка вычисления HMAC: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Проверяет новую подпись signature (новый формат Telegram Web Apps)
     * Согласно https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    private boolean validateSignature(Map<String, String> params, String expectedSignature, String botToken) {
        try {
            LOGGER.debug("🔍 Валидация signature для нового формата Telegram Web Apps");
            
            // Создаем строку для подписи (все параметры кроме signature и hash, отсортированные)
            // ВАЖНО: данные должны быть в URL-decoded виде
            String dataCheckString = params.entrySet().stream()
                    .filter(entry -> !"signature".equals(entry.getKey()) && !"hash".equals(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        try {
                            String decodedValue = java.net.URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8);
                            return entry.getKey() + "=" + decodedValue;
                        } catch (Exception e) {
                            // Если декодирование не удалось, используем оригинальное значение
                            return entry.getKey() + "=" + entry.getValue();
                        }
                    })
                    .collect(Collectors.joining("\n"));
            
            LOGGER.debug("🔍 Строка для подписи signature: {}", dataCheckString);
            
            // Алгоритм согласно документации Telegram:
            // 1. secret_key = HMAC-SHA256("WebAppData", bot_token)
            // 2. signature = HMAC-SHA256(data_check_string, secret_key)
            
            Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
            
            // Шаг 1: Создаем секретный ключ
            SecretKeySpec webAppKeySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmacSha256.init(webAppKeySpec);
            byte[] secretKey = hmacSha256.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
            
            LOGGER.debug("🔍 Секретный ключ создан (длина: {} байт)", secretKey.length);
            
            // Шаг 2: Подписываем данные секретным ключом
            hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec dataKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            hmacSha256.init(dataKeySpec);
            byte[] signatureBytes = hmacSha256.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            
            // Signature в Web Apps формате использует URL-safe Base64
            String calculatedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            
            LOGGER.debug("🔍 Сравнение signature: ожидаемый={}, вычисленный={}", expectedSignature, calculatedSignature);
            
            boolean isValid = calculatedSignature.equals(expectedSignature);
            
            if (!isValid) {
                // Пробуем обычный Base64 (не URL-safe)
                LOGGER.debug("🔍 Пробуем обычный Base64 (не URL-safe)");
                String regularBase64Signature = java.util.Base64.getEncoder().encodeToString(signatureBytes);
                LOGGER.debug("🔍 Обычный Base64 signature: {}", regularBase64Signature);
                isValid = regularBase64Signature.equals(expectedSignature);
                
                if (!isValid) {
                    // Пробуем с padding для URL-safe
                    LOGGER.debug("🔍 Пробуем URL-safe Base64 с padding");
                    String urlSafeWithPadding = java.util.Base64.getUrlEncoder().encodeToString(signatureBytes);
                    LOGGER.debug("🔍 URL-safe с padding signature: {}", urlSafeWithPadding);
                    isValid = urlSafeWithPadding.equals(expectedSignature);
                }
            }
            
            if (isValid) {
                LOGGER.debug("✅ Signature валидация успешна!");
            } else {
                LOGGER.warn("❌ Все варианты signature не совпали");
            }
            
            return isValid;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.error("❌ Ошибка вычисления signature: {}", e.getMessage(), e);
            return false;
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
    
    /**
     * Извлекает telegram_id из initData
     */
    public Long extractTelegramId(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            String userStr = params.get("user");
            
            if (userStr != null) {
                // Простой парсинг JSON для извлечения id
                // В реальном проекте лучше использовать JSON парсер
                int idIndex = userStr.indexOf("\"id\":");
                if (idIndex != -1) {
                    int start = userStr.indexOf(":", idIndex) + 1;
                    int end = userStr.indexOf(",", start);
                    if (end == -1) {
                        end = userStr.indexOf("}", start);
                    }
                    if (end != -1) {
                        String idStr = userStr.substring(start, end).trim();
                        return Long.parseLong(idStr);
                    }
                }
            }
            
            LOGGER.warn("❌ Не удалось извлечь telegram_id из initData");
            return null;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка извлечения telegram_id: {}", e.getMessage(), e);
            return null;
        }
    }
}
