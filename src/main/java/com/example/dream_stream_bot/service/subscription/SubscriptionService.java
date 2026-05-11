package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.PeriodSource;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionPlan;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
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
 * пересчитывается по {@code max(period_ends_at)} из {@link SubscriptionPeriodEntity}.
 */
@Service
public class SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    public static final int PERSONAL_TRIAL_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final TrialUsageRepository trialUsageRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPeriodRepository periodRepository,
                               TrialUsageRepository trialUsageRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.periodRepository = periodRepository;
        this.trialUsageRepository = trialUsageRepository;
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

    /**
     * Создаёт подписку в статусе {@link SubscriptionStatus#PENDING_CONSENT}.
     * Если подписка уже есть (personal — по owner+bot, group — по bot+chat) — возвращает существующую.
     */
    @Transactional
    public SubscriptionEntity createOrGet(Long ownerUserId, Long botId, SubscriptionPlan plan, Long scopeChatId) {
        if (plan.isGroup() && scopeChatId == null) {
            throw new IllegalArgumentException("scopeChatId is required for group plan " + plan);
        }
        if (!plan.isGroup() && scopeChatId != null) {
            throw new IllegalArgumentException("scopeChatId must be null for personal plan");
        }
        Optional<SubscriptionEntity> existing = plan.isGroup()
                ? subscriptionRepository.findByBotIdAndScopeChatId(botId, scopeChatId)
                : subscriptionRepository.findPersonal(botId, ownerUserId);
        if (existing.isPresent()) {
            return existing.get();
        }
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setBotId(botId);
        entity.setPlan(plan);
        entity.setScopeChatId(scopeChatId);
        entity.setMaxParticipants(plan.getDefaultMaxParticipants());
        entity.setStatus(SubscriptionStatus.PENDING_CONSENT);
        return subscriptionRepository.save(entity);
    }

    /**
     * Активирует триал. Бросает {@link IllegalStateException}, если триал уже использован.
     */
    @Transactional
    public SubscriptionEntity activateTrial(SubscriptionEntity subscription, int days, Long grantedByUserId) {
        Long scopeKey = subscription.getScopeChatId() != null ? subscription.getScopeChatId() : 0L;
        Optional<TrialUsageEntity> usage = trialUsageRepository.findByPlanAndOwnerUserIdAndScopeChatId(
                subscription.getPlan(), subscription.getOwnerUserId(), scopeKey);
        if (usage.isPresent()) {
            throw new IllegalStateException("Trial already used for plan=" + subscription.getPlan()
                    + " owner=" + subscription.getOwnerUserId() + " scope=" + scopeKey);
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime endsAt = now.plusDays(days);
        addPeriod(subscription, PeriodSource.TRIAL, now, endsAt, grantedByUserId, "Trial " + days + "d");

        TrialUsageEntity usageEntity = new TrialUsageEntity();
        usageEntity.setPlan(subscription.getPlan());
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

    /**
     * Продлевает подписку на {@code months} месяцев от max(now, expiresAt).
     * Источник — обычно {@link PeriodSource#PAYMENT} или {@link PeriodSource#MANUAL_GRANT}.
     */
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

    private SubscriptionEntity extend(SubscriptionEntity subscription, PeriodSource source, int days,
                                      Long grantedByUserId, String note) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime base = subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(now)
                ? subscription.getExpiresAt()
                : now;
        OffsetDateTime endsAt = base.plusDays(days);
        addPeriod(subscription, source, base, endsAt, grantedByUserId, note);

        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(now);
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        refreshExpiresAt(subscription);
        return subscriptionRepository.save(subscription);
    }

    private void addPeriod(SubscriptionEntity subscription, PeriodSource source,
                           OffsetDateTime start, OffsetDateTime end,
                           Long grantedByUserId, String note) {
        SubscriptionPeriodEntity period = new SubscriptionPeriodEntity();
        period.setSubscriptionId(subscription.getId());
        period.setSource(source);
        period.setPeriodStartedAt(start);
        period.setPeriodEndsAt(end);
        period.setGrantedByUserId(grantedByUserId);
        period.setNote(note);
        periodRepository.save(period);
        LOGGER.info("➕ Granted period | sub={} | source={} | days={} | ends={}",
                subscription.getId(), source, java.time.Duration.between(start, end).toDays(), end);
    }

    private void refreshExpiresAt(SubscriptionEntity subscription) {
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
