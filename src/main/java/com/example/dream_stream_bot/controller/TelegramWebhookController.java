package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.bot.AbstractTelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${telegram.webhook.secret-token:}")
    private String webhookSecretToken;

    @Autowired
    public TelegramWebhookController(Map<String, AbstractTelegramBot> botRegistry) {
        this.botRegistry = botRegistry;
    }

    @PostMapping("/{botUsername}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String botUsername,
            @RequestHeader(value = TELEGRAM_SECRET_HEADER, required = false) String secretHeader,
            @RequestBody Update update
    ) {
        String expected = webhookSecretToken != null ? webhookSecretToken.trim() : "";
        if (!expected.isBlank()) {
            String got = secretHeader != null ? secretHeader.trim() : "";
            if (!expected.equals(got)) {
                log.warn("❌ Webhook rejected: invalid secret token for bot: {}", botUsername);
                return ResponseEntity.status(401).body("Unauthorized");
            }
        }

        AbstractTelegramBot bot = botRegistry.get(botUsername);
        if (bot != null) {
            bot.onUpdateReceived(update);
            log.info("✅ Update routed to bot: {}", botUsername);
            return ResponseEntity.ok("OK");
        } else {
            log.warn("❌ No bot found for username: {}", botUsername);
            return ResponseEntity.notFound().build();
        }
    }
} 