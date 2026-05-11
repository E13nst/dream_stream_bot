package com.example.dream_stream_bot.model.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "referral_bonus_grant")
public class ReferralBonusGrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bot_id", nullable = false)
    private Long botId;

    @Column(name = "tariff_id", nullable = false)
    private Long tariffId;

    @Column(name = "payment_period_id", nullable = false)
    private Long paymentPeriodId;

    @Column(name = "referred_user_id", nullable = false)
    private Long referredUserId;

    @Column(name = "referrer_user_id", nullable = false)
    private Long referrerUserId;

    @Column(name = "referred_subscription_id", nullable = false)
    private Long referredSubscriptionId;

    @Column(name = "referrer_subscription_id", nullable = false)
    private Long referrerSubscriptionId;

    @Column(name = "referred_bonus_days", nullable = false)
    private int referredBonusDays;

    @Column(name = "referrer_bonus_days", nullable = false)
    private int referrerBonusDays;

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBotId() { return botId; }
    public void setBotId(Long botId) { this.botId = botId; }

    public Long getTariffId() { return tariffId; }
    public void setTariffId(Long tariffId) { this.tariffId = tariffId; }

    public Long getPaymentPeriodId() { return paymentPeriodId; }
    public void setPaymentPeriodId(Long paymentPeriodId) { this.paymentPeriodId = paymentPeriodId; }

    public Long getReferredUserId() { return referredUserId; }
    public void setReferredUserId(Long referredUserId) { this.referredUserId = referredUserId; }

    public Long getReferrerUserId() { return referrerUserId; }
    public void setReferrerUserId(Long referrerUserId) { this.referrerUserId = referrerUserId; }

    public Long getReferredSubscriptionId() { return referredSubscriptionId; }
    public void setReferredSubscriptionId(Long referredSubscriptionId) { this.referredSubscriptionId = referredSubscriptionId; }

    public Long getReferrerSubscriptionId() { return referrerSubscriptionId; }
    public void setReferrerSubscriptionId(Long referrerSubscriptionId) { this.referrerSubscriptionId = referrerSubscriptionId; }

    public int getReferredBonusDays() { return referredBonusDays; }
    public void setReferredBonusDays(int referredBonusDays) { this.referredBonusDays = referredBonusDays; }

    public int getReferrerBonusDays() { return referrerBonusDays; }
    public void setReferrerBonusDays(int referrerBonusDays) { this.referrerBonusDays = referrerBonusDays; }

    public OffsetDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(OffsetDateTime grantedAt) { this.grantedAt = grantedAt; }
}
