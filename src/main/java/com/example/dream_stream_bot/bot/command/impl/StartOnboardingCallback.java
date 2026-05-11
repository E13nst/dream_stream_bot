package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Starts personal access onboarding after the user has read the bot intro.
 */
@Component
public class StartOnboardingCallback implements CallbackHandler {

    private final OnboardingService onboardingService;

    public StartOnboardingCallback(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @Override
    public String prefix() {
        return OnboardingService.CALLBACK_START;
    }

    @Override
    public List<OutgoingMessage> handle(CallbackContext ctx) {
        if (ctx.getUser() == null || ctx.getBotEntity() == null || ctx.getChatId() == null) {
            return CallbackHandler.silent();
        }
        return onboardingService.startPersonalAccess(ctx.getUser(), ctx.getBotEntity(), ctx.getChatId());
    }
}
