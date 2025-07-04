package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.TelegramChatBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@Slf4j
public class BotInitializer {
    private final TelegramChatBot telegramBot;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${bot.webhook.url:}")
    private String webhookUrl;

    @Autowired
    public BotInitializer(TelegramChatBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        log.info("🤖 Initializing Telegram bot...");
        
        if ("prod".equals(activeProfile) && webhookUrl != null && !webhookUrl.trim().isEmpty()) {
            initWebhook();
        } else {
            initLongPolling();
        }
    }

    private void initWebhook() throws TelegramApiException {
        try {
            log.info("🌐 Setting up webhook for bot '{}' at URL: {}", telegramBot.getBotUsername(), webhookUrl);
            
            SetWebhook setWebhook = new SetWebhook();
            setWebhook.setUrl(webhookUrl);
            setWebhook.setAllowedUpdates(java.util.Arrays.asList("message", "callback_query"));
            
            telegramBot.execute(setWebhook);
            log.info("✅ Webhook set successfully for bot '{}'", telegramBot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("❌ Failed to set webhook for bot '{}': {}", telegramBot.getBotUsername(), e.getMessage());
            throw e;
        }
    }

    private void initLongPolling() throws TelegramApiException {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBot);
            log.info("✅ Bot '{}' registered successfully for long polling", telegramBot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register bot '{}': {}", telegramBot.getBotUsername(), e.getMessage());
            throw e;
        }
    }
}
