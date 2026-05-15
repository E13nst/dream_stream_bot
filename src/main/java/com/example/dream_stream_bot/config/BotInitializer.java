package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.bot.AbstractTelegramBot;
import com.example.dream_stream_bot.bot.BotFactory;
import com.example.dream_stream_bot.bot.EditedMessageHandler;
import com.example.dream_stream_bot.bot.command.CallbackDispatcher;
import com.example.dream_stream_bot.bot.command.CommandDispatcher;
import com.example.dream_stream_bot.bot.command.PrivateReplyNavigationRouter;
import com.example.dream_stream_bot.bot.error.BotUpdateErrorHandler;
import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.config.properties.TelegramProperties;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.access.AccessGate;
import com.example.dream_stream_bot.service.access.GatingDedup;
import com.example.dream_stream_bot.service.payment.ReceiptEmailAwaitService;
import com.example.dream_stream_bot.service.subscription.GroupLinkWizardService;
import com.example.dream_stream_bot.service.telegram.BotMenuSyncService;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.TelegramBotApiService;
import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BotInitializer {
    private final BotService botService;
    private final MessageHandlerService messageHandlerService;
    private final UserStateService userStateService;
    private final TelegramBotApiService telegramBotApiService;
    private final UserService userService;
    private final MessageSender messageSender;
    private final CommandDispatcher commandDispatcher;
    private final CallbackDispatcher callbackDispatcher;
    private final BotUpdateErrorHandler errorHandler;
    private final EditedMessageHandler editedMessageHandler;
    private final AccessGate accessGate;
    private final GatingDedup gatingDedup;
    private final PrivateReplyNavigationRouter privateReplyNavigationRouter;
    private final GroupLinkWizardService groupLinkWizardService;
    private final ReceiptEmailAwaitService receiptEmailAwaitService;
    private final TelegramProperties telegramProperties;
    private final BotMenuSyncService botMenuSyncService;
    private final Map<String, AbstractTelegramBot> botRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    public BotInitializer(BotService botService, MessageHandlerService messageHandlerService,
                          UserStateService userStateService,
                          TelegramBotApiService telegramBotApiService,
                          UserService userService,
                          MessageSender messageSender,
                          CommandDispatcher commandDispatcher,
                          CallbackDispatcher callbackDispatcher,
                          BotUpdateErrorHandler errorHandler,
                          EditedMessageHandler editedMessageHandler,
                          AccessGate accessGate,
                          GatingDedup gatingDedup,
                          PrivateReplyNavigationRouter privateReplyNavigationRouter,
                          GroupLinkWizardService groupLinkWizardService,
                          ReceiptEmailAwaitService receiptEmailAwaitService,
                          TelegramProperties telegramProperties,
                          BotMenuSyncService botMenuSyncService) {
        this.botService = botService;
        this.messageHandlerService = messageHandlerService;
        this.userStateService = userStateService;
        this.telegramBotApiService = telegramBotApiService;
        this.userService = userService;
        this.messageSender = messageSender;
        this.commandDispatcher = commandDispatcher;
        this.callbackDispatcher = callbackDispatcher;
        this.errorHandler = errorHandler;
        this.editedMessageHandler = editedMessageHandler;
        this.accessGate = accessGate;
        this.gatingDedup = gatingDedup;
        this.privateReplyNavigationRouter = privateReplyNavigationRouter;
        this.groupLinkWizardService = groupLinkWizardService;
        this.receiptEmailAwaitService = receiptEmailAwaitService;
        this.telegramProperties = telegramProperties;
        this.botMenuSyncService = botMenuSyncService;
    }

    @Bean
    public Map<String, AbstractTelegramBot> botRegistry() {
        return botRegistry;
    }

    @EventListener({ApplicationReadyEvent.class})
    public void init() {
        log.info("🚀 Application is ready! Starting bot initialization...");
        
        try {
            initializeBots();
        } catch (Exception e) {
            log.error("❌ Critical error during bot initialization", e);
            // Не прерываем запуск приложения, только логируем ошибку
        }
    }
    
    private void initializeBots() throws TelegramApiException {
        log.info("🤖 Initializing all Telegram bots...");
        
        // Проверяем зависимости
        log.info("🔍 Checking dependencies:");
        log.info("  - BotService: {}", botService != null ? "✅" : "❌");
        log.info("  - MessageHandlerService: {}", messageHandlerService != null ? "✅" : "❌");
        log.info("  - UserStateService: {}", userStateService != null ? "✅" : "❌");
        
        try {
            List<BotEntity> bots = botService.getAllBots();
            log.info("📋 Found {} bots in database", bots.size());

            String mode = telegramProperties.getDeliveryMode() == null
                    ? "long-polling"
                    : telegramProperties.getDeliveryMode().trim().toLowerCase();
            log.info("🧭 Telegram delivery mode: {}", mode);

            TelegramBotsApi telegramBotsApi = null;
            if (telegramProperties.isLongPolling()) {
                telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            }

            int successCount = 0;
            int errorCount = 0;
            
            for (BotEntity bot : bots) {
                log.info("🔍 Processing bot: username='{}', type='{}', active={}, miniapp={}",
                        bot.getUsername(), bot.getType(), bot.getIsActive(), bot.getMiniapp());

                if (Boolean.TRUE.equals(bot.getIsActive())) {
                    try {
                        var beforeInfo = telegramBotApiService.getWebhookInfo(bot);
                        if (beforeInfo.isPresent()) {
                            var info = beforeInfo.get();
                            log.info("📡 Bot '{}' current delivery: {} (pendingUpdates={}, lastError={})",
                                    bot.getUsername(),
                                    info.describeDelivery(),
                                    info.pendingUpdateCount(),
                                    info.lastErrorMessage());
                        } else {
                            log.info("📡 Bot '{}' current delivery: unknown(unavailable)", bot.getUsername());
                        }

                        AbstractTelegramBot telegramBot = BotFactory.createBot(
                                bot, botService, messageHandlerService, userStateService, userService,
                                messageSender, commandDispatcher, callbackDispatcher,
                                errorHandler, editedMessageHandler,
                                accessGate, gatingDedup, privateReplyNavigationRouter, groupLinkWizardService,
                                receiptEmailAwaitService);
                        botRegistry.put(bot.getUsername(), telegramBot);

                        if (telegramProperties.isLongPolling()) {
                            boolean deleted = telegramBotApiService.deleteWebhook(bot);
                            if (telegramBotsApi == null) {
                                throw new IllegalStateException("TelegramBotsApi is not initialized for long-polling mode");
                            }
                            telegramBotsApi.registerBot(telegramBot);
                            var afterInfo = telegramBotApiService.getWebhookInfo(bot);
                            String effective = afterInfo.map(TelegramBotApiService.WebhookInfo::describeDelivery)
                                    .orElse("unknown(unavailable)");
                            log.info("✅ Bot '{}' registered successfully (mode=long-polling, type={}, webhookDeleted={}, effective={})",
                                    bot.getUsername(), bot.getType(), deleted, effective);
                            syncBotMenuSafely(bot);
                            successCount++;
                        } else if (telegramProperties.isWebhookMode()) {
                            String baseUrl = telegramProperties.getWebhook().normalizedBaseUrl();

                            if (baseUrl.isBlank()) {
                                log.error("❌ Webhook base URL is empty. Bot '{}' cannot be registered in webhook mode.",
                                        bot.getUsername());
                                errorCount++;
                                continue;
                            }

                            String webhookUrl = baseUrl + "/webhook/" + bot.getUsername();
                            boolean ok = telegramBotApiService.setWebhook(bot, webhookUrl,
                                    telegramProperties.getWebhook().getSecretToken());
                            if (ok) {
                                var afterInfo = telegramBotApiService.getWebhookInfo(bot);
                                String effective = afterInfo.map(TelegramBotApiService.WebhookInfo::describeDelivery)
                                        .orElse("unknown(unavailable)");
                                log.info("✅ Bot '{}' registered successfully (mode=webhook, type={}, targetUrl={}, effective={})",
                                        bot.getUsername(), bot.getType(), webhookUrl, effective);
                                syncBotMenuSafely(bot);
                                successCount++;
                            } else {
                                log.error("❌ Bot '{}' webhook registration failed", bot.getUsername());
                                errorCount++;
                            }
                        } else {
                            log.error("❌ Unknown telegram.delivery-mode='{}'. Supported: long-polling, webhook", mode);
                            errorCount++;
                        }
                    } catch (Exception e) {
                        log.error("❌ Error creating bot '{}' (type: {}): {}",
                                bot.getUsername(), bot.getType(), e.getMessage(), e);
                        errorCount++;
                    }
                } else {
                    log.info("⏸️ Bot '{}' skipped (inactive)", bot.getUsername());
                }
            }
            
            log.info("🎉 Bot initialization completed: {} successful, {} errors", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("❌ Error loading bots from database: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void syncBotMenuSafely(BotEntity bot) {
        try {
            botMenuSyncService.syncFor(bot);
        } catch (Exception e) {
            log.warn("⚠️ Failed to sync Telegram menu for bot '{}': {}", bot.getUsername(), e.getMessage());
        }
    }
}
