package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.telegram.TelegramGroupAdminService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;
import java.util.Optional;

/**
 * Отправка владельцем приглашения в группу с deep link на согласие участника.
 */
@Service
public class GroupOwnerInviteService {

    private final SubscriptionService subscriptionService;
    private final MessageSender messageSender;
    private final TelegramGroupAdminService telegramGroupAdminService;

    public GroupOwnerInviteService(SubscriptionService subscriptionService,
                                   MessageSender messageSender,
                                   TelegramGroupAdminService telegramGroupAdminService) {
        this.subscriptionService = subscriptionService;
        this.messageSender = messageSender;
        this.telegramGroupAdminService = telegramGroupAdminService;
    }

    /**
     * Доставляет приглашение в {@code scope_chat_id} подписки (side-effect через {@link MessageSender#trySend})
     * и возвращает только сообщения в личку владельцу.
     */
    public List<OutgoingMessage> sendInviteToGroupAndNotifyOwner(Long privateChatId,
                                                                 BotEntity bot,
                                                                 UserEntity owner,
                                                                 long subscriptionId,
                                                                 AbsSender telegramClient) {
        if (privateChatId == null || bot == null || owner == null || telegramClient == null) {
            return List.of();
        }
        String username = bot.getUsername() == null ? "" : bot.getUsername().trim();
        if (username.isBlank()) {
            return List.of(OutgoingMessage.of(privateChatId,
                    "У бота не задан username — нельзя сформировать ссылку для участников. Настройте username в BotFather."));
        }
        Optional<SubscriptionEntity> subOpt = subscriptionService.findById(subscriptionId);
        if (subOpt.isEmpty()) {
            return List.of(OutgoingMessage.of(privateChatId, "Подписка не найдена."));
        }
        SubscriptionEntity sub = subOpt.get();
        if (!sub.getBotId().equals(bot.getId()) || !sub.getOwnerUserId().equals(owner.getId())) {
            return List.of(OutgoingMessage.of(privateChatId, "Недостаточно прав для этой операции."));
        }
        if (!subscriptionService.isGroupSubscriptionByTariff(sub.getTariffId()) || sub.getScopeChatId() == null) {
            return List.of(OutgoingMessage.of(privateChatId, "Это не групповая подписка или чат не привязан."));
        }
        Long scopeChatId = sub.getScopeChatId();
        String title = telegramGroupAdminService.getChatTitle(bot, scopeChatId)
                .orElse("Группа #" + scopeChatId);
        String consentUrl = "https://t.me/" + username + "?start=group_consent_" + sub.getId();
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("▶ Подтвердить и начать")
                        .url(consentUrl)
                        .build()))
                .build();
        String groupText = """
                👋 Привет! Теперь я помогаю в этом чате.

                Чтобы общаться со мной, подтвердите согласие — это займёт 10 секунд:"""
                .strip();
        OutgoingMessage toGroup = OutgoingMessage.builder()
                .chatId(scopeChatId)
                .text(groupText)
                .replyMarkup(kb)
                .build();
        boolean ok = messageSender.trySend(telegramClient, toGroup);
        if (!ok) {
            return List.of(OutgoingMessage.of(privateChatId,
                    """
                            ❌ Не удалось отправить сообщение в группу %s.
                            Убедитесь, что бот всё ещё является участником группы."""
                            .formatted(title).strip()));
        }
        return List.of(OutgoingMessage.of(privateChatId,
                "✅ Приглашение отправлено в группу " + title));
    }
}
