package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Выбор персонального free или trial тарифа с inline-клавиатуры. */
@Component
public class OnboardingTariffPickCallback implements CallbackHandler {

    private final OnboardingService onboardingService;

    public OnboardingTariffPickCallback(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @Override
    public String prefix() {
        return OnboardingService.CALLBACK_TARIFF_PICK;
    }

    @Override
    public List<OutgoingMessage> handle(CallbackContext ctx) {
        if (ctx.getUser() == null || ctx.getBotEntity() == null || ctx.getChatId() == null) {
            return CallbackHandler.silent();
        }
        long tariffId;
        try {
            tariffId = Long.parseLong(ctx.getPayload());
        } catch (NumberFormatException e) {
            return CallbackHandler.silent();
        }
        return onboardingService.pickPersonalTrialOrFreeTariff(
                ctx.getUser(), ctx.getBotEntity(), ctx.getChatId(), tariffId);
    }
}
