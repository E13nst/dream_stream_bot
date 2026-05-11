package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;

/**
 * Обработчик callback {@code consent_accept:<documentId>}.
 * Регистрирует принятие документа и продолжает онбординг.
 */
@Component
public class ConsentAcceptCallback implements CallbackHandler {

    private final OnboardingService onboardingService;

    public ConsentAcceptCallback(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @Override
    public String prefix() {
        return OnboardingService.CALLBACK_ACCEPT;
    }

    @Override
    public List<OutgoingMessage> handle(CallbackContext ctx) {
        if (ctx.getUser() == null || ctx.getBotEntity() == null || ctx.getChatId() == null) {
            return CallbackHandler.silent();
        }
        Long documentId;
        try {
            documentId = Long.parseLong(ctx.getPayload());
        } catch (NumberFormatException e) {
            return CallbackHandler.silent();
        }
        CallbackQuery cq = ctx.getCallbackQuery();
        Integer telegramMessageId = cq != null && cq.getMessage() != null
                ? cq.getMessage().getMessageId() : null;

        return onboardingService.recordAcceptance(
                ctx.getUser(), ctx.getBotEntity(), ctx.getChatId(), documentId, telegramMessageId);
    }
}
