package com.example.dream_stream_bot.model.telegram;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Типы ботов, поддерживаемые системой
 */
public enum BotType {
    COPYCAT("copycat"),
    ASSISTANT("assistant");
    
    private final String value;
    
    BotType(String value) {
        this.value = value;
    }
    
    /**
     * Получить строковое значение для хранения в БД
     * Используется для JSON сериализации
     */
    @JsonValue
    public String getValue() {
        return value;
    }
    
    /**
     * Парсинг строки в enum (case-insensitive)
     * Поддерживает как "copycat", так и "cotycat" (опечатка) для обратной совместимости
     * Используется для JSON десериализации
     */
    @JsonCreator
    public static BotType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        String normalized = value.toLowerCase().trim();
        
        // Обработка опечатки "cotycat" -> COPYCAT
        if ("cotycat".equals(normalized)) {
            return COPYCAT;
        }
        
        for (BotType type : BotType.values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown bot type: " + value + ". Supported types: " + 
            String.join(", ", java.util.Arrays.stream(BotType.values())
                .map(BotType::getValue)
                .toArray(String[]::new)));
    }
    
    /**
     * Проверка, является ли строка валидным типом бота
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        try {
            fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

