package com.example.dream_stream_bot.dto;

import com.example.dream_stream_bot.model.telegram.BotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;

import java.util.List;

/**
 * DTO для создания нового бота
 */
public class CreateBotRequest {
    
    @NotBlank(message = "Имя бота обязательно для заполнения")
    @Size(max = 64, message = "Имя бота не должно превышать 64 символов")
    private String name;
    
    @NotBlank(message = "Username бота обязателен для заполнения")
    @Size(max = 64, message = "Username не должен превышать 64 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username может содержать только буквы, цифры и подчеркивания")
    private String username;
    
    @NotBlank(message = "Токен бота обязателен для заполнения")
    @Size(max = 128, message = "Токен не должен превышать 128 символов")
    private String token;
    
    @NotNull(message = "Тип бота обязателен для заполнения")
    private BotType type;
    
    @Size(max = 2048, message = "Описание не должно превышать 2048 символов")
    private String description;
    
    /**
     * Начальный список ключевых слов-триггеров (опционально). Каждое слово — отдельная запись в БД.
     */
    @Valid
    private List<@Size(max = 256, message = "Каждое ключевое слово не должно превышать 256 символов") String> keywords;
    
    @Size(max = 256, message = "Webhook URL не должен превышать 256 символов")
    @Pattern(regexp = "^$|^https?://.*", message = "Webhook URL должен быть валидным HTTP/HTTPS URL или пустым", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String webhookUrl;
    
    @Size(max = 512, message = "Miniapp URL не должен превышать 512 символов")
    @Pattern(regexp = "^$|^https?://.*", message = "Miniapp URL должен быть валидным HTTP/HTTPS URL или пустым", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String miniapp;

    /** Required for {@link BotType#ASSISTANT}; ignored for non-AI bots. */
    private Long agentConfigId;

    private Boolean isActive;
    
    // Конструкторы
    public CreateBotRequest() {}
    
    // Геттеры и сеттеры
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public BotType getType() {
        return type;
    }
    
    public void setType(BotType type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public String getMiniapp() {
        return miniapp;
    }
    
    public void setMiniapp(String miniapp) {
        this.miniapp = miniapp;
    }

    public Long getAgentConfigId() {
        return agentConfigId;
    }

    public void setAgentConfigId(Long agentConfigId) {
        this.agentConfigId = agentConfigId;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

