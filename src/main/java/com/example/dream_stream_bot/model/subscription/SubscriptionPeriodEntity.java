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
@Table(name = "subscription_period")
public class SubscriptionPeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PeriodSource source;

    @Column(name = "period_started_at", nullable = false)
    private OffsetDateTime periodStartedAt;

    @Column(name = "period_ends_at", nullable = false)
    private OffsetDateTime periodEndsAt;

    @Column(name = "granted_by_user_id")
    private Long grantedByUserId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public PeriodSource getSource() { return source; }
    public void setSource(PeriodSource source) { this.source = source; }

    public OffsetDateTime getPeriodStartedAt() { return periodStartedAt; }
    public void setPeriodStartedAt(OffsetDateTime periodStartedAt) { this.periodStartedAt = periodStartedAt; }

    public OffsetDateTime getPeriodEndsAt() { return periodEndsAt; }
    public void setPeriodEndsAt(OffsetDateTime periodEndsAt) { this.periodEndsAt = periodEndsAt; }

    public Long getGrantedByUserId() { return grantedByUserId; }
    public void setGrantedByUserId(Long grantedByUserId) { this.grantedByUserId = grantedByUserId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
