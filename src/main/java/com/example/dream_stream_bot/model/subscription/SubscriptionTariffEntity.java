package com.example.dream_stream_bot.model.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "subscription_tariff")
public class SubscriptionTariffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bot_id", nullable = false)
    private Long botId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TariffScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 32)
    private TariffAccessMode accessMode;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "default_personal", nullable = false)
    private boolean defaultPersonal;

    @Column(name = "default_group", nullable = false)
    private boolean defaultGroup;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBotId() { return botId; }
    public void setBotId(Long botId) { this.botId = botId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TariffScope getScope() { return scope; }
    public void setScope(TariffScope scope) { this.scope = scope; }

    public TariffAccessMode getAccessMode() { return accessMode; }
    public void setAccessMode(TariffAccessMode accessMode) { this.accessMode = accessMode; }

    public Integer getTrialDays() { return trialDays; }
    public void setTrialDays(Integer trialDays) { this.trialDays = trialDays; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isDefaultPersonal() { return defaultPersonal; }
    public void setDefaultPersonal(boolean defaultPersonal) { this.defaultPersonal = defaultPersonal; }

    public boolean isDefaultGroup() { return defaultGroup; }
    public void setDefaultGroup(boolean defaultGroup) { this.defaultGroup = defaultGroup; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
