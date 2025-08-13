package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.StickerPack;
import com.example.dream_stream_bot.service.file.FileDownloadService;
import com.example.dream_stream_bot.service.image.ImageOptimizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Отрефакторенный StickerService - упрощенный и модульный
 */
@Service
public class StickerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerService.class);
    
    @Autowired
    private ImageOptimizationService imageOptimizationService;
    
    @Autowired
    private TelegramApiService telegramApiService;
    
    @Autowired
    private FileDownloadService fileDownloadService;
    
    @Autowired
    private StickerPackService stickerPackService;
    
    /**
     * Основной метод обработки изображений от пользователя
     */
    public SendMessage handleImageMessage(Message message, TelegramLongPollingBot bot, String stickerPackName) {
        try {
            LOGGER.info("📨 Получено сообщение | Тип: {} | ChatId: {} | StickerPackName: {}", 
                    message.hasPhoto() ? "PHOTO" : message.hasDocument() ? "DOCUMENT" : "OTHER", 
                    message.getChatId(), stickerPackName);
            
            if (message.hasPhoto()) {
                return processPhoto(message, bot, stickerPackName);
            } else if (message.hasDocument()) {
                return processDocument(message, bot, stickerPackName);
            } else {
                return SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("📸 Пожалуйста, отправьте изображение (фото или документ) для создания стикера.")
                        .build();
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обработке изображения: {}", e.getMessage(), e);
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Произошла ошибка при обработке изображения. Попробуйте еще раз.")
                    .build();
        }
    }
    
    /**
     * Обработка фотографий
     */
    private SendMessage processPhoto(Message message, TelegramLongPollingBot bot, String stickerPackName) throws Exception {
        List<PhotoSize> photos = message.getPhoto();
        // Берем самое большое фото
        PhotoSize photo = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(photos.get(photos.size() - 1));
        
        LOGGER.info("📸 Обрабатываем фото | FileId: {} | Size: {} bytes", 
                photo.getFileId(), photo.getFileSize());
        
        // Получаем информацию о файле
        GetFile getFile = new GetFile();
        getFile.setFileId(photo.getFileId());
        File file = bot.execute(getFile);
        
        if (file == null || file.getFilePath() == null) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Не удалось получить файл изображения.")
                    .build();
        }
        
        return processImageFile(file, message.getChatId(), bot, "фото", stickerPackName);
    }
    
    /**
     * Обработка документов
     */
    private SendMessage processDocument(Message message, TelegramLongPollingBot bot, String stickerPackName) throws Exception {
        Document document = message.getDocument();
        
        // Проверяем, что это изображение
        String mimeType = document.getMimeType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Пожалуйста, отправьте файл изображения (JPG, PNG, GIF).")
                    .build();
        }
        
        // Получаем информацию о файле
        GetFile getFile = new GetFile();
        getFile.setFileId(document.getFileId());
        File file = bot.execute(getFile);
        
        if (file == null || file.getFilePath() == null) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Не удалось получить файл изображения.")
                    .build();
        }
        
        return processImageFile(file, message.getChatId(), bot, "документ", stickerPackName);
    }
    
    /**
     * Обработка файла изображения
     */
    private SendMessage processImageFile(File file, Long chatId, TelegramLongPollingBot bot, String fileType, String stickerPackName) {
        Path downloadedFile = null;
        Path optimizedFile = null;
        
        try {
            // Настраиваем API сервис
            telegramApiService.setBotToken(bot.getBotToken());
            
            // Скачиваем файл
            downloadedFile = fileDownloadService.downloadTelegramFile(file, bot.getBotToken());
            
            // Оптимизируем изображение
            optimizedFile = imageOptimizationService.optimizeImageForSticker(downloadedFile);
            
            // Создаем или добавляем к стикерпаку
            StickerResult result = createOrAddToStickerPack(chatId, optimizedFile, stickerPackName);
            
            if (result.isSuccess()) {
                LOGGER.info("✅ Стикер создан успешно!");
                
                // Отправляем оптимизированное изображение
                sendOptimizedImage(chatId, bot, optimizedFile, result);
                
                // Пытаемся отправить созданный стикер
                tryToSendCreatedSticker(chatId, bot, result);
                
                return null; // Изображение уже отправлено
                
            } else {
                LOGGER.warn("⚠️ Не удалось создать стикер автоматически, отправляем оптимизированное изображение");
                sendOptimizedImage(chatId, bot, optimizedFile, null);
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обработке файла {}: {}", fileType, e.getMessage(), e);
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Произошла ошибка при создании стикера. Попробуйте еще раз.")
                    .build();
        } finally {
            // Очищаем временные файлы
            fileDownloadService.cleanupTempFile(downloadedFile);
            fileDownloadService.cleanupTempFile(optimizedFile);
        }
    }
    
    /**
     * Создает новый стикерпак или добавляет к существующему
     */
    private StickerResult createOrAddToStickerPack(Long chatId, Path optimizedFile, String stickerPackName) {
        try {
            if (stickerPackName == null || stickerPackName.isEmpty()) {
                return StickerResult.failure("Имя стикерпака не указано");
            }
            
            // Проверяем, существует ли стикерпак в БД
            StickerPack stickerPack = stickerPackService.findByName(stickerPackName);
            if (stickerPack == null) {
                return StickerResult.failure("Стикерпак не найден в базе данных: " + stickerPackName);
            }
            
            // Проверяем, существует ли стикерпак в Telegram
            TelegramApiService.StickerSetInfo telegramInfo = telegramApiService.getStickerSetInfo(stickerPackName);
            
            if (telegramInfo != null && telegramInfo.exists()) {
                // Добавляем к существующему стикерпаку
                LOGGER.info("➕ Добавляем стикер к существующему стикерпаку: {}", stickerPackName);
                boolean added = telegramApiService.addStickerToSet(chatId, optimizedFile.toFile(), stickerPackName, "🎯");
                if (added) {
                    return StickerResult.success(stickerPackName, stickerPack.getTitle(), false, telegramInfo.getStickerCount());
                } else {
                    return StickerResult.failure("Не удалось добавить стикер к существующему стикерпаку");
                }
            } else {
                // Создаем новый стикерпак в Telegram
                LOGGER.info("📦 Создаем новый стикерпак в Telegram: {}", stickerPackName);
                boolean created = telegramApiService.createNewStickerSet(chatId, optimizedFile.toFile(), stickerPackName, stickerPack.getTitle(), "🎯");
                if (created) {
                    return StickerResult.success(stickerPackName, stickerPack.getTitle(), true, 0);
                } else {
                    return StickerResult.failure("Не удалось создать стикерпак в Telegram");
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании/добавлении стикера: {}", e.getMessage(), e);
            return StickerResult.failure("Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Отправляет оптимизированное изображение пользователю
     */
    private void sendOptimizedImage(Long chatId, TelegramLongPollingBot bot, Path optimizedFile, StickerResult result) {
        try {
            String caption = result != null && result.isSuccess() ? 
                "🎉 **Стикер создан успешно!**\n\n" +
                "📦 **Информация о стикерпаке:**\n" +
                "• Название: " + result.getStickerSetName() + "\n" +
                "• Заголовок: " + result.getStickerSetTitle() + "\n" +
                "• Тип: " + (result.isNewSet() ? "Новый стикерпак" : "Добавлен к существующему") :
                "📸 Изображение оптимизировано для стикера";
            
            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(optimizedFile.toFile()))
                    .caption(caption)
                    .parseMode("Markdown")
                    .build();
            
            LOGGER.info("🚀 Отправляем изображение в чат: {}", chatId);
            bot.execute(sendPhoto);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при отправке изображения: {}", e.getMessage());
        }
    }
    
    /**
     * Пытается отправить созданный стикер пользователю
     */
    private void tryToSendCreatedSticker(Long chatId, TelegramLongPollingBot bot, StickerResult result) {
        try {
            if (!result.isSuccess()) return;
            
            String stickerSetName = result.getStickerSetName();
            int stickerIndex = result.getStickerIndex();
            
            LOGGER.info("🎯 Отправляем созданный стикер пользователю из стикерпака: {} | Индекс: {}", 
                    stickerSetName, stickerIndex);
            
            // Получаем file_id стикера
            String stickerFileId = telegramApiService.getStickerFileId(stickerSetName, stickerIndex);
            
            if (stickerFileId != null) {
                // Отправляем стикер
                SendSticker sendSticker = SendSticker.builder()
                        .chatId(chatId)
                        .sticker(new InputFile(stickerFileId))
                        .build();
                
                bot.execute(sendSticker);
                LOGGER.info("✅ Стикер успешно отправлен пользователю");
                
            } else {
                LOGGER.warn("⚠️ Не удалось получить file_id стикера из стикерпака: {}", stickerSetName);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при отправке стикера: {}", e.getMessage());
        }
    }
    
    /**
     * Результат создания стикера
     */
    private static class StickerResult {
        private final boolean success;
        private final String stickerSetName;
        private final String stickerSetTitle;
        private final boolean isNewSet;
        private final int stickerIndex;
        private final String errorMessage;
        
        private StickerResult(boolean success, String stickerSetName, String stickerSetTitle, 
                             boolean isNewSet, int stickerIndex, String errorMessage) {
            this.success = success;
            this.stickerSetName = stickerSetName;
            this.stickerSetTitle = stickerSetTitle;
            this.isNewSet = isNewSet;
            this.stickerIndex = stickerIndex;
            this.errorMessage = errorMessage;
        }
        
        public static StickerResult success(String name, String title, boolean isNew, int index) {
            return new StickerResult(true, name, title, isNew, index, null);
        }
        
        public static StickerResult failure(String error) {
            return new StickerResult(false, null, null, false, 0, error);
        }
        
        public boolean isSuccess() { return success; }
        public String getStickerSetName() { return stickerSetName; }
        public String getStickerSetTitle() { return stickerSetTitle; }
        public boolean isNewSet() { return isNewSet; }
        public int getStickerIndex() { return stickerIndex; }
        public String getErrorMessage() { return errorMessage; }
    }
}