package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.bot.BotFactory;
import com.example.dream_stream_bot.bot.AbstractTelegramBot;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.telegram.StickerPackService;
import com.example.dream_stream_bot.service.telegram.StickerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BotInitializer {
    private final BotService botService;
    private final MessageHandlerService messageHandlerService;
    private final UserStateService userStateService;
    private final StickerPackService stickerPackService;
    private final StickerService stickerService;
    private final Map<String, AbstractTelegramBot> botRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    public BotInitializer(BotService botService, MessageHandlerService messageHandlerService, 
                         UserStateService userStateService, StickerPackService stickerPackService,
                         StickerService stickerService) {
        this.botService = botService;
        this.messageHandlerService = messageHandlerService;
        this.userStateService = userStateService;
        this.stickerPackService = stickerPackService;
        this.stickerService = stickerService;
    }

    @Bean
    public Map<String, AbstractTelegramBot> botRegistry() {
        return botRegistry;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        log.info("🤖 Initializing all Telegram bots...");
        
        // Проверяем зависимости
        log.info("🔍 Проверяем зависимости:");
        log.info("  - BotService: {}", botService != null ? "✅" : "❌");
        log.info("  - MessageHandlerService: {}", messageHandlerService != null ? "✅" : "❌");
        log.info("  - UserStateService: {}", userStateService != null ? "✅" : "❌");
        log.info("  - StickerPackService: {}", stickerPackService != null ? "✅" : "❌");
        log.info("  - StickerService: {}", stickerService != null ? "✅" : "❌");
        
        List<BotEntity> bots = botService.getAllBots();
        log.info("📋 Найдено ботов в базе: {}", bots.size());
        
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        for (BotEntity bot : bots) {
            log.info("🔍 Обрабатываем бота: username='{}', type='{}', active={}", 
                    bot.getUsername(), bot.getType(), bot.getIsActive());
            
            if (Boolean.TRUE.equals(bot.getIsActive())) {
                try {
                    AbstractTelegramBot telegramBot = BotFactory.createBot(bot, messageHandlerService, userStateService, stickerPackService, stickerService);
                    telegramBotsApi.registerBot(telegramBot);
                    botRegistry.put(bot.getUsername(), telegramBot);
                    log.info("✅ Bot '{}' registered successfully (type: {})", bot.getUsername(), bot.getType());
                } catch (Exception e) {
                    log.error("❌ Ошибка при создании бота '{}' (type: {}): {}", 
                            bot.getUsername(), bot.getType(), e.getMessage(), e);
                    throw e;
                }
            } else {
                log.info("⏸️ Bot '{}' пропущен (неактивен)", bot.getUsername());
            }
        }
    }
}
