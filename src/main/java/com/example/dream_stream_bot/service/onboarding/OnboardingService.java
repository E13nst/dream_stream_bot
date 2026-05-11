package com.example.dream_stream_bot.service.onboarding;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.subscription.SubscriptionTariffService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import com.example.dream_stream_bot.service.telegram.TelegramGroupAdminService;
import com.example.dream_stream_bot.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Онбординг в личке: персональная подписка, владелец группы и участники группы.
 */
@Service
public class OnboardingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardingService.class);

    public static final String CALLBACK_START = "onboarding_start";
    public static final String CALLBACK_ACCEPT = "consent_accept";
    public static final String CALLBACK_DECLINE = "consent_decline";

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionTariffService subscriptionTariffService;
    private final ConsentService consentService;
    private final OnboardingScopeHolder scopeHolder;
    private final TelegramGroupAdminService telegramGroupAdminService;
    private final BotNavigationService botNavigationService;

    public OnboardingService(UserService userService,
                             SubscriptionService subscriptionService,
                             SubscriptionTariffService subscriptionTariffService,
                             ConsentService consentService,
                             OnboardingScopeHolder scopeHolder,
                             TelegramGroupAdminService telegramGroupAdminService,
                             BotNavigationService botNavigationService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.subscriptionTariffService = subscriptionTariffService;
        this.consentService = consentService;
        this.scopeHolder = scopeHolder;
        this.telegramGroupAdminService = telegramGroupAdminService;
        this.botNavigationService = botNavigationService;
    }

    /** /start payload в личке. */
    public List<OutgoingMessage> start(UserEntity user, BotEntity bot, Long chatId, String args) {
        if (user == null) {
            return List.of(OutgoingMessage.of(chatId,
                    "Не удалось определить ваш профиль. Попробуйте позже."));
        }
        if (bot == null) {
            return List.of(OutgoingMessage.of(chatId, "Бот не настроен. Обратитесь в поддержку."));
        }

        String payload = args == null ? "" : args.trim();

        if (payload.startsWith("group_consent_")) {
            return startParticipantConsent(bot, chatId, user, payload.substring("group_consent_".length()));
        }
        if (payload.startsWith("group_owner_")) {
            return startGroupOwner(bot, chatId, user, payload.substring("group_owner_".length()));
        }

        applyReferralPayload(user, payload);

        return showBotIntro(bot, chatId);
    }

    public List<OutgoingMessage> startPersonalAccess(UserEntity user, BotEntity bot, Long chatId) {
        if (user == null) {
            return List.of(OutgoingMessage.of(chatId,
                    "Не удалось определить ваш профиль. Попробуйте позже."));
        }
        if (bot == null) {
            return List.of(OutgoingMessage.of(chatId, "Бот не настроен. Обратитесь в поддержку."));
        }

        SubscriptionEntity sub = subscriptionService.createOrGet(
                user.getId(), bot.getId(), subscriptionTariffService.resolveDefaultPersonal(bot.getId()).getId(), null);

        scopeHolder.clearPendingGroup(bot.getId(), user.getId());
        scopeHolder.clearPendingParticipant(bot.getId(), user.getId());
        return continueOnboardingPersonal(user, bot, chatId, sub);
    }

    private List<OutgoingMessage> showBotIntro(BotEntity bot, Long chatId) {
        String text = botIntroText(bot);
        // В одном сообщении нельзя совместить inline- и reply-клавиатуру; /start показывает нижнее меню.
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
    }

    private List<OutgoingMessage> startParticipantConsent(BotEntity bot, Long chatId, UserEntity user, String subscriptionIdTail) {
        long subId;
        try {
            subId = Long.parseLong(subscriptionIdTail.trim());
        } catch (NumberFormatException e) {
            return List.of(OutgoingMessage.of(chatId, "Некорректная ссылка на подписку."));
        }
        Optional<SubscriptionEntity> opt = subscriptionService.findById(subId);
        if (opt.isEmpty() || !subscriptionService.isGroupSubscriptionByTariff(opt.get().getTariffId())
                || opt.get().getScopeChatId() == null) {
            return List.of(OutgoingMessage.of(chatId, "Подписка группы не найдена."));
        }
        SubscriptionEntity subscription = opt.get();
        if (!subscription.getBotId().equals(bot.getId())) {
            return List.of(OutgoingMessage.of(chatId, "Эта ссылка относится к другому боту."));
        }
        scopeHolder.setPendingParticipantSubscription(bot.getId(), user.getId(), subscription.getId());
        scopeHolder.clearPendingGroup(bot.getId(), user.getId());
        return continueParticipantOnboarding(user, bot, chatId);
    }

    private List<OutgoingMessage> startGroupOwner(BotEntity bot, Long chatId, UserEntity user, String chatTail) {
        Long tgChatId;
        try {
            tgChatId = Long.parseLong(chatTail.trim());
        } catch (NumberFormatException e) {
            return List.of(OutgoingMessage.of(chatId, "Некорректная ссылка на группу."));
        }
        if (!telegramGroupAdminService.isUserChatAdministrator(bot, tgChatId, user.getTelegramId())) {
            return List.of(OutgoingMessage.of(chatId,
                    "Вы должны быть администратором группы с id " + tgChatId + ", чтобы активировать подписку."));
        }

        SubscriptionEntity sub = subscriptionService.createOrGet(
                user.getId(), bot.getId(), subscriptionTariffService.resolveDefaultGroup(bot.getId()).getId(), tgChatId);
        scopeHolder.setPendingGroupChat(bot.getId(), user.getId(), tgChatId);
        scopeHolder.clearPendingParticipant(bot.getId(), user.getId());
        LOGGER.info("Group owner onboarding | user={} | bot={} | chat={} | subscription={}",
                user.getId(), bot.getId(), tgChatId, sub.getId());
        return continueOnboardingGroupOwner(user, bot, chatId, sub);
    }

    private List<OutgoingMessage> continueOnboardingPersonal(UserEntity user, BotEntity bot, Long chatId,
                                                          SubscriptionEntity subscription) {
        List<ConsentCode> required = requiredConsentsOwner(bot);
        ConsentCode next = nextMissingConsent(user.getId(), required);
        if (next != null) {
            return promptConsent(bot.getId(), chatId, next);
        }
        return finalizePersonalAfterConsents(user, bot, chatId, subscription);
    }

    private List<OutgoingMessage> continueOnboardingGroupOwner(UserEntity user, BotEntity bot, Long chatId,
                                                              SubscriptionEntity subscription) {
        List<ConsentCode> required = requiredConsentsOwner(bot);
        ConsentCode next = nextMissingConsent(user.getId(), required);
        if (next != null) {
            return promptConsent(bot.getId(), chatId, next);
        }
        return finalizeGroupOwnerAfterConsents(user, bot, chatId, subscription);
    }

    private List<OutgoingMessage> continueParticipantOnboarding(UserEntity user, BotEntity bot, Long chatId) {
        List<ConsentCode> required = requiredConsentsParticipant(bot);
        ConsentCode next = nextMissingConsent(user.getId(), required);
        if (next != null) {
            return promptConsent(bot.getId(), chatId, next);
        }
        Optional<Long> subId = scopeHolder.getPendingParticipantSubscription(bot.getId(), user.getId());
        if (subId.isEmpty()) {
            return List.of(OutgoingMessage.of(chatId,
                    "Сессия истекла. Откройте ссылку из группы заново."));
        }
        subscriptionService.requireById(subId.get());
        scopeHolder.clearPendingParticipant(bot.getId(), user.getId());
        return List.of(OutgoingMessage.of(chatId,
                """
                Спасибо! Согласия приняты. Теперь бот может отвечать вам в группе при обращениях."""));
    }

    public List<OutgoingMessage> continueOnboarding(UserEntity user, BotEntity bot, Long chatId,
                                                    SubscriptionEntity subscription) {
        Optional<Long> g = scopeHolder.getPendingGroupChat(bot.getId(), user.getId());
        if (g.isPresent()) {
            return continueOnboardingGroupOwner(user, bot, chatId, subscription);
        }
        if (subscriptionService.isGroupSubscriptionByTariff(subscription.getTariffId())
                && subscription.getScopeChatId() != null && subscription.getOwnerUserId().equals(user.getId())) {
            return continueOnboardingGroupOwner(user, bot, chatId, subscription);
        }
        return continueOnboardingPersonal(user, bot, chatId, subscription);
    }

    public List<OutgoingMessage> recordAcceptance(UserEntity user, BotEntity bot, Long chatId,
                                                  Long documentId, Integer telegramMessageId) {
        if (user == null || bot == null || documentId == null) {
            return List.of();
        }

        Optional<Long> participantSubId = scopeHolder.getPendingParticipantSubscription(bot.getId(), user.getId());
        if (participantSubId.isPresent()) {
            consentService.recordAcceptance(user.getId(), documentId, participantSubId.get(),
                    chatId, telegramMessageId, "callback");
            return continueParticipantOnboarding(user, bot, chatId);
        }

        SubscriptionEntity subscription = resolveOwnerSubscription(bot, user);
        consentService.recordAcceptance(user.getId(), documentId, subscription.getId(),
                chatId, telegramMessageId, "callback");
        return continueOnboarding(user, bot, chatId, subscription);
    }

    private SubscriptionEntity resolveOwnerSubscription(BotEntity bot, UserEntity user) {
        Optional<Long> gc = scopeHolder.getPendingGroupChat(bot.getId(), user.getId());
        if (gc.isPresent()) {
            return subscriptionService.findGroup(bot.getId(), gc.get())
                    .orElseGet(() -> subscriptionService.createOrGet(
                            user.getId(), bot.getId(), subscriptionTariffService.resolveDefaultGroup(bot.getId()).getId(), gc.get()));
        }
        return subscriptionService.findPersonal(bot.getId(), user.getId())
                .orElseGet(() -> subscriptionService.createOrGet(user.getId(), bot.getId(),
                        subscriptionTariffService.resolveDefaultPersonal(bot.getId()).getId(), null));
    }

    public List<OutgoingMessage> recordDecline(Long chatId, ConsentCode code) {
        if (code == ConsentCode.OFFER) {
            return List.of(OutgoingMessage.of(chatId, """
                    Без согласия с документом «%s» оплатить подписку невозможно.
                    Когда будете готовы — вернитесь к оплате или нажмите /start.
                    """.formatted(code.getDefaultTitle())));
        }
        return List.of(OutgoingMessage.of(chatId, """
                Без согласия с документом «%s» доступ к боту невозможен.
                Когда будете готовы — нажмите /start ещё раз.
                """.formatted(code.getDefaultTitle())));
    }

    private List<OutgoingMessage> finalizePersonalAfterConsents(UserEntity user, BotEntity bot, Long chatId,
                                                              SubscriptionEntity subscription) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                || subscription.getStatus() == SubscriptionStatus.TRIAL) {
            return List.of(mainMenuMessage(chatId, activeGreeting(subscription)));
        }

        if (subscription.getStatus() == SubscriptionStatus.PENDING_CONSENT) {
            SubscriptionTariffEntity tariff = subscriptionTariffService.require(subscription.getTariffId());
            if (tariff.getAccessMode() == TariffAccessMode.FREE_UNLIMITED) {
                subscriptionService.activateFreeUnlimited(subscription);
                LOGGER.info("🎉 Free unlimited activated for user={} bot={}", user.getId(), bot.getId());
                return List.of(mainMenuMessage(chatId,
                        """
                        ✅ Все согласия приняты. Доступ бесплатный и без ограничения срока.
                        Используйте кнопки ниже для навигации:
                        🌙 Рассказать сон, 📖 Мой дневник, ⚙️ Настройки, 💎 Подписка."""));
            }
            if (tariff.getAccessMode() == TariffAccessMode.PAID_TERM) {
                ConsentCode nextPurchaseConsent = nextMissingConsent(user.getId(), requiredPurchaseConsentsOwner(bot.getId()));
                if (nextPurchaseConsent != null) {
                    return promptConsent(bot.getId(), chatId, nextPurchaseConsent);
                }
                SubscriptionEntity awaiting = subscriptionService.markAwaitingActivation(subscription);
                LOGGER.info("Personal subscription awaiting activation | sub={} | user={}", awaiting.getId(), user.getId());
                return List.of(mainMenuMessage(chatId,
                        """
                        Спасибо! Согласия приняты. Подписка ожидает активации или оплаты администратором.
                        Если у вас уже была оплаченная запись — обратитесь в поддержку.
                        Напоминаем команду статуса: /subscriptions."""));
            }
            if (tariff.getAccessMode() == TariffAccessMode.TRIAL_ONBOARDING) {
                int days = tariff.getTrialDays() != null ? tariff.getTrialDays() : SubscriptionService.FALLBACK_TRIAL_DAYS;
                try {
                    SubscriptionEntity activated = subscriptionService.activateTrial(subscription, days, null);
                    LOGGER.info("🎉 Trial activated for user={} bot={} expires={}",
                            user.getId(), bot.getId(), activated.getExpiresAt());
                    return List.of(mainMenuMessage(chatId,
                            """
                            🎉 Все согласия приняты. Активирован пробный период на %d дня (до %s).
                            Используйте кнопки ниже для навигации:
                            🌙 Рассказать сон, 📖 Мой дневник, ⚙️ Настройки, 💎 Подписка.""".formatted(
                                    days,
                                    formatExpiry(activated.getExpiresAt()))));
                } catch (IllegalStateException e) {
                    LOGGER.info("Trial already used for user={} bot={}", user.getId(), bot.getId());
                    return List.of(OutgoingMessage.of(chatId,
                            """
                            Пробный период вы уже использовали ранее.
                            Чтобы продолжить — оплатите подписку командой /subscriptions
                            или свяжитесь с поддержкой."""));
                }
            }
        }
        return genericStatus(chatId, subscription);
    }

    private List<OutgoingMessage> finalizeGroupOwnerAfterConsents(UserEntity user, BotEntity bot, Long chatId,
                                                                  SubscriptionEntity subscription) {
        if (subscription.getStatus() == SubscriptionStatus.TRIAL
                || subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            scopeHolder.clearPendingGroup(bot.getId(), user.getId());
            return List.of(mainMenuMessage(chatId, activeGreeting(subscription)));
        }

        ConsentCode nextPurchaseConsent = nextMissingConsent(user.getId(), requiredPurchaseConsentsOwner(bot.getId()));
        if (nextPurchaseConsent != null) {
            return promptConsent(bot.getId(), chatId, nextPurchaseConsent);
        }

        scopeHolder.clearPendingGroup(bot.getId(), user.getId());
        SubscriptionEntity awaiting = subscriptionService.markAwaitingActivation(subscription);

        Optional<Long> sc = subscription.getScopeChatId() != null
                ? Optional.of(subscription.getScopeChatId()) : Optional.empty();
        Long scope = sc.orElse(null);
        String invite = scope != null
                ? """
                Спасибо! Согласия приняты. Подписка ожидает активации администратором сервиса.
                После активации участники должны один раз принять условия в личке:
                https://t.me/%s?start=group_consent_%d
                
                После включения каждый участник сможет писать боту в группе по триггерам.
                """.formatted(bot.getUsername(), awaiting.getId())
                : "Согласия приняты. Ожидайте активации администратором.";

        LOGGER.info("Group subscription awaiting activation | sub={} | chat={}", awaiting.getId(), scope);
        return List.of(OutgoingMessage.of(chatId, invite));
    }

    private static List<OutgoingMessage> genericStatus(Long chatId, SubscriptionEntity subscription) {
        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            return List.of(OutgoingMessage.of(chatId,
                    "Срок подписки истёк. Чтобы продолжить, продлите её — /subscriptions."));
        }
        if (subscription.getStatus() == SubscriptionStatus.BLOCKED_CONSENT) {
            return List.of(OutgoingMessage.of(chatId,
                    "Условия использования обновились. Нажмите /start ещё раз."));
        }
        return List.of(OutgoingMessage.of(chatId,
                "Текущий статус подписки: " + subscription.getStatus()));
    }

    private List<OutgoingMessage> promptConsent(Long botId, Long chatId, ConsentCode code) {
        Optional<ConsentDocumentEntity> doc = botId == null
                ? Optional.empty()
                : consentService.getActiveForBot(botId, code);
        if (doc.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Документ «%s» ещё не опубликован администратором.

                            Это не мешает посмотреть тарифы: откройте /subscriptions
                            или кнопку «💎 Подписка» внизу.

                            Для полноценного доступа к диалогу понадобится публикация документа и его принятие.
                            """.formatted(code.getDefaultTitle()).trim())
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }

        ConsentDocumentEntity d = doc.get();
        String url = d.getExternalUrl();
        StringBuilder text = new StringBuilder();
        text.append("📄 *").append(escapeMd(d.getTitle())).append("* (v").append(d.getVersion()).append(")\n\n");
        if (url != null && !url.isBlank()) {
            text.append("Прочитайте: ").append(url).append("\n\n");
        }
        text.append("Нажимая «Принимаю», вы подтверждаете согласие с указанным документом.");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("✅ Принимаю")
                .callbackData(CALLBACK_ACCEPT + ":" + d.getId())
                .build());
        row.add(InlineKeyboardButton.builder()
                .text("❌ Отказываюсь")
                .callbackData(CALLBACK_DECLINE + ":" + d.getCode().name())
                .build());

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();

        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .text(text.toString())
                .parseMode("Markdown")
                .disableWebPagePreview(false)
                .replyMarkup(keyboard)
                .build());
    }

    private ConsentCode nextMissingConsent(Long userId, List<ConsentCode> required) {
        for (ConsentCode code : required) {
            if (!consentService.hasAcceptedCurrent(userId, code)) {
                return code;
            }
        }
        return null;
    }

    private List<ConsentCode> requiredConsentsOwner(BotEntity bot) {
        if (bot == null || bot.getId() == null) {
            return List.of();
        }
        return consentService.requiredCodesForBot(bot.getId(), false);
    }

    private List<ConsentCode> requiredPurchaseConsentsOwner(Long botId) {
        if (botId == null) {
            return List.of();
        }
        List<ConsentCode> allCodes = consentService.requiredCodesForBot(botId, true);
        return allCodes.contains(ConsentCode.OFFER) ? List.of(ConsentCode.OFFER) : List.of();
    }

    /** Участник группы не подписывает оферту (покупает ведущий). */
    private List<ConsentCode> requiredConsentsParticipant(BotEntity bot) {
        if (bot == null || bot.getId() == null) {
            return List.of();
        }
        return consentService.requiredCodesForBot(bot.getId(), false);
    }

    private void applyReferralPayload(UserEntity user, String args) {
        if (args == null || args.isBlank()) {
            return;
        }
        String trimmed = args.trim();
        if (!trimmed.startsWith("ref_")) {
            return;
        }
        if (user.getReferredByUserId() != null) {
            return;
        }
        String tail = trimmed.substring(4);
        Long referrerId;
        try {
            referrerId = Long.parseLong(tail);
        } catch (NumberFormatException e) {
            return;
        }
        if (referrerId.equals(user.getId())) {
            return;
        }
        if (userService.findById(referrerId).isEmpty()) {
            return;
        }
        user.setReferredByUserId(referrerId);
        userService.save(user);
        LOGGER.info("🤝 Referral attached | user={} | referrer={}", user.getId(), referrerId);
    }

    private static String botIntroText(BotEntity bot) {
        String description = bot.getDescription();
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        String name = bot.getBotName();
        String label = name != null && !name.isBlank() ? name.trim() : "этот бот";
        return """
                Привет! Я %s.

                Нажмите «%s» в меню внизу, чтобы активировать доступ, или просто пришлите сообщение, если доступ уже подключён.
                """.formatted(label, BotNavigationService.BTN_START).trim();
    }

    private static String activeGreeting(SubscriptionEntity subscription) {
        return """
                Подписка активна (%s).
                Главное меню доступно в кнопках внизу: 🌙, 📖, ⚙️, 💎.""".formatted(
                formatExpiry(subscription.getExpiresAt()));
    }

    private OutgoingMessage mainMenuMessage(Long chatId, String text) {
        return OutgoingMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build();
    }

    private static String formatExpiry(OffsetDateTime expiresAt) {
        if (expiresAt == null) {
            return "—";
        }
        return expiresAt.toLocalDate().toString();
    }

    private static String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("*", "\\*").replace("_", "\\_").replace("[", "\\[").replace("]", "\\]");
    }
}
