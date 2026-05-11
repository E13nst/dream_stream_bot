package com.example.dream_stream_bot.model.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "trial_usage")
public class TrialUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bot_id", nullable = false)
    private Long botId;

    @Column(name = "tariff_id", nullable = false)
    private Long tariffId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    /** Для PERSONAL — 0, для GROUP — telegram chat_id. */
    @Column(name = "scope_chat_id", nullable = false)
    private Long scopeChatId = 0L;

    @Column(name = "used_at", nullable = false)
    private OffsetDateTime usedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBotId() { return botId; }
    public void setBotId(Long botId) { this.botId = botId; }

    public Long getTariffId() { return tariffId; }
    public void setTariffId(Long tariffId) { this.tariffId = tariffId; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public Long getScopeChatId() { return scopeChatId; }
    public void setScopeChatId(Long scopeChatId) { this.scopeChatId = scopeChatId == null ? 0L : scopeChatId; }

    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
}
