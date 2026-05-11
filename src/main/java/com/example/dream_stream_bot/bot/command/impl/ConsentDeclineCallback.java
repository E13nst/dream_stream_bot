package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Обработчик callback {@code consent_decline:<code>}.
 * Возвращает мягкое сообщение, что без согласия доступ невозможен.
 */
@Component
public class ConsentDeclineCallback implements CallbackHandler {

    private final OnboardingService onboardingService;

    public ConsentDeclineCallback(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @Override
    public String prefix() {
        return OnboardingService.CALLBACK_DECLINE;
    }

    @Override
    public List<OutgoingMessage> handle(CallbackContext ctx) {
        if (ctx.getChatId() == null) {
            return CallbackHandler.silent();
        }
        ConsentCode code;
        try {
            code = ConsentCode.valueOf(ctx.getPayload());
        } catch (IllegalArgumentException e) {
            code = ConsentCode.OFFER;
        }
        return onboardingService.recordDecline(ctx.getChatId(), code);
    }
}
