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

    @Column(name = "referral_enabled", nullable = false)
    private boolean referralEnabled;

    @Column(name = "referral_referrer_days")
    private Integer referralReferrerDays;

    @Column(name = "referral_referred_days")
    private Integer referralReferredDays;

    @Column(name = "referral_first_payment_only", nullable = false)
    private boolean referralFirstPaymentOnly = true;

    /** Цена в минимальных единицах валюты (копейки); null — не продаётся через бота. */
    @Column(name = "price_amount_minor")
    private Long priceAmountMinor;

    @Column(nullable = false, length = 8)
    private String currency = "RUB";

    /** Дней доступа за одну успешную оплату по этому тарифу. */
    @Column(name = "paid_term_days")
    private Integer paidTermDays;

    @Column(name = "checkout_description", columnDefinition = "TEXT")
    private String checkoutDescription;

    /** Многострочный текст условий/включено в тариф (экран перед оплатой в боте). */
    @Column(name = "detail_description", columnDefinition = "TEXT")
    private String detailDescription;

    /** Для групповых тарифов: инструкция после активации (права бота и т.д.); для персональных — null. */
    @Column(name = "activation_instruction", columnDefinition = "TEXT")
    private String activationInstruction;

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

    public boolean isReferralEnabled() { return referralEnabled; }
    public void setReferralEnabled(boolean referralEnabled) { this.referralEnabled = referralEnabled; }

    public Integer getReferralReferrerDays() { return referralReferrerDays; }
    public void setReferralReferrerDays(Integer referralReferrerDays) { this.referralReferrerDays = referralReferrerDays; }

    public Integer getReferralReferredDays() { return referralReferredDays; }
    public void setReferralReferredDays(Integer referralReferredDays) { this.referralReferredDays = referralReferredDays; }

    public boolean isReferralFirstPaymentOnly() { return referralFirstPaymentOnly; }
    public void setReferralFirstPaymentOnly(boolean referralFirstPaymentOnly) { this.referralFirstPaymentOnly = referralFirstPaymentOnly; }

    public Long getPriceAmountMinor() { return priceAmountMinor; }
    public void setPriceAmountMinor(Long priceAmountMinor) { this.priceAmountMinor = priceAmountMinor; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getPaidTermDays() { return paidTermDays; }
    public void setPaidTermDays(Integer paidTermDays) { this.paidTermDays = paidTermDays; }

    public String getCheckoutDescription() { return checkoutDescription; }
    public void setCheckoutDescription(String checkoutDescription) { this.checkoutDescription = checkoutDescription; }

    public String getDetailDescription() { return detailDescription; }
    public void setDetailDescription(String detailDescription) { this.detailDescription = detailDescription; }

    public String getActivationInstruction() { return activationInstruction; }
    public void setActivationInstruction(String activationInstruction) { this.activationInstruction = activationInstruction; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
