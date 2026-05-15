package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import com.example.dream_stream_bot.service.payment.ReceiptEmailAwaitService;
import com.example.dream_stream_bot.service.payment.SubscriptionCheckoutService;
import com.example.dream_stream_bot.service.payment.YooKassaCheckoutOutgoingFactory;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import com.example.dream_stream_bot.service.telegram.TelegramGroupAdminService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Мастер «Подключить группу»: выбор тарифа, {@code startgroup}, подтверждение, старт онбординга владельца.
 */
@Service
public class GroupLinkWizardService {

    private final GroupLinkWizardStateHolder stateHolder;
    private final SubscriptionTariffService tariffService;
    private final TelegramGroupAdminService telegramGroupAdminService;
    private final OnboardingService onboardingService;
    private final BotNavigationService botNavigationService;
    private final TariffCheckoutPreviewTextBuilder tariffCheckoutPreviewTextBuilder;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCheckoutService subscriptionCheckoutService;
    private final ReceiptEmailAwaitService receiptEmailAwaitService;
    private final YooKassaCheckoutOutgoingFactory yooKassaCheckoutOutgoingFactory;

    public GroupLinkWizardService(GroupLinkWizardStateHolder stateHolder,
                                  SubscriptionTariffService tariffService,
                                  TelegramGroupAdminService telegramGroupAdminService,
                                  OnboardingService onboardingService,
                                  BotNavigationService botNavigationService,
                                  TariffCheckoutPreviewTextBuilder tariffCheckoutPreviewTextBuilder,
                                  SubscriptionRepository subscriptionRepository,
                                  SubscriptionCheckoutService subscriptionCheckoutService,
                                  ReceiptEmailAwaitService receiptEmailAwaitService,
                                  YooKassaCheckoutOutgoingFactory yooKassaCheckoutOutgoingFactory) {
        this.stateHolder = stateHolder;
        this.tariffService = tariffService;
        this.telegramGroupAdminService = telegramGroupAdminService;
        this.onboardingService = onboardingService;
        this.botNavigationService = botNavigationService;
        this.tariffCheckoutPreviewTextBuilder = tariffCheckoutPreviewTextBuilder;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionCheckoutService = subscriptionCheckoutService;
        this.receiptEmailAwaitService = receiptEmailAwaitService;
        this.yooKassaCheckoutOutgoingFactory = yooKassaCheckoutOutgoingFactory;
    }

    public boolean hasActiveSession(Long botId, Long appUserId) {
        return stateHolder.get(botId, appUserId).isPresent();
    }

