package com.example.dream_stream_bot.scheduling;

import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.memory.ChatMemoryRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ночное обслуживание: эскалация истёкшего grace по согласиям и retention для истории чатов.
 */
@Component
public class ScheduledMaintenance {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledMaintenance.class);

    private final ConsentService consentService;
    private final ChatMemoryRetentionService chatMemoryRetentionService;

    public ScheduledMaintenance(ConsentService consentService,
                               ChatMemoryRetentionService chatMemoryRetentionService) {
        this.consentService = consentService;
        this.chatMemoryRetentionService = chatMemoryRetentionService;
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void nightlyConsentAndRetention() {
        int blocked = consentService.escalateExpiredConsents();
        int purged = chatMemoryRetentionService.purgeExpiredMemories();
        LOGGER.info("🗓 Scheduled maintenance | blocked_consents={} | memory_rows_removed={}",
                blocked, purged);
    }
}
