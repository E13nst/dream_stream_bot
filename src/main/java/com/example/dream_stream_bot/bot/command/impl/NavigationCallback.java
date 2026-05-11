package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NavigationCallback implements CallbackHandler {

    private final BotNavigationService botNavigationService;

    public NavigationCallback(BotNavigationService botNavigationService) {
        this.botNavigationService = botNavigationService;
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

        return switch (ctx.getPayload()) {
            case "subscriptions" -> List.of(message(chatId, "Откройте раздел подписки кнопкой «💎 Подписка» или командой /subscriptions."));
            case "referral" -> List.of(message(chatId, "Реферальная ссылка доступна по команде /referral."));
            case "forget_last" -> List.of(message(chatId, "Чтобы удалить последний обмен, используйте /forget_last."));
            case "forget_me" -> List.of(message(chatId, "Для полного удаления данных используйте /forget_me."));
            case "main" -> List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Главное меню готово. Выберите действие на нижней клавиатуре.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
            default -> CallbackHandler.silent();
        };
    }

    private OutgoingMessage message(Long chatId, String text) {
        return OutgoingMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build();
    }
}
