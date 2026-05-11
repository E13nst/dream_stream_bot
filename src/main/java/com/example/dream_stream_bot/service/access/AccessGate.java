package com.example.dream_stream_bot.service.access;

import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.subscription.OwnerParticipantLimitNotifier;
import com.example.dream_stream_bot.service.subscription.SubscriptionParticipantService;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Единая точка проверки доступа сообщения к {@code MessageHandlerService}.
 */
@Service
public class AccessGate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessGate.class);
    private static final ZoneId MOSKOW = ZoneId.of("Europe/Moscow");

    private static final String STUB_NO_SUB_PERSONAL =
            "🔒 Подписка не активирована. Нажмите /start, чтобы пройти онбординг и активировать триал.";
    private static final String STUB_NO_SUB_GROUP =
            "🔒 У этой группы нет активной подписки. Владельцу группы — напишите /start в личке бота.";
    private static final String STUB_PENDING =
            "📝 Завершите онбординг: нажмите /start в личке бота и примите соглашения.";
    private static final String STUB_EXPIRED =
            "⏰ Срок подписки истёк. Чтобы продолжить, продлите её — /subscriptions.";
    private static final String STUB_CANCELLED =
            "🚫 Подписка отменена.";
    private static final String STUB_BLOCKED_CONSENT =
            "📝 Изменились условия использования. Подтвердите новую версию документа, чтобы продолжить.";
    private static final String STUB_LIMIT_HARD =
            "👥 Превышен лимит активных участников группы в этом месяце. Обратитесь к владельцу подписки.";
    private static final String STUB_AWAITING_ACTIVATION =
            "⏳ Групповая подписка создана, но ещё не активирована администратором. Владелец получит уведомление после вашей оплаты/активации.";

    private final SubscriptionService subscriptionService;
    private final SubscriptionParticipantService participantService;
    private final UserService userService;
    private final GroupTriggerMatcher triggerMatcher;
    private final ConsentService consentService;
    private final OwnerParticipantLimitNotifier ownerParticipantLimitNotifier;
    private final SubscriptionTariffRepository subscriptionTariffRepository;

    public AccessGate(SubscriptionService subscriptionService,
                      SubscriptionParticipantService participantService,
                      UserService userService,
                      GroupTriggerMatcher triggerMatcher,
                      ConsentService consentService,
                      OwnerParticipantLimitNotifier ownerParticipantLimitNotifier,
                      SubscriptionTariffRepository subscriptionTariffRepository) {
        this.subscriptionService = subscriptionService;
        this.participantService = participantService;
        this.userService = userService;
        this.triggerMatcher = triggerMatcher;
        this.consentService = consentService;
        this.ownerParticipantLimitNotifier = ownerParticipantLimitNotifier;
        this.subscriptionTariffRepository = subscriptionTariffRepository;
    }

    public AccessDecision evaluate(BotEntity bot, Message message, ChatScope scope, String botUsername) {
        if (message == null || bot == null) {
            return AccessDecision.deny(AccessReason.UNSUPPORTED_CHAT);
        }
        if (scope == ChatScope.CHANNEL || scope == ChatScope.UNKNOWN) {
            return AccessDecision.deny(AccessReason.UNSUPPORTED_CHAT);
        }

        if (scope == ChatScope.PRIVATE) {
            return evaluatePrivate(bot, message);
        }
        if (scope.isGroupLike()) {
            return evaluateGroup(bot, message, botUsername);
        }
        return AccessDecision.deny(AccessReason.UNSUPPORTED_CHAT);
    }

    private AccessDecision evaluatePrivate(BotEntity bot, Message message) {
        Long telegramUserId = message.getFrom() != null ? message.getFrom().getId() : null;
        if (telegramUserId == null) {
            return AccessDecision.deny(AccessReason.UNSUPPORTED_CHAT);
        }
        Optional<UserEntity> user = userService.findByTelegramId(telegramUserId);
        if (user.isEmpty()) {
            return AccessDecision.deny(AccessReason.NO_SUBSCRIPTION, STUB_NO_SUB_PERSONAL);
        }
        Optional<SubscriptionEntity> sub = subscriptionService.findPersonal(bot.getId(), user.get().getId());
        if (sub.isEmpty()) {
            return AccessDecision.deny(AccessReason.NO_SUBSCRIPTION, STUB_NO_SUB_PERSONAL);
        }
        AccessDecision decision = evaluateSubscriptionStatus(sub.get());
        if (!decision.isAllowed()) {
            return decision;
        }
        if (!consentService.hasRequiredConsents(bot, user.get().getId())) {
            return AccessDecision.deny(AccessReason.PENDING_CONSENT, STUB_PENDING);
        }
        return decision;
    }

    private AccessDecision evaluateGroup(BotEntity bot, Message message, String botUsername) {
        if (!triggerMatcher.isAddressedToBot(bot, message, botUsername)) {
            return AccessDecision.deny(AccessReason.GROUP_TRIGGER_NOT_MATCHED);
        }
        Optional<SubscriptionEntity> sub = subscriptionService.findGroup(bot.getId(), message.getChatId());
        if (sub.isEmpty()) {
            return AccessDecision.deny(AccessReason.NO_SUBSCRIPTION, STUB_NO_SUB_GROUP);
        }
        SubscriptionEntity subscription = sub.get();
        AccessDecision base = evaluateSubscriptionStatus(subscription);
        if (!base.isAllowed()) {
            return base;
        }

        Long telegramUserId = message.getFrom() != null ? message.getFrom().getId() : null;
        if (telegramUserId == null) {
            return AccessDecision.deny(AccessReason.UNSUPPORTED_CHAT);
        }
        Optional<UserEntity> participant = userService.findByTelegramId(telegramUserId);
        if (participant.isEmpty()) {
            return AccessDecision.deny(AccessReason.UNSUPPORTED_CHAT);
        }
        Long appUserId = participant.get().getId();
        boolean consentOk = subscription.getOwnerUserId().equals(appUserId)
                ? consentService.hasRequiredConsents(bot, appUserId)
                : consentService.hasParticipantConsents(bot, appUserId);
        if (!consentOk) {
            String deeplink = "Откройте бота в личке: https://t.me/" + botUsername
                    + "?start=group_consent_" + subscription.getId();
            return AccessDecision.deny(AccessReason.PENDING_CONSENT,
                    "📝 Чтобы бот мог ответить вам в этой группе, подтвердите согласия в личке.\n" + deeplink);
        }

        participantService.touch(subscription.getId(), telegramUserId);

        Integer max = subscription.getMaxParticipants();
        if (max != null && max > 0) {
            OffsetDateTime monthStart = firstDayOfCalendarMonthUtc();
            long active = participantService.countActiveSince(subscription.getId(), monthStart);
            long hardCeiling = Math.round(Math.ceil(max * 1.3));
            LOGGER.debug("Participants | sub={} | active_since_month={} | max={} | hard={}",
                    subscription.getId(), active, max, hardCeiling);
            if (active > hardCeiling) {
                return AccessDecision.deny(AccessReason.PARTICIPANT_LIMIT_HARD, STUB_LIMIT_HARD);
            }
            if (active > max) {
                ownerParticipantLimitNotifier.notifySoftLimitExceeded(bot, subscription, active, max);
                LOGGER.warn("Group {} subscription #{} exceeds soft participant cap ({} > {})",
                        message.getChatId(), subscription.getId(), active, max);
            }
        }

        return base;
    }

    private AccessDecision evaluateSubscriptionStatus(SubscriptionEntity sub) {
        return switch (sub.getStatus()) {
            case AWAITING_ACTIVATION -> AccessDecision.deny(AccessReason.NO_SUBSCRIPTION, STUB_AWAITING_ACTIVATION);
            case PENDING_CONSENT -> AccessDecision.deny(AccessReason.PENDING_CONSENT, STUB_PENDING);
            case CANCELLED -> AccessDecision.deny(AccessReason.CANCELLED, STUB_CANCELLED);
            case BLOCKED_CONSENT -> AccessDecision.deny(AccessReason.BLOCKED_CONSENT, STUB_BLOCKED_CONSENT);
            case EXPIRED -> AccessDecision.deny(AccessReason.EXPIRED, STUB_EXPIRED);
            case TRIAL, ACTIVE -> {
                Optional<SubscriptionTariffEntity> tariffOpt = subscriptionTariffRepository.findById(sub.getTariffId());
                if (tariffOpt.isPresent()
                        && tariffOpt.get().getAccessMode() == TariffAccessMode.FREE_UNLIMITED
                        && sub.getStatus() == SubscriptionStatus.ACTIVE) {
                    if (sub.getRequiresConsentReacceptanceUntil() != null
                            && sub.getRequiresConsentReacceptanceUntil().isAfter(OffsetDateTime.now())) {
                        yield AccessDecision.allowWithReminder(sub, AccessReason.CONSENT_GRACE,
                                "📝 Условия обновились — подтвердите принятие новой версии до "
                                        + sub.getRequiresConsentReacceptanceUntil().toLocalDate());
                    }
                    yield AccessDecision.allow(sub);
                }

                OffsetDateTime expiresAt = sub.getExpiresAt();
                if (expiresAt == null || !expiresAt.isAfter(OffsetDateTime.now())) {
                    LOGGER.info("Subscription #{} status={} but expired at {}", sub.getId(), sub.getStatus(), expiresAt);
                    yield AccessDecision.deny(AccessReason.EXPIRED, STUB_EXPIRED);
                }
                if (sub.getRequiresConsentReacceptanceUntil() != null
                        && sub.getRequiresConsentReacceptanceUntil().isAfter(OffsetDateTime.now())) {
                    yield AccessDecision.allowWithReminder(sub, AccessReason.CONSENT_GRACE,
                            "📝 Условия обновились — подтвердите принятие новой версии до "
                                    + sub.getRequiresConsentReacceptanceUntil().toLocalDate());
                }
                yield AccessDecision.allow(sub);
            }
        };
    }

    private static OffsetDateTime firstDayOfCalendarMonthUtc() {
        OffsetDateTime now = OffsetDateTime.now(MOSKOW);
        OffsetDateTime first = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return first;
    }
}
