package com.example.dream_stream_bot.service.file;

import com.example.dream_stream_bot.dto.StickerCacheDto;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Сервис для скачивания файлов из Telegram Bot API
 */
@Service
public class TelegramFileService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramFileService.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    private static final String TELEGRAM_FILE_URL = "https://api.telegram.org/file/bot";
    private static final long MAX_FILE_SIZE = 512 * 1024; // 512 KB
    
    private final RestTemplate restTemplate;
    private final BotService botService;
    private final ObjectMapper objectMapper;
    private final MimeTypeDetectionService mimeTypeService;
    
    @Autowired
    public TelegramFileService(BotService botService, ObjectMapper objectMapper, 
                              MimeTypeDetectionService mimeTypeService) {
        this.restTemplate = new RestTemplate();
        this.botService = botService;
        this.objectMapper = objectMapper;
        this.mimeTypeService = mimeTypeService;
    }
    
    /**
     * Скачивает файл из Telegram и создает StickerCacheDto
     * 
     * @param fileId идентификатор файла в Telegram
     * @param botName имя бота для получения токена
     * @return StickerCacheDto с данными файла
     * @throws RuntimeException если файл не удалось скачать
     */
    public StickerCacheDto downloadFile(String fileId, String botName) {
        try {
            LOGGER.debug("📥 Начинаем скачивание файла '{}' для бота '{}'", fileId, botName);
            
            // 1. Получаем токен бота
            var botOpt = botService.findByName(botName);
            if (botOpt.isEmpty()) {
                throw new IllegalArgumentException("Бот не найден: " + botName);
            }
            
            String token = botOpt.get().getToken();
            
            // 2. Получаем информацию о файле через getFile
            TelegramFileInfo fileInfo = getFileInfo(fileId, token);
            
            // 3. Валидация размера файла
            if (fileInfo.fileSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                    String.format("Файл слишком большой: %d байт (максимум: %d)", 
                    fileInfo.fileSize(), MAX_FILE_SIZE));
            }
            
            // 4. Скачиваем файл
            byte[] fileData = downloadFileData(fileInfo.filePath(), token);
            
            // 5. Определяем MIME тип
            String mimeType = determineMimeType(fileId, fileInfo.filePath());
            
            // 6. Валидация MIME типа для стикеров
            if (!mimeTypeService.isValidStickerMimeType(mimeType)) {
                LOGGER.warn("⚠️ Неподдерживаемый MIME тип для стикера: {}", mimeType);
            }
            
            // 7. Генерируем имя файла
            String fileName = mimeTypeService.generateFileName(fileId, mimeType);
            
            // 8. Создаем результат
            StickerCacheDto result = StickerCacheDto.create(
                fileId, fileData, mimeType, fileName, fileInfo.filePath());
            
            LOGGER.info("✅ Файл '{}' успешно скачан: {} байт, MIME: {}", 
                    fileId, fileData.length, mimeType);
            
            return result;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при скачивании файла '{}': {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Не удалось скачать файл из Telegram: " + fileId, e);
        }
    }
    
    /**
     * Скачивает файл с использованием бота по умолчанию
     */
    public StickerCacheDto downloadFile(String fileId) {
        return downloadFile(fileId, "StickerGallery");
    }
    
    /**
     * Получает информацию о файле через Telegram Bot API
     */
    private TelegramFileInfo getFileInfo(String fileId, String token) throws Exception {
        String url = TELEGRAM_API_URL + token + "/getFile?file_id=" + fileId;
        
        LOGGER.debug("🌐 Запрос информации о файле: {}", url.replace(token, "***"));
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Неуспешный HTTP ответ: " + response.getStatusCode());
        }
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        
        if (!responseJson.has("ok") || !responseJson.get("ok").asBoolean()) {
            String errorDescription = responseJson.has("description") 
                ? responseJson.get("description").asText() 
                : "Unknown error";
            throw new RuntimeException("Telegram API error: " + errorDescription);
        }
        
        JsonNode result = responseJson.get("result");
        String filePath = result.get("file_path").asText();
        long fileSize = result.has("file_size") ? result.get("file_size").asLong() : 0;
        
        LOGGER.debug("📄 Информация о файле получена: path={}, size={}", filePath, fileSize);
        
        return new TelegramFileInfo(fileId, filePath, fileSize);
    }
    
    /**
     * Скачивает данные файла по file_path
     */
    private byte[] downloadFileData(String filePath, String token) {
        String fileUrl = TELEGRAM_FILE_URL + token + "/" + filePath;
        
        LOGGER.debug("📥 Скачиваем файл: {}", fileUrl.replace(token, "***"));
        
        ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Не удалось скачать файл: " + response.getStatusCode());
        }
        
        byte[] fileData = response.getBody();
        LOGGER.debug("✅ Файл скачан: {} байт", fileData.length);
        
        return fileData;
    }
    
    /**
     * Определяет MIME тип файла
     */
    private String determineMimeType(String fileId, String filePath) {
        // Сначала пробуем по расширению файла
        String mimeByExtension = mimeTypeService.detectMimeTypeByExtension(filePath);
        if (!"application/octet-stream".equals(mimeByExtension)) {
            return mimeByExtension;
        }
        
        // Fallback: определяем по file_id
        return mimeTypeService.detectMimeType(fileId);
    }
    
    /**
     * Валидирует file_id
     */
    public boolean isValidFileId(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            return false;
        }
        
        // Базовая валидация: file_id должен содержать только допустимые символы
        return fileId.matches("^[A-Za-z0-9_-]+$") && fileId.length() >= 10 && fileId.length() <= 100;
    }
    
    /**
     * Record для информации о файле из Telegram
     */
    private record TelegramFileInfo(String fileId, String filePath, long fileSize) {}
}
