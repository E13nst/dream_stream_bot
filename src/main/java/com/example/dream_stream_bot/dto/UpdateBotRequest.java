package com.example.dream_stream_bot.dto;

import com.example.dream_stream_bot.model.telegram.BotType;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * DTO для обновления существующего бота
 * Все поля опциональные - обновляются только переданные поля
 */
public class UpdateBotRequest {
    
    @Size(max = 64, message = "Имя бота не должно превышать 64 символов")
    private String name;
    
    @Size(max = 64, message = "Username не должен превышать 64 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "Username может содержать только буквы, цифры и подчеркивания")
    private String username;
    
    @Size(max = 128, message = "Токен не должен превышать 128 символов")
    private String token;
    
    private BotType type;
    
    @Size(max = 256, message = "Описание не должно превышать 256 символов")
    private String description;
    
    @Size(max = 256, message = "Триггеры не должны превышать 256 символов")
    private String triggers;
    
    @Size(max = 256, message = "Webhook URL не должен превышать 256 символов")
    @Pattern(regexp = "^$|^https?://.*", message = "Webhook URL должен быть валидным HTTP/HTTPS URL или пустым", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String webhookUrl;
    
    @Size(max = 512, message = "Miniapp URL не должен превышать 512 символов")
    @Pattern(regexp = "^$|^https?://.*", message = "Miniapp URL должен быть валидным HTTP/HTTPS URL или пустым", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String miniapp;
    
    private String prompt; // TEXT поле, без ограничений по длине
    
    @Min(value = 1, message = "Окно памяти должно быть не менее 1")
    @Max(value = 10000, message = "Окно памяти должно быть не более 10000")
    private Integer memWindow;
    
    private Boolean isActive;
    
    // Конструкторы
    public UpdateBotRequest() {}
    
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
    
    public String getTriggers() {
        return triggers;
    }
    
    public void setTriggers(String triggers) {
        this.triggers = triggers;
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
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public Integer getMemWindow() {
        return memWindow;
    }
    
    public void setMemWindow(Integer memWindow) {
        this.memWindow = memWindow;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

