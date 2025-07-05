package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.bot.TelegramChatBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class TelegramWebhookController {

    private final TelegramChatBot telegramBot;

    @Autowired
    public TelegramWebhookController(TelegramChatBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @PostMapping
    public ResponseEntity<String> handleTelegramWebhook(@RequestBody Update update) {
        try {
            log.info("📨 Webhook update received | ID: {} | Type: {}", 
                update.getUpdateId(), getUpdateType(update));
            
            telegramBot.handleUpdateAsync(update);
            
            log.debug("✅ Webhook update processed successfully | ID: {}", update.getUpdateId());
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ Error processing webhook update | ID: {} | Error: {}", 
                update.getUpdateId(), e.getMessage());
            return ResponseEntity.internalServerError().body("Error processing update");
        }
    }

    @PostMapping("/telegram")
    public ResponseEntity<String> handleTelegramWebhookLegacy(@RequestBody Update update) {
        return handleTelegramWebhook(update);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.debug("🏥 Health check requested");
        return ResponseEntity.ok("Telegram webhook is healthy");
    }

    private String getUpdateType(Update update) {
        if (update.hasMessage()) return "message";
        if (update.hasCallbackQuery()) return "callback_query";
        if (update.hasInlineQuery()) return "inline_query";
        if (update.hasChosenInlineQuery()) return "chosen_inline_query";
        if (update.hasChannelPost()) return "channel_post";
        if (update.hasEditedMessage()) return "edited_message";
        if (update.hasEditedChannelPost()) return "edited_channel_post";
        if (update.hasShippingQuery()) return "shipping_query";
        if (update.hasPreCheckoutQuery()) return "pre_checkout_query";
        if (update.hasPoll()) return "poll";
        if (update.hasPollAnswer()) return "poll_answer";
        if (update.hasMyChatMember()) return "my_chat_member";
        if (update.hasChatMember()) return "chat_member";
        if (update.hasChatJoinRequest()) return "chat_join_request";
        return "unknown";
    }
} 