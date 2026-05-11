package com.example.dream_stream_bot.service.memory;

import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.settings.SystemSettingsService;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.subscription.SubscriptionTariffService;
import com.example.dream_stream_bot.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Удаление записей {@code chat_memory} после окончания подписки согласно {@link SystemSettingsService}.
 */
@Service
public class ChatMemoryRetentionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMemoryRetentionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionTariffService subscriptionTariffService;
    private final PostgresChatMemoryRepository chatMemoryRepository;
    private final SystemSettingsService systemSettingsService;
    private final UserService userService;

    public ChatMemoryRetentionService(SubscriptionRepository subscriptionRepository,
                                      SubscriptionService subscriptionService,
                                      SubscriptionTariffService subscriptionTariffService,
                                      PostgresChatMemoryRepository chatMemoryRepository,
                                      SystemSettingsService systemSettingsService,
                                      UserService userService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionTariffService = subscriptionTariffService;
        this.chatMemoryRepository = chatMemoryRepository;
        this.systemSettingsService = systemSettingsService;
        this.userService = userService;
    }

    @Transactional
    public int purgeExpiredMemories() {
        if (systemSettingsService.isRetentionUnlimited()) {
            return 0;
        }
        int days = systemSettingsService.getRetentionDaysAfterExpiry();
        OffsetDateTime now = OffsetDateTime.now();
        int totalDeleted = 0;

        for (SubscriptionEntity sub : subscriptionRepository.findAll()) {
            if (sub.getExpiresAt() == null) {
                continue;
            }
            OffsetDateTime purgeOkAfter = sub.getExpiresAt().plusDays(days);
            if (now.isBefore(purgeOkAfter)) {
                continue;
            }
            if (subscriptionService.isActive(sub)) {
                continue;
            }
            if (subscriptionTariffService.isGroupTariff(sub.getTariffId()) && sub.getScopeChatId() != null) {
                String prefix = "bot:" + sub.getBotId() + ":chat:" + sub.getScopeChatId();
                int rows = chatMemoryRepository.deleteByConversationIdStartingWith(prefix);
                totalDeleted += rows;
                if (rows > 0) {
                    LOGGER.info("🧹 Retention purge | group prefix={} | deleted={}", prefix, rows);
                }
            } else {
                UserEntity owner = userService.findById(sub.getOwnerUserId()).orElse(null);
                if (owner == null) {
                    continue;
                }
                String personalConv = "bot:" + sub.getBotId() + ":user:" + owner.getTelegramId();
                long rows = chatMemoryRepository.deleteAllByConversationId(personalConv);
                totalDeleted += (int) rows;
                if (rows > 0) {
                    LOGGER.info("🧹 Retention purge | personal conv={} | deleted={}", personalConv, rows);
                }
            }
        }

        return totalDeleted;
    }
}
