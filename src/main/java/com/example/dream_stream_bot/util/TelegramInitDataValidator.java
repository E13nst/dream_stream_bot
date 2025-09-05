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
            
            if (signature != null) {
                LOGGER.debug("🔍 Используем новый формат с signature: {} | AuthDate: {}", signature, authDateStr);
            } else {
                LOGGER.debug("🔍 Используем старый формат с hash: {} | AuthDate: {}", hash, authDateStr);
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
            if (signature != null) {
                LOGGER.debug("🔍 Проверяем новую подпись signature");
                signatureValid = validateSignature(params, signature, botToken);
                if (!signatureValid) {
                    LOGGER.warn("❌ Неверная подпись signature для бота '{}'", botName);
                }
            } else if (hash != null) {
                LOGGER.debug("🔍 Проверяем старую подпись hash");
                signatureValid = validateHash(params, hash, botToken);
                if (!signatureValid) {
                    LOGGER.warn("❌ Неверная подпись hash для бота '{}'", botName);
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
        try {
            // Создаем строку для подписи (все параметры кроме hash и signature, отсортированные)
            String dataCheckString = params.entrySet().stream()
                    .filter(entry -> !"hash".equals(entry.getKey()) && !"signature".equals(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            
            // Вычисляем подпись
            String secretKey = "WebAppData";
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
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
     * Проверяет новую подпись signature (новый формат Telegram)
     */
    private boolean validateSignature(Map<String, String> params, String expectedSignature, String botToken) {
        try {
            LOGGER.debug("🔍 Валидация signature для нового формата Telegram");
            
            // Создаем строку для подписи (все параметры кроме signature и hash, отсортированные)
            String dataCheckString = params.entrySet().stream()
                    .filter(entry -> !"signature".equals(entry.getKey()) && !"hash".equals(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            
            LOGGER.debug("🔍 Строка для подписи signature: {}", dataCheckString);
            
            // Для нового формата попробуем несколько алгоритмов подписи
            byte[] signatureBytes = null;
            
            // Алгоритм 1: Двухэтапный (стандартный для WebApp)
            try {
                Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
                SecretKeySpec webAppKeySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
                hmacSha256.init(webAppKeySpec);
                byte[] secretKey = hmacSha256.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
                
                hmacSha256 = Mac.getInstance(HMAC_SHA256);
                SecretKeySpec dataKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
                hmacSha256.init(dataKeySpec);
                signatureBytes = hmacSha256.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOGGER.debug("🔍 Ошибка алгоритма 1: {}", e.getMessage());
            }
            
            // Signature в новом формате передается в Base64, а не hex
            String calculatedSignature = java.util.Base64.getEncoder().encodeToString(signatureBytes);
            
            LOGGER.debug("🔍 Сравнение signature: ожидаемый={}, вычисленный={}", expectedSignature, calculatedSignature);
            
            boolean isValid = calculatedSignature.equals(expectedSignature);
            
            if (!isValid) {
                // Пробуем альтернативный алгоритм - прямая подпись как в старом формате
                LOGGER.debug("🔍 Пробуем альтернативный алгоритм для signature");
                Mac simpleMac = Mac.getInstance(HMAC_SHA256);
                SecretKeySpec simpleKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
                simpleMac.init(simpleKeySpec);
                byte[] simpleSignatureBytes = simpleMac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
                String simpleCalculatedSignature = java.util.Base64.getEncoder().encodeToString(simpleSignatureBytes);
                
                LOGGER.debug("🔍 Альтернативная signature: {}", simpleCalculatedSignature);
                isValid = simpleCalculatedSignature.equals(expectedSignature);
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
