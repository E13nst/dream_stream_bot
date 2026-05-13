package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.memory.ChatMemoryService;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * /forget_me — отзыв согласий по подпискам этого бота, отмена ваших подписок как владельца на этом боте и полная очистка истории для вас в этом боте.
 */
@Component
public class ForgetMeCommand implements BotCommand {

    private final ChatMemoryService chatMemoryService;
    private final ConsentService consentService;
    private final SubscriptionService subscriptionService;
    private final BotNavigationService botNavigationService;

    public ForgetMeCommand(ChatMemoryService chatMemoryService,
                          ConsentService consentService,
                          SubscriptionService subscriptionService,
                          BotNavigationService botNavigationService) {
        this.chatMemoryService = chatMemoryService;
        this.consentService = consentService;
        this.subscriptionService = subscriptionService;
        this.botNavigationService = botNavigationService;
    }

    @Override
    public String name() {
        return "forget_me";
    }

    @Override
    public boolean appliesIn(ChatScope scope) {
        return scope == ChatScope.PRIVATE;
    }

    @Override
    public Optional<String> menuDescription() {
        return Optional.empty();
    }

    @Override
    public Set<ChatScope> menuScopes() {
        return Set.of(ChatScope.PRIVATE);
    }

    @Override
    public List<OutgoingMessage> handle(CommandContext ctx) {
        Message message = ctx.getMessage();
        UserEntity user = ctx.getUser();
        BotEntity bot = ctx.getBotEntity();
        Long botId = bot != null ? bot.getId() : null;
        Long tgUserId = message.getFrom() != null ? message.getFrom().getId() : null;
        if (user == null || botId == null || tgUserId == null) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("Не удалось выполнить запрос. Напишите /forget_me в чате с ботом после /start.")
                    .build());
        }
        int revoked = consentService.revokeConsentsLinkedToBot(user.getId(), botId);
        int cancelled = subscriptionService.cancelSubscriptionsOwnedByUserOnBot(user.getId(), botId);
        int removed = chatMemoryService.forgetUser(botId, tgUserId);

        String text = "Выполнено: отозвано записей согласий (по этому боту): " + revoked
                + "; отменено подписок как владельца: " + cancelled
                + "; удалено сообщений из памяти диалога: " + removed + ".";
        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
    }
}
