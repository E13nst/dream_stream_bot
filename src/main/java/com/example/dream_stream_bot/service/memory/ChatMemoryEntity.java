package com.example.dream_stream_bot.service.memory;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_memory")
public class ChatMemoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "message_index", nullable = false)
    private Integer messageIndex;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "telegram_message_id")
    private Integer telegramMessageId;

    @Column(name = "message_thread_id")
    private Integer messageThreadId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public Integer getMessageIndex() { return messageIndex; }
    public void setMessageIndex(Integer messageIndex) { this.messageIndex = messageIndex; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getTelegramMessageId() { return telegramMessageId; }
    public void setTelegramMessageId(Integer telegramMessageId) { this.telegramMessageId = telegramMessageId; }

    public Integer getMessageThreadId() { return messageThreadId; }
    public void setMessageThreadId(Integer messageThreadId) { this.messageThreadId = messageThreadId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 