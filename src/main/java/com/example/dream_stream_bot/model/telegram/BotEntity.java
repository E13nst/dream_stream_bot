package com.example.dream_stream_bot.model.telegram;

import com.example.dream_stream_bot.model.agent.AgentConfigEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "bot")
@Data
public class BotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name; // отображаемое имя

    @Column(nullable = false, length = 64)
    private String username; // username в Telegram

    @Column(nullable = false, length = 128)
    private String token;

    @Column(name = "webhook_url", length = 256)
    private String webhookUrl;

    @Column(nullable = false, length = 32)
    private String type; // тип бота

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "bot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<BotKeywordEntity> keywords = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_config_id")
    private AgentConfigEntity agentConfig;

    @Column(name = "miniapp", length = 512)
    private String miniapp; // ссылка на миниприложение Telegram

    /** Переопределение shopId ЮKassa для этого бота; если пусто — из application properties. */
    @Column(name = "yookassa_shop_id", length = 64)
    private String yookassaShopId;

    /** Переопределение секретного ключа ЮKassa; хранится как в token (осторожно в проде). */
    @Column(name = "yookassa_secret_key", length = 512)
    private String yookassaSecretKey;

    @Column(name = "yookassa_receipt_enabled", nullable = false)
    private boolean yookassaReceiptEnabled;

    public java.util.List<String> getBotAliasesList() {
        if (name == null) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(name.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Ключевые слова-триггеры (подстрока в тексте, сравнение без учёта регистра в {@link com.example.dream_stream_bot.bot.AssistantBot}).
     */
    public List<String> getBotTriggersList() {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        return keywords.stream().map(BotKeywordEntity::getKeyword).toList();
    }

    public String getBotName() {
        return name;
    }

    /**
     * Автоматическое обновление updatedAt перед сохранением
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
