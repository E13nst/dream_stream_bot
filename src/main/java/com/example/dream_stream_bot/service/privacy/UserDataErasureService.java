package com.example.dream_stream_bot.service.privacy;

import com.example.dream_stream_bot.model.privacy.BotUserErasureEntity;
import com.example.dream_stream_bot.model.privacy.BotUserErasureRepository;
import com.example.dream_stream_bot.model.subscription.ReferralBonusGrantRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionParticipantRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.memory.ChatMemoryService;
import com.example.dream_stream_bot.service.onboarding.OnboardingScopeHolder;
import com.example.dream_stream_bot.service.payment.ReceiptEmailAwaitService;
import com.example.dream_stream_bot.service.subscription.GroupLinkWizardStateHolder;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Полное удаление данных пользователя в рамках одного бота (/forget_me).
 * Строка {@code users} и {@code telegram_id} не удаляются (мульти-бот).
 */
@Service
public class UserDataErasureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataErasureService.class);

    private final BotUserErasureRepository erasureRepository;
    private final TelegramIdHashService telegramIdHashService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionParticipantRepository participantRepository;
    private final ReferralBonusGrantRepository referralBonusGrantRepository;
    private final ConsentService consentService;
    private final ChatMemoryService chatMemoryService;
    private final OnboardingScopeHolder onboardingScopeHolder;
    private final GroupLinkWizardStateHolder groupLinkWizardStateHolder;
    private final ReceiptEmailAwaitService receiptEmailAwaitService;

    public UserDataErasureService(BotUserErasureRepository erasureRepository,
                                  TelegramIdHashService telegramIdHashService,
                                  SubscriptionRepository subscriptionRepository,
                                  SubscriptionService subscriptionService,
                                  SubscriptionParticipantRepository participantRepository,
                                  ReferralBonusGrantRepository referralBonusGrantRepository,
                                  ConsentService consentService,
                                  ChatMemoryService chatMemoryService,
                                  OnboardingScopeHolder onboardingScopeHolder,
                                  GroupLinkWizardStateHolder groupLinkWizardStateHolder,
                                  ReceiptEmailAwaitService receiptEmailAwaitService) {
        this.erasureRepository = erasureRepository;
        this.telegramIdHashService = telegramIdHashService;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.participantRepository = participantRepository;
        this.referralBonusGrantRepository = referralBonusGrantRepository;
        this.consentService = consentService;
        this.chatMemoryService = chatMemoryService;
        this.onboardingScopeHolder = onboardingScopeHolder;
        this.groupLinkWizardStateHolder = groupLinkWizardStateHolder;
        this.receiptEmailAwaitService = receiptEmailAwaitService;
    }

    @Transactional
    public ErasureResult eraseForBot(long botId, long appUserId, long telegramUserId) {
        String hash = telegramIdHashService.hashForBot(botId, telegramUserId);
        if (erasureRepository.existsByBotIdAndTelegramIdHash(botId, hash)) {
            LOGGER.info("⏭ Erasure already recorded | bot={} | hash={}", botId, hash);
            return new ErasureResult(true, 0, 0, 0, 0, 0);
        }

        clearInMemoryState(botId, appUserId);

        int referralDeleted = referralBonusGrantRepository.deleteByBotIdAndUserId(botId, appUserId);
        int participantsRemoved = participantRepository.deleteByTelegramIdOnBot(telegramUserId, botId);

        List<SubscriptionEntity> owned = new ArrayList<>(
                subscriptionRepository.findByOwnerUserIdAndBotId(appUserId, botId));
        int subscriptionsDeleted = 0;
        for (SubscriptionEntity sub : owned) {
            subscriptionService.deleteFully(sub);
            subscriptionsDeleted++;
        }

        int consentsDeleted = consentService.deleteConsentsForUserOnBot(appUserId, botId);
        int chatMemoryRowsDeleted = chatMemoryService.forgetUser(botId, telegramUserId);

        BotUserErasureEntity record = new BotUserErasureEntity();
        record.setBotId(botId);
        record.setTelegramIdHash(hash);
        erasureRepository.save(record);

        LOGGER.info("🗑 User data erased | bot={} | appUser={} | subs={} | participants={} | consents={} | referral={} | memory={}",
                botId, appUserId, subscriptionsDeleted, participantsRemoved, consentsDeleted,
                referralDeleted, chatMemoryRowsDeleted);

        return new ErasureResult(false, subscriptionsDeleted, participantsRemoved, consentsDeleted,
                referralDeleted, chatMemoryRowsDeleted);
    }

    private void clearInMemoryState(long botId, long appUserId) {
        onboardingScopeHolder.clearAll(botId, appUserId);
        groupLinkWizardStateHolder.clear(botId, appUserId);
        receiptEmailAwaitService.clearAwaiting(botId, appUserId);
    }
}
