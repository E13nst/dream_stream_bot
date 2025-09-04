package com.example.dream_stream_bot.dto;

import com.example.dream_stream_bot.model.user.UserEntity;
import java.time.OffsetDateTime;

/**
 * DTO для пользователя
 */
public class UserDto {
    
    private Long id;
    private Long telegramId;
    private String username;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Long artBalance;
    private String role;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Конструкторы
    public UserDto() {}
    
    public UserDto(Long id, Long telegramId, String username, String firstName, String lastName, 
                   String avatarUrl, Long artBalance, String role, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.telegramId = telegramId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
        this.artBalance = artBalance;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Создает DTO из Entity
     */
    public static UserDto fromEntity(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new UserDto(
                entity.getId(),
                entity.getTelegramId(),
                entity.getUsername(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getAvatarUrl(),
                entity.getArtBalance(),
                entity.getRole().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
    
    /**
     * Создает Entity из DTO
     */
    public UserEntity toEntity() {
        UserEntity entity = new UserEntity();
        entity.setId(this.id);
        entity.setTelegramId(this.telegramId);
        entity.setUsername(this.username);
        entity.setFirstName(this.firstName);
        entity.setLastName(this.lastName);
        entity.setAvatarUrl(this.avatarUrl);
        entity.setArtBalance(this.artBalance);
        if (this.role != null) {
            entity.setRole(UserEntity.UserRole.valueOf(this.role));
        }
        entity.setCreatedAt(this.createdAt);
        entity.setUpdatedAt(this.updatedAt);
        return entity;
    }
    
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public Long getArtBalance() { return artBalance; }
    public void setArtBalance(Long artBalance) { this.artBalance = artBalance; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", telegramId=" + telegramId +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role='" + role + '\'' +
                ", artBalance=" + artBalance +
                '}';
    }
}
