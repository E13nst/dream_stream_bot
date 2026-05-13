package com.example.dream_stream_bot.service.onboarding;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.payment.SubscriptionCheckoutService;
import com.example.dream_stream_bot.service.subscription.GroupLinkWizardStateHolder;
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
import java.util.LinkedList;
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

    /** Inline «Начать» после текста про политику (одно сообщение с reply-клавиатурой вводится первым сообщением /start отдельно). */
    public static final String CALLBACK_PRIVACY_ACCEPT = "onboarding_privacy_accept";

    /** Выбор персонального триала/free тарифа на онбординге. */
    public static final String CALLBACK_TARIFF_PICK = "onboarding_tariff_pick";

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionTariffService subscriptionTariffService;
    private final ConsentService consentService;
    private final OnboardingScopeHolder scopeHolder;
    private final TelegramGroupAdminService telegramGroupAdminService;
    private final BotNavigationService botNavigationService;
    private final SubscriptionCheckoutService subscriptionCheckoutService;
    private final GroupLinkWizardStateHolder groupLinkWizardStateHolder;

    public OnboardingService(UserService userService,
                             SubscriptionService subscriptionService,
                             SubscriptionTariffService subscriptionTariffService,
                             ConsentService consentService,
                             OnboardingScopeHolder scopeHolder,
                             TelegramGroupAdminService telegramGroupAdminService,
                             BotNavigationService botNavigationService,
                             SubscriptionCheckoutService subscriptionCheckoutService,
                             GroupLinkWizardStateHolder groupLinkWizardStateHolder) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.subscriptionTariffService = subscriptionTariffService;
        this.consentService = consentService;
        this.scopeHolder = scopeHolder;
        this.telegramGroupAdminService = telegramGroupAdminService;
        this.botNavigationService = botNavigationService;
        this.subscriptionCheckoutService = subscriptionCheckoutService;
        this.groupLinkWizardStateHolder = groupLinkWizardStateHolder;
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

        groupLinkWizardStateHolder.clear(bot.getId(), user.getId());

        if (payload.startsWith("group_consent_")) {
            return startParticipantConsent(bot, chatId, user, payload.substring("group_consent_".length()));
        }
        if (payload.startsWith("group_owner_")) {
            return startGroupOwner(bot, chatId, user, payload.substring("group_owner_".length()));
        }

        applyReferralPayload(user, payload);

        return showBotIntro(bot, user, chatId);
    }

    public List<OutgoingMessage> startPersonalAccess(UserEntity user, BotEntity bot, Long chatId) {
        if (user == null) {
            return List.of(OutgoingMessage.of(chatId,
                    "Не удалось определить ваш профиль. Попробуйте позже."));
        }
        if (bot == null) {
            return List.of(OutgoingMessage.of(chatId, "Бот не настроен. Обратитесь в поддержку."));
        }

        scopeHolder.clearPendingGroup(bot.getId(), user.getId());
        scopeHolder.clearPendingParticipant(bot.getId(), user.getId());
        groupLinkWizardStateHolder.clear(bot.getId(), user.getId());

        if (!personalPrivacySatisfied(bot.getId(), user.getId())) {
            return privacyGateReminderOrBanner(bot.getId(), chatId);
        }
        Optional<SubscriptionEntity> existing = subscriptionService.findPersonal(bot.getId(), user.getId());
        if (existing.isPresent()) {
            return continueOnboardingPersonal(user, bot, chatId, existing.get());
        }
        return proceedPersonalTariffSelection(user, bot, chatId);
    }

    /** Callback: принятие активной версии политики с шлюза перед выбором тарифа/активацией. */
    public List<OutgoingMessage> acceptPrivacyGateAndContinue(UserEntity user, BotEntity bot, Long chatId,
                                                               long privacyDocumentId, Integer telegramMessageId) {
        if (user == null || bot == null) {
            return List.of();
        }
        Optional<ConsentDocumentEntity> active =
                consentService.getActiveForBot(bot.getId(), ConsentCode.PRIVACY_POLICY);
        if (active.isEmpty() || !active.get().getId().equals(privacyDocumentId)) {
            return List.of(OutgoingMessage.of(chatId,
                    "Версия политики устарела или не найдена. Нажмите /start ещё раз."));
        }
        Optional<Long> pendingParticipantSub = scopeHolder.getPendingParticipantSubscription(bot.getId(), user.getId());
        if (pendingParticipantSub.isPresent()) {
            consentService.recordAcceptance(user.getId(), privacyDocumentId, pendingParticipantSub.get(), chatId,
                    telegramMessageId, "privacy_gate");
            scopeHolder.clearPendingGroup(bot.getId(), user.getId());
            groupLinkWizardStateHolder.clear(bot.getId(), user.getId());
            return continueParticipantOnboarding(user, bot, chatId);
        }
        consentService.recordAcceptance(user.getId(), privacyDocumentId, null, chatId,
                telegramMessageId, "privacy_gate");
        scopeHolder.clearPendingGroup(bot.getId(), user.getId());
        scopeHolder.clearPendingParticipant(bot.getId(), user.getId());
        groupLinkWizardStateHolder.clear(bot.getId(), user.getId());
        Optional<SubscriptionEntity> existing = subscriptionService.findPersonal(bot.getId(), user.getId());
        if (existing.isPresent()) {
            return continueOnboardingPersonal(user, bot, chatId, existing.get());
        }
        return proceedPersonalTariffSelection(user, bot, chatId);
    }

    /** Callback: выбор персонального free/trial после политики. */
    public List<OutgoingMessage> pickPersonalTrialOrFreeTariff(UserEntity user, BotEntity bot, Long chatId,
                                                                long tariffId) {
        if (user == null || bot == null) {
            return List.of();
        }
        if (!personalPrivacySatisfied(bot.getId(), user.getId())) {
            return List.of(OutgoingMessage.of(chatId,
                    "Подтвердите политику конфиденциальности — нажмите «Начать» во втором сообщении после /start или откройте /start заново."));
        }
        boolean allowed = subscriptionTariffService.listPersonalTrialAndFreeEligible(bot.getId(), user.getId())
                .stream()
                .anyMatch(t -> t.getId().equals(tariffId));
        if (!allowed) {
            return List.of(OutgoingMessage.of(chatId,
                    "Этот вариант сейчас недоступен. Откройте /start или раздел 💎 Подписка."));
        }
        SubscriptionTariffEntity pickedMeta = subscriptionTariffService.require(tariffId);
        if (pickedMeta.getScope() != TariffScope.PERSONAL) {
            return List.of(OutgoingMessage.of(chatId,
                    "Этот вариант сейчас недоступен. Откройте /start или раздел 💎 Подписка."));
        }
        SubscriptionEntity sub =
                subscriptionService.createOrGet(user.getId(), bot.getId(), tariffId, null);
        return continueOnboardingPersonal(user, bot, chatId, sub);
    }

    /**
     * {@code false}, если для бота задана привязка PRIVACY_POLICY и пользователь ещё не принял активную версию.
     */
    private boolean personalPrivacySatisfied(Long botId, Long userId) {
        Optional<ConsentDocumentEntity> privacy = consentService.getActiveForBot(botId, ConsentCode.PRIVACY_POLICY);
        if (privacy.isEmpty()) {
            return true;
        }
        return consentService.hasAcceptedBotBoundDocument(userId, botId, ConsentCode.PRIVACY_POLICY);
    }

    private List<OutgoingMessage> proceedPersonalTariffSelection(UserEntity user, BotEntity bot, Long chatId) {
        List<SubscriptionTariffEntity> eligible =
                subscriptionTariffService.listPersonalTrialAndFreeEligible(bot.getId(), user.getId());
        if (eligible.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Бесплатный доступ сейчас недоступен. Обратитесь в поддержку или проверьте настройки бота.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        if (eligible.size() == 1) {
            SubscriptionTariffEntity t = eligible.get(0);
            SubscriptionEntity sub = subscriptionService.createOrGet(user.getId(), bot.getId(), t.getId(), null);
            return continueOnboardingPersonal(user, bot, chatId, sub);
        }
        return List.of(tariffPickerMessage(chatId, eligible));
    }

    private OutgoingMessage tariffPickerMessage(Long chatId, List<SubscriptionTariffEntity> tariffs) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (SubscriptionTariffEntity t : tariffs) {
            keyboard.add(List.of(InlineKeyboardButton.builder()
                    .text(truncateInlineButtonText(t.getTitle()))
                    .callbackData(CALLBACK_TARIFF_PICK + ":" + t.getId())
                    .build()));
        }
        return OutgoingMessage.builder()
                .chatId(chatId)
                .text("Выберите вариант бесплатного доступа:")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboard)
                        .build())
                .build();
    }

    /** Telegram inline-кнопка: до 64 символов. */
    private static String truncateInlineButtonText(String title) {
        if (title == null || title.isBlank()) {
            return "Тариф";
        }
        if (title.length() <= 64) {
            return title;
        }
        return title.substring(0, 61) + "…";
    }

    /**
     * Напоминание о непринятой политике или неопубликованном документе (inline «Начать» во втором сообщении после /start).
     * Также срабатывает, если пользователь жмёт устаревшую reply-кнопку «Начать» до принятия политики.
     */
    private List<OutgoingMessage> privacyGateReminderOrBanner(Long botId, Long chatId) {
        Optional<ConsentDocumentEntity> privacy = consentService.getActiveForBot(botId, ConsentCode.PRIVACY_POLICY);
        if (privacy.isEmpty()) {
            return List.of(OutgoingMessage.of(chatId, "Откройте /start для продолжения."));
        }
        ConsentDocumentEntity doc = privacy.get();
        String url = doc.getExternalUrl();
        if (url == null || url.isBlank()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Политика конфиденциальности ещё не опубликована администратором.

                            Пока это не исправят, активировать пробный доступ нельзя.""")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        List<OutgoingMessage> out = new LinkedList<>();
        out.add(OutgoingMessage.of(chatId,
                "Подтвердите ознакомление с политикой — нажмите «Начать» во втором сообщении после команды /start."));
        out.add(privacyConsentInlineMessage(chatId, doc, null));
        return out;
    }

    private List<OutgoingMessage> showBotIntro(BotEntity bot, UserEntity user, Long chatId) {
        String text = botIntroText(bot);
        List<OutgoingMessage> messages = new ArrayList<>();
        messages.add(OutgoingMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
        messages.addAll(privacyGateOutgoingIfNeeded(bot, user, chatId));
        return messages;
    }

    /**
     * Второе сообщение после /start: политика + inline «Начать». Пустой список, если политика не привязана или уже принята.
     */
    private List<OutgoingMessage> privacyGateOutgoingIfNeeded(BotEntity bot, UserEntity user, Long chatId) {
        Optional<ConsentDocumentEntity> privacy =
                consentService.getActiveForBot(bot.getId(), ConsentCode.PRIVACY_POLICY);
        if (privacy.isEmpty()) {
            return List.of();
        }
        if (consentService.hasAcceptedBotBoundDocument(user.getId(), bot.getId(), ConsentCode.PRIVACY_POLICY)) {
            return List.of();
        }
        ConsentDocumentEntity doc = privacy.get();
        String url = doc.getExternalUrl();
        if (url == null || url.isBlank()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Документ «%s» ещё не опубликован администратором (нет ссылки).

                            После публикации нажмите /start ещё раз.""".formatted(ConsentCode.PRIVACY_POLICY.getDefaultTitle()).trim())
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        return List.of(privacyConsentInlineMessage(chatId, doc, null));
    }

    /**
     * @param prefixPlainLine необязательная строка без разметки (например контекст для участника группы); экранируется для Markdown.
     */
    private OutgoingMessage privacyConsentInlineMessage(Long chatId, ConsentDocumentEntity doc, String prefixPlainLine) {
        String url = markdownEscapeTelegram(doc.getExternalUrl());
        String core = """
                Нажимая кнопку «Начать», вы подтверждаете, что ознакомились с [Политикой конфиденциальности](%s) и даёте согласие на обработку персональных данных."""
                .formatted(url);
        String text = (prefixPlainLine == null || prefixPlainLine.isBlank())
                ? core
                : escapeMd(prefixPlainLine.strip()) + "\n\n" + core;
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text(BotNavigationService.BTN_START)
                        .callbackData(CALLBACK_PRIVACY_ACCEPT + ":" + doc.getId())
                        .build()))
                .build();
        return OutgoingMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .disableWebPagePreview(true)
                .replyMarkup(keyboard)
                .build();
    }

    /**
     * Экранирование URL для Telegram legacy Markdown ({@code (...)} может ломаться на символах вроде {@code )}).
     */
    private static String markdownEscapeTelegram(String u) {
        if (u == null) {
            return "";
        }
        return u.replace("\\", "\\\\").replace(")", "\\)");
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
                    """
                            Не удалось подтвердить, что вы создатель или администратор этой группы.
                            Откройте ссылку с того же аккаунта Telegram, с которого заходите в чат, и убедитесь, что бот добавлен в группу."""
                            .strip()));
        }

        SubscriptionEntity sub = subscriptionService.createOrGet(
                user.getId(), bot.getId(), subscriptionTariffService.resolveDefaultGroup(bot.getId()).getId(), tgChatId);
        scopeHolder.setPendingGroupChat(bot.getId(), user.getId(), tgChatId);
        scopeHolder.clearPendingParticipant(bot.getId(), user.getId());
        LOGGER.info("Group owner onboarding | user={} | bot={} | chat={} | subscription={}",
                user.getId(), bot.getId(), tgChatId, sub.getId());
        return continueOnboardingGroupOwner(user, bot, chatId, sub);
    }

    /**
     * Мастер привязки группы: подписка с выбранным тарифом и тот же онбординг владельца группы, что и для {@code group_owner_}.
     */
    public List<OutgoingMessage> startGroupOwnerWithChosenTariff(BotEntity bot, Long privateChatId, UserEntity user,
                                                                 Long scopeChatId, long tariffId) {
        subscriptionTariffService.requireForBot(bot.getId(), tariffId);
        SubscriptionEntity sub = subscriptionService.createOrGet(
                user.getId(), bot.getId(), tariffId, scopeChatId);
        scopeHolder.setPendingGroupChat(bot.getId(), user.getId(), scopeChatId);
        scopeHolder.clearPendingParticipant(bot.getId(), user.getId());
        LOGGER.info("Group wizard onboarding | user={} | bot={} | chat={} | tariff={} | sub={}",
                user.getId(), bot.getId(), scopeChatId, tariffId, sub.getId());
        return continueOnboardingGroupOwner(user, bot, privateChatId, sub);
    }

    private List<OutgoingMessage> continueOnboardingPersonal(UserEntity user, BotEntity bot, Long chatId,
                                                          SubscriptionEntity subscription) {
        List<ConsentCode> required = requiredConsentsOwner(bot);
        ConsentCode next = nextMissingConsent(user.getId(), bot.getId(), required);
        if (next != null) {
            return promptConsent(bot.getId(), chatId, next);
        }
        return finalizePersonalAfterConsents(user, bot, chatId, subscription);
    }

    private List<OutgoingMessage> continueOnboardingGroupOwner(UserEntity user, BotEntity bot, Long chatId,
                                                              SubscriptionEntity subscription) {
        List<ConsentCode> required = requiredConsentsOwner(bot);
        ConsentCode next = nextMissingConsent(user.getId(), bot.getId(), required);
        if (next != null) {
            return promptConsent(bot.getId(), chatId, next);
        }
        return finalizeGroupOwnerAfterConsents(user, bot, chatId, subscription);
    }

    private List<OutgoingMessage> continueParticipantOnboarding(UserEntity user, BotEntity bot, Long chatId) {
        List<ConsentCode> required = requiredConsentsParticipant(bot);
        ConsentCode next = nextMissingConsent(user.getId(), bot.getId(), required);
        if (next == ConsentCode.PRIVACY_POLICY) {
            Optional<ConsentDocumentEntity> privacy =
                    consentService.getActiveForBot(bot.getId(), ConsentCode.PRIVACY_POLICY);
            if (privacy.isEmpty()) {
                return promptConsent(bot.getId(), chatId, ConsentCode.PRIVACY_POLICY);
            }
            ConsentDocumentEntity doc = privacy.get();
            String url = doc.getExternalUrl();
            if (url == null || url.isBlank()) {
                return List.of(OutgoingMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Документ «%s» ещё не опубликован администратором (нет ссылки).

                                После публикации нажмите /start ещё раз.""".formatted(ConsentCode.PRIVACY_POLICY.getDefaultTitle()).trim())
                        .replyMarkup(botNavigationService.privateMainKeyboard())
                        .build());
            }
            Optional<Long> pendingSubId = scopeHolder.getPendingParticipantSubscription(bot.getId(), user.getId());
            if (pendingSubId.isEmpty()) {
                return List.of(OutgoingMessage.of(chatId,
                        "Сессия истекла. Откройте ссылку из группы заново."));
            }
            SubscriptionEntity pendingSub = subscriptionService.requireById(pendingSubId.get());
            Long scopeChatId = pendingSub.getScopeChatId();
            String groupTitle = scopeChatId == null
                    ? "группу"
                    : telegramGroupAdminService.getChatTitle(bot, scopeChatId).orElse("Группа #" + scopeChatId);
            String prefix = "Вы присоединяетесь к боту через группу " + groupTitle;
            return List.of(privacyConsentInlineMessage(chatId, doc, prefix));
        }
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
                        На нижней клавиатуре — «%s» и «%s». Дополнительные действия открываются в «%s».
                        Сон можно отправить одним сообщением в этот чат.""".formatted(
                                BotNavigationService.BTN_SETTINGS,
                                BotNavigationService.BTN_SUBSCRIPTION,
                                BotNavigationService.BTN_SETTINGS)));
            }
            if (tariff.getAccessMode() == TariffAccessMode.PAID_TERM) {
                ConsentCode nextPurchaseConsent = nextMissingConsent(user.getId(), bot.getId(),
                        requiredPurchaseConsentsOwner(bot.getId()));
                if (nextPurchaseConsent != null) {
                    return promptConsent(bot.getId(), chatId, nextPurchaseConsent);
                }
                SubscriptionEntity awaiting = subscriptionService.markAwaitingActivation(subscription);
                LOGGER.info("Personal subscription awaiting activation | sub={} | user={}", awaiting.getId(), user.getId());

                if (subscriptionCheckoutService.isPaidCheckoutAvailable(bot, tariff)) {
                    String receiptHint = bot.isYookassaReceiptEnabled()
                            ? "\n\nДля чека по 54‑ФЗ укажите email командой:\n/billing_email ваш@example.ru"
                            : "";
                    InlineKeyboardMarkup payKb = InlineKeyboardMarkup.builder()
                            .keyboardRow(List.of(InlineKeyboardButton.builder()
                                    .text("💳 Оплатить в Telegram")
                                    .callbackData(BotNavigationService.CALLBACK_PAY + ":list")
                                    .build()))
                            .build();
                    return List.of(OutgoingMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Спасибо! Согласия приняты. Оплатите подписку, чтобы активировать доступ.%s

                                    После успешной оплаты доступ откроется автоматически (обычно в течение минуты).
                                    Статус: /subscriptions
                                    """.formatted(receiptHint))
                            .replyMarkup(payKb)
                            .build());
                }

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
                            На нижней клавиатуре — «%s» и «%s». Дополнительные действия — в «%s».
                            Сон можно отправить одним сообщением в этот чат.""".formatted(
                                    days,
                                    formatExpiry(activated.getExpiresAt()),
                                    BotNavigationService.BTN_SETTINGS,
                                    BotNavigationService.BTN_SUBSCRIPTION,
                                    BotNavigationService.BTN_SETTINGS)));
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

        SubscriptionTariffEntity tariff = subscriptionTariffService.require(subscription.getTariffId());
        if (subscription.getStatus() == SubscriptionStatus.PENDING_CONSENT
                && tariff.getAccessMode() == TariffAccessMode.TRIAL_ONBOARDING) {
            int days = tariff.getTrialDays() != null ? tariff.getTrialDays() : SubscriptionService.FALLBACK_TRIAL_DAYS;
            try {
                SubscriptionEntity activated = subscriptionService.activateTrial(subscription, days, null);
                scopeHolder.clearPendingGroup(bot.getId(), user.getId());
                LOGGER.info("🎉 Group trial activated | user={} | bot={} | sub={} | expires={}",
                        user.getId(), bot.getId(), activated.getId(), activated.getExpiresAt());
                String instruction = "";
                if (tariff.getActivationInstruction() != null && !tariff.getActivationInstruction().isBlank()) {
                    instruction = "\n\n" + tariff.getActivationInstruction().trim();
                }
                Long scopeChatId = activated.getScopeChatId();
                String groupTitle = scopeChatId == null
                        ? "Группа"
                        : telegramGroupAdminService.getChatTitle(bot, scopeChatId).orElse("Группа #" + scopeChatId);
                String mainText = """
                        ✅ Группа %s подключена до %s.%s

                        Отправьте участникам приглашение, чтобы они могли общаться с ботом."""
                        .formatted(groupTitle, formatExpiry(activated.getExpiresAt()), instruction)
                        .strip();
                InlineKeyboardMarkup successKb = InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("📢 Отправить приглашение в группу")
                                        .callbackData(BotNavigationService.CALLBACK_GRP + ":invite:" + activated.getId())
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text(BotNavigationService.BTN_SUBSCRIPTION)
                                        .callbackData(botNavigationService.navPayload("subscriptions"))
                                        .build()))
                        .build();
                OutgoingMessage withInline = OutgoingMessage.builder()
                        .chatId(chatId)
                        .text(mainText)
                        .replyMarkup(successKb)
                        .build();
                OutgoingMessage withReplyKb = OutgoingMessage.builder()
                        .chatId(chatId)
                        .text(("Ниже — «%s» и «%s».")
                                .formatted(BotNavigationService.BTN_SETTINGS, BotNavigationService.BTN_SUBSCRIPTION))
                        .replyMarkup(botNavigationService.privateMainKeyboard())
                        .build();
                return List.of(withInline, withReplyKb);
            } catch (IllegalStateException e) {
                LOGGER.info("Group trial already used | user={} | bot={} | sub={}", user.getId(), bot.getId(), subscription.getId());
                scopeHolder.clearPendingGroup(bot.getId(), user.getId());
                return List.of(OutgoingMessage.of(chatId,
                        """
                                Пробный групповой период по этому тарифу уже использован.
                                Выберите другой тариф в /subscriptions или напишите в поддержку."""));
            }
        }

        ConsentCode nextPurchaseConsent = nextMissingConsent(user.getId(), bot.getId(),
                requiredPurchaseConsentsOwner(bot.getId()));
        if (nextPurchaseConsent != null) {
            return promptConsent(bot.getId(), chatId, nextPurchaseConsent);
        }

        scopeHolder.clearPendingGroup(bot.getId(), user.getId());
        SubscriptionEntity awaiting = subscriptionService.markAwaitingActivation(subscription);

        if (tariff.getAccessMode() == TariffAccessMode.PAID_TERM
                && subscriptionCheckoutService.isPaidCheckoutAvailable(bot, tariff)) {
            String receiptHint = bot.isYookassaReceiptEnabled()
                    ? "\n\nДля чека по 54‑ФЗ укажите email командой:\n/billing_email ваш@email.ru"
                    : "";
            InlineKeyboardMarkup payKb = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(InlineKeyboardButton.builder()
                            .text("💳 Оплатить в Telegram")
                            .callbackData(BotNavigationService.CALLBACK_GRP + ":pay:detail:" + awaiting.getId())
                            .build()))
                    .build();
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Спасибо! Согласия приняты. Оплатите подписку на группу, чтобы активировать доступ.%s

                            После успешной оплаты доступ откроется автоматически (обычно в течение минуты).
                            Статус: /subscriptions
                            """.formatted(receiptHint).strip())
                    .replyMarkup(payKb)
                    .build());
        }

        Optional<Long> sc = awaiting.getScopeChatId() != null
                ? Optional.of(awaiting.getScopeChatId()) : Optional.empty();
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

    private ConsentCode nextMissingConsent(Long userId, Long botId, List<ConsentCode> required) {
        for (ConsentCode code : required) {
            if (!consentService.hasAcceptedBotBoundDocument(userId, botId, code)) {
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

                Если доступ ещё не подключён — следуйте следующим сообщениям: там политика и кнопка «Начать» под текстом.
                Можно также нажать /start ещё раз.
                Если доступ уже есть — просто напишите в этот чат.""".formatted(label).trim();
    }

    private static String activeGreeting(SubscriptionEntity subscription) {
        return """
                Подписка активна (%s).
                Внизу — «%s» и «%s». Опишите сон обычным сообщением в этот чат.""".formatted(
                formatExpiry(subscription.getExpiresAt()),
                BotNavigationService.BTN_SETTINGS,
                BotNavigationService.BTN_SUBSCRIPTION);
    }

    private OutgoingMessage mainMenuMessage(Long chatId, String text) {
        return OutgoingMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build();
    }

    /**
     * Дата окончания для текстов «до …». При {@code expiresAt == null} не использовать формулировку «до {дата}»
     * в пользовательских сообщениях — вместо этого «Подписка активна» (правило на будущее; триал всегда с датой).
     */
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
