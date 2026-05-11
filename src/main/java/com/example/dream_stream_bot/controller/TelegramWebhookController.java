package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.bot.AbstractTelegramBot;
import com.example.dream_stream_bot.bot.error.BotUpdateErrorHandler;
import com.example.dream_stream_bot.config.properties.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class TelegramWebhookController {
    private static final String TELEGRAM_SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final Map<String, AbstractTelegramBot> botRegistry;
    private final TelegramProperties telegramProperties;
    private final BotUpdateErrorHandler errorHandler;

    public TelegramWebhookController(Map<String, AbstractTelegramBot> botRegistry,
                                     TelegramProperties telegramProperties,
                                     BotUpdateErrorHandler errorHandler) {
        this.botRegistry = botRegistry;
        this.telegramProperties = telegramProperties;
        this.errorHandler = errorHandler;
    }

    @PostMapping("/{botUsername}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String botUsername,
            @RequestHeader(value = TELEGRAM_SECRET_HEADER, required = false) String secretHeader,
            @RequestBody Update update
    ) {
        String expected = telegramProperties.getWebhook().getSecretToken();
        expected = expected != null ? expected.trim() : "";
        if (!expected.isBlank()) {
            String got = secretHeader != null ? secretHeader.trim() : "";
            if (!expected.equals(got)) {
                log.warn("❌ Webhook rejected: invalid secret token for bot: {}", botUsername);
                return ResponseEntity.status(401).body("Unauthorized");
            }
        }

        AbstractTelegramBot bot = botRegistry.get(botUsername);
        if (bot == null) {
            log.warn("❌ No bot found for username: {}", botUsername);
            return ResponseEntity.notFound().build();
        }

        errorHandler.handle(bot, botUsername, update, bot::onUpdateReceived);
        log.info("✅ Update routed to bot: {}", botUsername);
        return ResponseEntity.ok("OK");
    }
}
