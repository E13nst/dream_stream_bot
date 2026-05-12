package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.PeriodSource;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TrialUsageEntity;
import com.example.dream_stream_bot.model.subscription.TrialUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Доменный сервис управления подписками: создание, выдача периодов,
 * продление, отмена и проверка активности.
 *
 * Срок жизни подписки — кэш {@code expires_at} в {@link SubscriptionEntity}
 * пересчитывается по {@code max(period_ends_at)} из {@link SubscriptionPeriodEntity};
 * тарифы {@link SubscriptionTariffEntity} задают персональный/групповой режим и способ доступа.
 */
@Service
public class SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    public static final int FALLBACK_TRIAL_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final TrialUsageRepository trialUsageRepository;
    private final SubscriptionTariffRepository tariffRepository;
    private final SubscriptionTariffService tariffService;
    private final ReferralBonusService referralBonusService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPeriodRepository periodRepository,
                               TrialUsageRepository trialUsageRepository,
                               SubscriptionTariffRepository tariffRepository,
                               SubscriptionTariffService tariffService,
                               ReferralBonusService referralBonusService) {
        this.subscriptionRepository = subscriptionRepository;
        this.periodRepository = periodRepository;
        this.trialUsageRepository = trialUsageRepository;
        this.tariffRepository = tariffRepository;
        this.tariffService = tariffService;
        this.referralBonusService = referralBonusService;
    }

    /** Личная подписка пользователя на бот. */
    public Optional<SubscriptionEntity> findPersonal(Long botId, Long ownerUserId) {
        return subscriptionRepository.findPersonal(botId, ownerUserId);
    }

    /** Групповая подписка по чату на бот. */
    public Optional<SubscriptionEntity> findGroup(Long botId, Long chatId) {
        return subscriptionRepository.findByBotIdAndScopeChatId(botId, chatId);
    }

    public List<SubscriptionEntity> findAll() {
        return subscriptionRepository.findAll();
    }

    public List<SubscriptionEntity> findOwnedBy(Long ownerUserId) {
        return subscriptionRepository.findByOwnerUserId(ownerUserId);
    }

    public SubscriptionEntity requireById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: id=" + id));
    }

    public Optional<SubscriptionEntity> findById(Long id) {
        return subscriptionRepository.findById(id);
    }

    public List<SubscriptionPeriodEntity> findPeriods(Long subscriptionId) {
        return periodRepository.findBySubscriptionIdOrderByPeriodEndsAtDesc(subscriptionId);
    }

    public boolean isGroupSubscriptionByTariff(Long tariffId) {
        return tariffService.isGroupTariff(tariffId);
    }

    /**
     * Создаёт подписку в статусе {@link SubscriptionStatus#PENDING_CONSENT}.
     * Если уже есть (personal — owner+bot, group — bot+chat) — возвращает существующую (тариф уже зафиксирован).
     */
    @Transactional
    public SubscriptionEntity createOrGet(Long ownerUserId, Long botId, Long tariffId, Long scopeChatId) {
        SubscriptionTariffEntity tariff = tariffService.requireForBot(botId, tariffId);
        if (!Boolean.TRUE.equals(tariff.isActive())) {
            throw new IllegalArgumentException("Тариф неактивен: id=" + tariffId);
        }
        boolean group = tariff.getScope().isGroup();
        if (group && scopeChatId == null) {
            throw new IllegalArgumentException("scopeChatId is required for group tariff " + tariff.getCode());
        }
        if (!group && scopeChatId != null) {
            throw new IllegalArgumentException("scopeChatId must be null for personal tariff");
        }
        Optional<SubscriptionEntity> existing = group
                ? subscriptionRepository.findByBotIdAndScopeChatId(botId, scopeChatId)
                : subscriptionRepository.findPersonal(botId, ownerUserId);
        if (existing.isPresent()) {
            return existing.get();
        }
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setBotId(botId);
        entity.setTariffId(tariffId);
        entity.setScopeChatId(scopeChatId);
        entity.setMaxParticipants(group ? tariff.getMaxParticipants() : null);
        entity.setStatus(SubscriptionStatus.PENDING_CONSENT);
        return subscriptionRepository.save(entity);
    }

    /**
     * Активирует триал. Бросает {@link IllegalStateException}, если триал уже использован по этому тарифу.
     */
    @Transactional
    public SubscriptionEntity activateTrial(SubscriptionEntity subscription, int days, Long grantedByUserId) {
        SubscriptionTariffEntity tariff = tariffService.require(subscription.getTariffId());
        Long scopeKey = subscription.getScopeChatId() != null ? subscription.getScopeChatId() : 0L;
        Optional<TrialUsageEntity> usage = trialUsageRepository.findByTariffIdAndOwnerUserIdAndScopeChatId(
                subscription.getTariffId(), subscription.getOwnerUserId(), scopeKey);
        if (usage.isPresent()) {
            throw new IllegalStateException("Trial already used for tariff=" + tariff.getCode()
                    + " owner=" + subscription.getOwnerUserId() + " scope=" + scopeKey);
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime endsAt = now.plusDays(days);
        addPeriod(subscription, PeriodSource.TRIAL, now, endsAt, grantedByUserId, "Trial " + days + "d");

        TrialUsageEntity usageEntity = new TrialUsageEntity();
        usageEntity.setBotId(subscription.getBotId());
        usageEntity.setTariffId(subscription.getTariffId());
        usageEntity.setOwnerUserId(subscription.getOwnerUserId());
        usageEntity.setScopeChatId(scopeKey);
        usageEntity.setUsedAt(now);
        trialUsageRepository.save(usageEntity);

        subscription.setStatus(SubscriptionStatus.TRIAL);
        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(now);
        }
        refreshExpiresAt(subscription);
        return subscriptionRepository.save(subscription);
    }

    /** Персональный безлимит без периодов (после согласий). */
    @Transactional
    public SubscriptionEntity activateFreeUnlimited(SubscriptionEntity subscription) {
        OffsetDateTime now = OffsetDateTime.now();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(now);
        }
        subscription.setExpiresAt(null);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public SubscriptionEntity grantManual(SubscriptionEntity subscription, int days, Long grantedByUserId, String note) {
        return extend(subscription, PeriodSource.MANUAL_GRANT, days, grantedByUserId, note);
    }

    @Transactional
    public SubscriptionEntity grantPayment(SubscriptionEntity subscription, int days, Long grantedByUserId, String note) {
        return extend(subscription, PeriodSource.PAYMENT, days, grantedByUserId, note);
    }

    @Transactional
    public SubscriptionEntity grantReferralBonus(SubscriptionEntity subscription, int days, Long grantedByUserId, String note) {
        return extend(subscription, PeriodSource.REFERRAL_BONUS, days, grantedByUserId, note);
    }

    @Transactional
    public SubscriptionEntity extendMonths(SubscriptionEntity subscription, int months, PeriodSource source,
                                           Long grantedByUserId, String note) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime base = subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(now)
                ? subscription.getExpiresAt()
                : now;
        OffsetDateTime endsAt = base.plusMonths(months);
        addPeriod(subscription, source, base, endsAt, grantedByUserId, note);

        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(now);
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        refreshExpiresAt(subscription);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public SubscriptionEntity cancel(SubscriptionEntity subscription) {
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        return subscriptionRepository.save(subscription);
    }

    /**
     * Все подписки пользователя как владельца на указанном боте переводятся в {@link SubscriptionStatus#CANCELLED}.
     */
    @Transactional
    public int cancelSubscriptionsOwnedByUserOnBot(Long ownerUserId, Long botId) {
        var list = subscriptionRepository.findByOwnerUserIdAndBotId(ownerUserId, botId);
        int n = 0;
        for (SubscriptionEntity s : list) {
            cancel(s);
            n++;
        }
        return n;
    }

    public boolean isActive(SubscriptionEntity subscription) {
        if (subscription == null) {
            return false;
        }
        Optional<SubscriptionTariffEntity> tariffOpt = tariffRepository.findById(subscription.getTariffId());
        if (tariffOpt.isPresent()
                && tariffOpt.get().getAccessMode() == TariffAccessMode.FREE_UNLIMITED
                && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return true;
        }
        if (!subscription.getStatus().isAccessAllowed()) {
            return false;
        }
        return subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(OffsetDateTime.now());
    }

    @Transactional
    public SubscriptionEntity setMaxParticipants(SubscriptionEntity subscription, Integer max) {
        subscription.setMaxParticipants(max);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public SubscriptionEntity markAwaitingActivation(SubscriptionEntity subscription) {
        subscription.setStatus(SubscriptionStatus.AWAITING_ACTIVATION);
        return subscriptionRepository.save(subscription);
    }

    /** Пересчитать expires_at по периодам + перевести в EXPIRED при необходимости. */
    @Transactional
    public SubscriptionEntity refreshExpiry(SubscriptionEntity subscription) {
        refreshExpiresAt(subscription);
        return subscriptionRepository.save(subscription);
    }

    /**
     * Полное удаление подписки: строка подписки, каскадно периоды/участники/платежи,
     * плюс сброс учёта триала по этому тарифу и владельцу (можно снова выдать триал).
     */
    @Transactional
    public void deleteFullyForAdmin(Long subscriptionId) {
        SubscriptionEntity sub = requireById(subscriptionId);
        Long scopeKey = sub.getScopeChatId() != null ? sub.getScopeChatId() : 0L;
        trialUsageRepository.deleteByTariffIdAndOwnerUserIdAndScopeChatId(
                sub.getTariffId(), sub.getOwnerUserId(), scopeKey);
        subscriptionRepository.delete(sub);
    }

    private SubscriptionEntity extend(SubscriptionEntity subscription, PeriodSource source, int days,
                                      Long grantedByUserId, String note) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime base = subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(now)
                ? subscription.getExpiresAt()
                : now;
        OffsetDateTime endsAt = base.plusDays(days);
        SubscriptionPeriodEntity createdPeriod = addPeriod(subscription, source, base, endsAt, grantedByUserId, note);
        if (source == PeriodSource.PAYMENT) {
            applyReferralBonusOnPayment(subscription, createdPeriod);
        }

        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(now);
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        refreshExpiresAt(subscription);
        return subscriptionRepository.save(subscription);
    }

    private SubscriptionPeriodEntity addPeriod(SubscriptionEntity subscription, PeriodSource source,
                                               OffsetDateTime start, OffsetDateTime end,
                                               Long grantedByUserId, String note) {
        SubscriptionPeriodEntity period = new SubscriptionPeriodEntity();
        period.setSubscriptionId(subscription.getId());
        period.setSource(source);
        period.setPeriodStartedAt(start);
        period.setPeriodEndsAt(end);
        period.setGrantedByUserId(grantedByUserId);
        period.setNote(note);
        SubscriptionPeriodEntity saved = periodRepository.save(period);
        LOGGER.info("➕ Granted period | sub={} | source={} | days={} | ends={}",
                subscription.getId(), source, java.time.Duration.between(start, end).toDays(), end);
        return saved;
    }

    private void applyReferralBonusOnPayment(SubscriptionEntity referredSubscription,
                                             SubscriptionPeriodEntity paymentPeriod) {
        referralBonusService.prepareForPayment(referredSubscription, paymentPeriod).ifPresent(prepared -> {
            if (prepared.referredBonusDays() > 0) {
                grantReferralBonus(prepared.referredSubscription(), prepared.referredBonusDays(), null,
                        "Referral bonus (referred)");
            }
            if (prepared.referrerBonusDays() > 0) {
                grantReferralBonus(prepared.referrerSubscription(), prepared.referrerBonusDays(), null,
                        "Referral bonus (referrer)");
            }
            referralBonusService.markGranted(prepared);
            LOGGER.info("🤝 Referral bonus granted | paymentPeriod={} | referredUser={} | referrerUser={} | referredDays={} | referrerDays={}",
                    prepared.paymentPeriodId(), prepared.referredUserId(), prepared.referrerUserId(),
                    prepared.referredBonusDays(), prepared.referrerBonusDays());
        });
    }

    private void refreshExpiresAt(SubscriptionEntity subscription) {
        SubscriptionTariffEntity tariff = tariffRepository.findById(subscription.getTariffId()).orElse(null);
        if (tariff != null && tariff.getAccessMode() == TariffAccessMode.FREE_UNLIMITED) {
            subscription.setExpiresAt(null);
            return;
        }
        OffsetDateTime max = periodRepository.findMaxEndsAt(subscription.getId()).orElse(null);
        subscription.setExpiresAt(max);
        if (max != null && max.isBefore(OffsetDateTime.now())
                && subscription.getStatus() != SubscriptionStatus.CANCELLED
                && subscription.getStatus() != SubscriptionStatus.BLOCKED_CONSENT
                && subscription.getStatus() != SubscriptionStatus.PENDING_CONSENT
                && subscription.getStatus() != SubscriptionStatus.AWAITING_ACTIVATION) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
        }
    }
}