    public List<OutgoingMessage> openTariffPicker(Long privateChatId, BotEntity bot) {
        List<SubscriptionTariffEntity> tariffs = tariffService.listActiveGroupTariffs(bot.getId());
        if (tariffs.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(privateChatId)
                    .text("Нет доступных групповых тарифов для этого бота.")
                    .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                    .build());
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (SubscriptionTariffEntity t : tariffs) {
            String titlePart = (t.getTitle() == null || t.getTitle().isBlank()) ? "Тариф" : t.getTitle().trim();
            String rub = formatRub(t.getPriceAmountMinor());
            String label = truncateButton(titlePart + " — " + rub + " ₽");
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData(BotNavigationService.CALLBACK_GRP + ":pick:" + t.getId())
                    .build()));
        }
        rows.add(List.of(InlineKeyboardButton.builder()
                .text("⬅ Назад")
                .callbackData(BotNavigationService.CALLBACK_NAV + ":subscriptions")
                .build()));
        return List.of(OutgoingMessage.builder()
                .chatId(privateChatId)
                .text("Выберите тариф для группы:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build());
    }

    public List<OutgoingMessage> onPickTariff(Long privateChatId, BotEntity bot, UserEntity user, long tariffId) {
        if (user == null || bot == null) {
            return List.of();
        }
        SubscriptionTariffEntity tariff = tariffService.requireForBot(bot.getId(), tariffId);
        if (tariff.getScope() != TariffScope.GROUP) {
            return List.of(OutgoingMessage.of(privateChatId, "Это не групповой тариф."));
        }
        String uname = bot.getUsername() == null || bot.getUsername().isBlank() ? "bot" : bot.getUsername().trim();
        String startGroupUrl = "https://t.me/" + uname + "?startgroup=link_group_" + user.getTelegramId();
        stateHolder.startAwaitingGroupPick(bot.getId(), user.getId(), tariffId);
        String preview = tariffCheckoutPreviewTextBuilder.buildCheckoutPreviewText(tariff, true,
                TariffCheckoutPreviewTextBuilder.PayFooter.GROUP_CONNECT_NEXT);
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("🔗 Выбрать группу")
                        .url(startGroupUrl)
                        .build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("⬅ К тарифам")
                        .callbackData(BotNavigationService.CALLBACK_GRP + ":begin")
                        .build(),
                        InlineKeyboardButton.builder()
                                .text("❌ Отмена")
                                .callbackData(BotNavigationService.CALLBACK_GRP + ":cancel")
                                .build()))
                .build();
        return List.of(OutgoingMessage.builder()
                .chatId(privateChatId)
                .text(preview)
                .replyMarkup(kb)
                .build());
    }

    public List<OutgoingMessage> onCancel(Long privateChatId, BotEntity bot, UserEntity user) {
        if (user == null || bot == null) {
            return List.of();
        }
        stateHolder.clear(bot.getId(), user.getId());
        return List.of(OutgoingMessage.builder()
                .chatId(privateChatId)
                .text("Подключение группы отменено.")
                .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                .build());
    }

    /**
     * Вызов из группы по {@code /start link_group_<telegramUserId>}.
     */
    public List<OutgoingMessage> handleLinkGroupStartFromGroup(Long privateChatId, BotEntity bot, UserEntity user,
                                                               long telegramUserIdFromPayload, Long groupChatId) {
        if (user == null || bot == null || !user.getTelegramId().equals(telegramUserIdFromPayload)) {
            return List.of();
        }
        Optional<GroupLinkWizardStateHolder.Session> sessionOpt = stateHolder.get(bot.getId(), user.getId());
        if (sessionOpt.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(privateChatId)
                    .text("Сессия привязки группы сброшена или устарела. Откройте /subscriptions → «Подключить группу» и начните заново.")
                    .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                    .build());
        }
        GroupLinkWizardStateHolder.Session session = sessionOpt.get();
        if (session.getStep() != GroupLinkWizardStateHolder.Step.AWAITING_GROUP_PICK) {
            return List.of(OutgoingMessage.of(privateChatId,
                    "Сначала завершите подтверждение в личке или нажмите «Другая группа»."));
        }
        String title = telegramGroupAdminService.getChatTitle(bot, groupChatId)
                .orElse("Группа #" + groupChatId);
        return applySelectionAndOfferConfirm(privateChatId, bot, user, session.getTariffId(), groupChatId, title);
    }

    private List<OutgoingMessage> applySelectionAndOfferConfirm(Long privateChatId, BotEntity bot, UserEntity user,
                                                                long tariffId, Long scopeChatId, String title) {
        Optional<String> elevated = telegramGroupAdminService.resolveElevatedMemberStatus(bot, scopeChatId,
                user.getTelegramId());
        if (elevated.isEmpty()) {
            Optional<String> anyRole = telegramGroupAdminService.getChatMemberRole(bot, scopeChatId,
                    user.getTelegramId());
            if (anyRole.isEmpty()) {
                return List.of(OutgoingMessage.of(privateChatId,
                        "Не удалось проверить группу. Убедитесь, что бот @" + safeUsername(bot)
                                + " добавлен в группу и попробуйте снова."));
            }
            return List.of(OutgoingMessage.of(privateChatId,
                    "Вы не являетесь администратором этой группы. Выберите группу, где у вас такая роль, "
                            + "кнопкой «Выбрать группу»."));
        }
        String role = elevated.get();
        String roleRu = ("creator".equals(role) || "owner".equals(role)) ? "Создатель" : "Администратор";
        stateHolder.setConfirming(bot.getId(), user.getId(), tariffId, scopeChatId);
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("✅ Продолжить").callbackData(BotNavigationService.CALLBACK_GRP + ":go").build(),
                        InlineKeyboardButton.builder().text("🔄 Другая группа").callbackData(BotNavigationService.CALLBACK_GRP + ":retry").build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("❌ Отмена").callbackData(BotNavigationService.CALLBACK_GRP + ":cancel").build()
                ))
                .build();
        String text = """
                Найдена группа: %s
                Ваша роль: %s (%s)

                Продолжить?""".formatted(title, roleRu, role);
        return List.of(OutgoingMessage.builder()
                .chatId(privateChatId)
                .text(text.strip())
                .replyMarkup(kb)
                .build());
    }

    public List<OutgoingMessage> onConfirmGo(Long privateChatId, BotEntity bot, UserEntity user) {
        Optional<GroupLinkWizardStateHolder.Session> s = stateHolder.get(bot.getId(), user.getId());
        if (s.isEmpty() || s.get().getStep() != GroupLinkWizardStateHolder.Step.CONFIRMING_GROUP
                || s.get().getDraftScopeChatId() == null) {
            return List.of(OutgoingMessage.of(privateChatId, "Сессия устарела. Начните с экрана /subscriptions."));
        }
        long tariffId = s.get().getTariffId();
        Long scopeChatId = s.get().getDraftScopeChatId();
        tariffService.requireForBot(bot.getId(), tariffId);
        stateHolder.clear(bot.getId(), user.getId());
        return onboardingService.startGroupOwnerWithChosenTariff(bot, privateChatId, user, scopeChatId, tariffId);
    }

    /**
     * Экран условий перед созданием платежа по групповой подписке (после согласий).
     */
    public List<OutgoingMessage> groupPayDetailPreview(Long privateChatId, BotEntity bot, UserEntity user,
                                                       long subscriptionId) {
        if (user == null || bot == null) {
            return List.of();
        }
        Optional<SubscriptionEntity> subOpt = loadGroupSubscriptionForYooKassa(bot.getId(), user.getId(), subscriptionId);
        if (subOpt.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(privateChatId)
                    .text("Оплата недоступна для этой подписки. Откройте /subscriptions.")
                    .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                    .build());
        }
        SubscriptionTariffEntity tariff = tariffService.requireForBot(bot.getId(), subOpt.get().getTariffId());
        if (!subscriptionCheckoutService.isPaidCheckoutAvailable(bot, tariff)) {
            return List.of(OutgoingMessage.builder()
                    .chatId(privateChatId)
                    .text("Оплата через ЮKassa для этого бота не настроена. Обратитесь в поддержку.")
                    .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                    .build());
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("💳 Оплатить через ЮKassa")
                        .callbackData(BotNavigationService.CALLBACK_GRP + ":pay:open:" + subscriptionId)
                        .build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("⬅ К подписке")
                        .callbackData(botNavigationService.navPayload("subscriptions"))
                        .build()))
                .build();
        return List.of(OutgoingMessage.builder()
                .chatId(privateChatId)
                .text(tariffCheckoutPreviewTextBuilder.buildCheckoutPreviewText(tariff, true,
                        TariffCheckoutPreviewTextBuilder.PayFooter.YOOKASSA_HINT))
                .replyMarkup(kb)
                .build());
    }

    /**
     * Создание платежа ЮKassa по групповой подписке; новое сообщение со ссылкой и «Я оплатил».
     */
    public List<OutgoingMessage> groupPayOpenCheckout(Long privateChatId, BotEntity bot, UserEntity user,
                                                      long subscriptionId) {
        if (user == null || bot == null) {
            return List.of();
        }
        Optional<SubscriptionEntity> subOpt = loadGroupSubscriptionForYooKassa(bot.getId(), user.getId(), subscriptionId);
        if (subOpt.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(privateChatId)
                    .text("Оплата недоступна для этой подписки. Откройте /subscriptions.")
                    .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                    .build());
        }
        SubscriptionTariffEntity tariff = tariffService.requireForBot(bot.getId(), subOpt.get().getTariffId());
        if (!subscriptionCheckoutService.isPaidCheckoutAvailable(bot, tariff)) {
            return List.of(OutgoingMessage.builder()
                    .chatId(privateChatId)
                    .text("Оплата через ЮKassa для этого бота не настроена. Обратитесь в поддержку.")
                    .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                    .build());
        }
        if (Boolean.TRUE.equals(bot.isYookassaReceiptEnabled())) {
            String billing = user.getBillingEmail();
            if (billing == null || billing.isBlank()) {
                receiptEmailAwaitService.startAwaitingGroup(bot.getId(), user.getId(), subscriptionId);
                return receiptEmailAwaitService.promptMessages(privateChatId, null, true);
            }
        }
        try {
            SubscriptionCheckoutService.CheckoutResult result = subscriptionCheckoutService
                    .createCheckoutForGroupSubscription(bot.getId(), user.getId(), subscriptionId);
            return yooKassaCheckoutOutgoingFactory.groupSubscriptionPaymentReady(privateChatId, null, result);
        } catch (RuntimeException e) {
            return List.of(OutgoingMessage.builder()
                    .chatId(privateChatId)
                    .text("Не удалось создать платёж: " + e.getMessage())
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboardRow(List.of(InlineKeyboardButton.builder()
                                    .text("⬅ К подписке")
                                    .callbackData(botNavigationService.navPayload("subscriptions"))
                                    .build()))
                            .build())
                    .build());
        }
    }

    private Optional<SubscriptionEntity> loadGroupSubscriptionForYooKassa(long botId, long ownerUserId,
                                                                          long subscriptionId) {
        Optional<SubscriptionEntity> base = subscriptionRepository.findById(subscriptionId)
                .filter(s -> s.getBotId().equals(botId))
                .filter(s -> s.getOwnerUserId().equals(ownerUserId))
                .filter(s -> s.getScopeChatId() != null)
                .filter(s -> s.getStatus() == SubscriptionStatus.AWAITING_ACTIVATION);
        if (base.isEmpty()) {
            return Optional.empty();
        }
        SubscriptionEntity s = base.get();
        SubscriptionTariffEntity t = tariffService.requireForBot(botId, s.getTariffId());
        if (t.getScope() != TariffScope.GROUP
                || t.getAccessMode() != TariffAccessMode.PAID_TERM
                || t.getPriceAmountMinor() == null
                || t.getPriceAmountMinor() <= 0) {
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public List<OutgoingMessage> onConfirmRetry(Long privateChatId, BotEntity bot, UserEntity user) {
        Optional<GroupLinkWizardStateHolder.Session> s = stateHolder.get(bot.getId(), user.getId());
        if (s.isEmpty()) {
            return List.of(OutgoingMessage.of(privateChatId, "Сессия сброшена. Откройте /subscriptions."));
        }
        long tariffId = s.get().getTariffId();
        stateHolder.clear(bot.getId(), user.getId());
        stateHolder.startAwaitingGroupPick(bot.getId(), user.getId(), tariffId);
        return onPickTariff(privateChatId, bot, user, tariffId);
    }

    /** Напоминание при обычном тексте в активной сессии мастера (без перехвата команд — см. AssistantBot). */
    public Optional<List<OutgoingMessage>> tryPlainTextReminder(Long privateChatId, BotEntity bot, UserEntity user,
                                                                 String text) {
        if (user == null || text == null || text.isBlank()) {
            return Optional.empty();
        }
        Optional<GroupLinkWizardStateHolder.Session> sessionOpt = stateHolder.get(bot.getId(), user.getId());
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        GroupLinkWizardStateHolder.Session session = sessionOpt.get();
        if (session.getStep() == GroupLinkWizardStateHolder.Step.CONFIRMING_GROUP) {
            String t = text.trim();
            if ("❌ Отмена".equals(t) || "Отмена".equalsIgnoreCase(t)) {
                return Optional.of(onCancel(privateChatId, bot, user));
            }
            return Optional.of(List.of(OutgoingMessage.of(privateChatId,
                    "Используйте кнопки под сообщением с подтверждением: «Продолжить», «Другая группа» или «Отмена».")));
        }
        if (session.getStep() != GroupLinkWizardStateHolder.Step.AWAITING_GROUP_PICK) {
            return Optional.empty();
        }
        String t = text.trim();
        if ("❌ Отмена".equals(t) || "Отмена".equalsIgnoreCase(t)) {
            return Optional.of(onCancel(privateChatId, bot, user));
        }
        return Optional.of(List.of(OutgoingMessage.of(privateChatId,
                "Используйте кнопку «🔗 Выбрать группу» в сообщении выше, затем подтвердите выбор в этом чате.")));
    }

    private static String safeUsername(BotEntity bot) {
        return bot.getUsername() == null || bot.getUsername().isBlank() ? "…" : bot.getUsername().trim();
    }

    private static String truncateButton(String title) {
        if (title == null || title.isBlank()) {
            return "Тариф";
        }
        return title.length() <= 64 ? title : title.substring(0, 61) + "…";
    }

    /** Формат цены как на шаге «Выберите тариф для оплаты» (копейки → рубли). */
    private static String formatRub(Long minor) {
        if (minor == null) {
            return "?";
        }
        return BigDecimal.valueOf(minor).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
