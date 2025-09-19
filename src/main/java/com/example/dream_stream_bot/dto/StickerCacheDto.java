package com.example.dream_stream_bot.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * DTO для кэширования стикеров в Redis
 */
@Data
@NoArgsConstructor
public class StickerCacheDto {
    
    /**
     * Идентификатор файла в Telegram
     */
    private String fileId;
    
    /**
     * Данные файла в base64
     */
    private String fileData;
    
    /**
     * MIME тип файла
     */
    private String mimeType;
    
    /**
     * Имя файла
     */
    private String fileName;
    
    /**
     * Размер файла в байтах
     */
    private long fileSize;
    
    /**
     * Путь к файлу в Telegram (для отладки)
     */
    private String telegramFilePath;
    
    /**
     * Время последнего обновления
     */
    private LocalDateTime lastUpdated;
    
    @JsonCreator
    public StickerCacheDto(
            @JsonProperty("fileId") String fileId,
            @JsonProperty("fileData") String fileData,
            @JsonProperty("mimeType") String mimeType,
            @JsonProperty("fileName") String fileName,
            @JsonProperty("fileSize") long fileSize,
            @JsonProperty("telegramFilePath") String telegramFilePath,
            @JsonProperty("lastUpdated") LocalDateTime lastUpdated) {
        this.fileId = fileId;
        this.fileData = fileData;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.telegramFilePath = telegramFilePath;
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Создает StickerCacheDto из данных файла
     */
    public static StickerCacheDto create(String fileId, byte[] fileBytes, String mimeType, 
                                       String fileName, String telegramFilePath) {
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        return new StickerCacheDto(
                fileId,
                base64Data,
                mimeType,
                fileName,
                fileBytes.length,
                telegramFilePath,
                LocalDateTime.now()
        );
    }
    
    /**
     * Возвращает данные файла как byte array
     */
    @JsonIgnore
    public byte[] getFileBytes() {
        if (fileData == null) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(fileData);
    }
    
    /**
     * Проверяет, не устарел ли кэш (старше 7 дней)
     */
    @JsonIgnore
    public boolean isExpired() {
        if (lastUpdated == null) {
            return true;
        }
        return lastUpdated.plusDays(7).isBefore(LocalDateTime.now());
    }
    
    @Override
    public String toString() {
        return String.format("StickerCache{fileId='%s', mimeType='%s', size=%d, updated=%s}", 
                fileId, mimeType, fileSize, lastUpdated);
    }
}
