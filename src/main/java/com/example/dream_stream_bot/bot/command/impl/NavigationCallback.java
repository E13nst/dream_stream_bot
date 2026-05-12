package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.command.CommandDispatcher;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class NavigationCallback implements CallbackHandler {

    private final BotNavigationService botNavigationService;
    private final CommandDispatcher commandDispatcher;

    public NavigationCallback(BotNavigationService botNavigationService,
                              CommandDispatcher commandDispatcher) {
        this.botNavigationService = botNavigationService;
        this.commandDispatcher = commandDispatcher;
    }

    @Override
    public String prefix() {
        return BotNavigationService.CALLBACK_NAV;
    }

    @Override
    public List<OutgoingMessage> handle(CallbackContext ctx) {
        Long chatId = ctx.getChatId();
        if (chatId == null) {
            return CallbackHandler.silent();
        }

        String payload = ctx.getPayload();
        return switch (payload) {
            case "subscriptions", "referral", "forget_last", "forget_me", "settings" -> {
                dispatchSlashEquivalent(ctx, payload);
                yield CallbackHandler.silent();
            }
            case "main" -> List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text(("Главное меню. Внизу — «%s» и «%s». Сон можно описать обычным сообщением в этот чат,"
                            + " когда доступ уже открыт.")
                            .formatted(BotNavigationService.BTN_SETTINGS, BotNavigationService.BTN_SUBSCRIPTION))
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
            default -> CallbackHandler.silent();
        };
    }

    /** Тот же обработчик, что для slash-команды {@code /<commandName>}. */
    private void dispatchSlashEquivalent(CallbackContext ctx, String commandName) {
        CallbackQuery cq = ctx.getCallbackQuery();
        if (cq == null || cq.getMessage() == null || !(cq.getMessage() instanceof Message msg)) {
            return;
        }
        Update update = new Update();
        update.setCallbackQuery(cq);
        ChatScope scope = ChatScope.fromMessageType(
                msg.isUserMessage(),
                msg.isGroupMessage(),
                msg.isSuperGroupMessage(),
                msg.isChannelMessage());
        CommandContext cmdCtx = new CommandContext(
                update,
                msg,
                ctx.getBotEntity(),
                ctx.getSender(),
                ctx.getBotUsername(),
                ctx.getUser(),
                null,
                null,
                scope);
        commandDispatcher.dispatch(cmdCtx, commandName, "");
    }
}
