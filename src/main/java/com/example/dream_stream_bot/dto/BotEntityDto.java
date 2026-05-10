package com.example.dream_stream_bot.dto;

import com.example.dream_stream_bot.model.telegram.BotEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST DTO for bots. Prompt and memory window come from the linked {@code agent_config} only.
 */
public class BotEntityDto {

    private Long id;
    private String name;
    private String username;
    private String token;
    private String webhookUrl;
    private String type;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
    private List<String> keywords = new ArrayList<>();
    private String miniapp;

    private Long agentConfigId;
    /** Denormalized from agent for convenience (single source of truth is agent_config). */
    private String systemPrompt;
    private Integer memWindow;

    public BotEntityDto() {
    }

    public BotEntityDto(Long id, String name, String username, String token,
                        String webhookUrl, String type, Boolean isActive, LocalDateTime createdAt,
                        LocalDateTime updatedAt, String description, List<String> keywords,
                        String miniapp, Long agentConfigId, String systemPrompt, Integer memWindow) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.token = token;
        this.webhookUrl = webhookUrl;
        this.type = type;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.description = description;
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        this.miniapp = miniapp;
        this.agentConfigId = agentConfigId;
        this.systemPrompt = systemPrompt;
        this.memWindow = memWindow;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
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

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Integer getMemWindow() {
        return memWindow;
    }

    public void setMemWindow(Integer memWindow) {
        this.memWindow = memWindow;
    }

    /**
     * Token не возвращается в API ответах для безопасности.
     */
    public static BotEntityDto fromEntity(BotEntity entity) {
        if (entity == null) {
            return null;
        }
        Long agentId = null;
        String sysPrompt = null;
        Integer mw = null;
        if (entity.getAgentConfig() != null) {
            var ac = entity.getAgentConfig();
            agentId = ac.getId();
            sysPrompt = ac.getSystemPrompt();
            mw = ac.getMemWindow();
        }
        return new BotEntityDto(
                entity.getId(),
                entity.getName(),
                entity.getUsername(),
                null,
                entity.getWebhookUrl(),
                entity.getType(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDescription(),
                entity.getBotTriggersList(),
                entity.getMiniapp(),
                agentId,
                sysPrompt,
                mw
        );
    }

    public List<String> getBotAliasesList() {
        if (name == null) {
            return List.of();
        }
        return List.of(name.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    public String toString() {
        return "BotEntityDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", type='" + type + '\'' +
                ", isActive=" + isActive +
                ", agentConfigId=" + agentConfigId +
                ", miniapp='" + miniapp + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
