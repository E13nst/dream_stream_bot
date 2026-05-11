package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;

@Component
public class PrivateReplyNavigationRouter {

    private final CommandDispatcher commandDispatcher;
    private final BotNavigationService botNavigationService;
    private final MessageSender messageSender;
    private final OnboardingService onboardingService;

    public PrivateReplyNavigationRouter(CommandDispatcher commandDispatcher,
                                        BotNavigationService botNavigationService,
                                        MessageSender messageSender,
                                        OnboardingService onboardingService) {
        this.commandDispatcher = commandDispatcher;
        this.botNavigationService = botNavigationService;
        this.messageSender = messageSender;
        this.onboardingService = onboardingService;
    }

    public boolean tryRoute(CommandContext ctx) {
        if (ctx.getMessage() == null || ctx.getMessage().getText() == null || ctx.getChatScope() != ChatScope.PRIVATE) {
            return false;
        }
        String text = ctx.getMessage().getText().trim();
        if (text.isEmpty()) {
            return false;
        }
        if (BotNavigationService.BTN_START.equals(text)) {
            if (ctx.getUser() == null || ctx.getBotEntity() == null || ctx.getChatId() == null) {
                return false;
            }
            messageSender.sendAll(ctx.getSender(),
                    onboardingService.startPersonalAccess(ctx.getUser(), ctx.getBotEntity(), ctx.getChatId()));
            return true;
        }
        if (BotNavigationService.BTN_SETTINGS.equals(text)) {
            return commandDispatcher.dispatch(ctx, "settings", "");
        }
        if (BotNavigationService.BTN_SUBSCRIPTION.equals(text)) {
            return commandDispatcher.dispatch(ctx, "subscriptions", "");
        }
        if (BotNavigationService.BTN_DREAM.equals(text)) {
            messageSender.send(ctx.getSender(), OutgoingMessage.builder()
                    .chatId(ctx.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("🌙 Расскажите сон в одном сообщении — я разберу символы и общий смысл.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
            return true;
        }
        if (BotNavigationService.BTN_DIARY.equals(text)) {
            messageSender.send(ctx.getSender(), OutgoingMessage.builder()
                    .chatId(ctx.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("📖 Дневник снов скоро появится. Пока можно анализировать сны в этом чате.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
            return true;
        }
        return false;
    }
}
