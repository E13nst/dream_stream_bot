package com.example.dream_stream_bot.model.privacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "bot_user_erasure")
public class BotUserErasureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bot_id", nullable = false)
    private Long botId;

    @Column(name = "telegram_id_hash", nullable = false, length = 64)
    private String telegramIdHash;

    @Column(name = "erased_at", nullable = false)
    private OffsetDateTime erasedAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBotId() {
        return botId;
    }

    public void setBotId(Long botId) {
        this.botId = botId;
    }

    public String getTelegramIdHash() {
        return telegramIdHash;
    }

    public void setTelegramIdHash(String telegramIdHash) {
        this.telegramIdHash = telegramIdHash;
    }

    public OffsetDateTime getErasedAt() {
        return erasedAt;
    }

    public void setErasedAt(OffsetDateTime erasedAt) {
        this.erasedAt = erasedAt;
    }
}
