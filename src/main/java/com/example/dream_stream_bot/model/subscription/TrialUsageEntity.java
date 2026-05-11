package com.example.dream_stream_bot.model.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubscriptionPlan plan;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    /** Для PERSONAL — 0, для GROUP — telegram chat_id. */
    @Column(name = "scope_chat_id", nullable = false)
    private Long scopeChatId = 0L;

    @Column(name = "used_at", nullable = false)
    private OffsetDateTime usedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public Long getScopeChatId() { return scopeChatId; }
    public void setScopeChatId(Long scopeChatId) { this.scopeChatId = scopeChatId == null ? 0L : scopeChatId; }

    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
}
