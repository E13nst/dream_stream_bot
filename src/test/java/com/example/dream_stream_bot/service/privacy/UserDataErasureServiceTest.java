package com.example.dream_stream_bot.service.privacy;

import com.example.dream_stream_bot.model.consent.ConsentChangeType;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.model.consent.UserConsentRepository;
import com.example.dream_stream_bot.model.privacy.BotUserErasureRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotRepository;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.model.user.UserRepository;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.memory.ChatMemoryEntity;
import com.example.dream_stream_bot.service.memory.PostgresChatMemoryRepository;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserDataErasureServiceTest {

    @Autowired
    private UserDataErasureService userDataErasureService;
    @Autowired
    private TelegramIdHashService telegramIdHashService;
    @Autowired
    private BotRepository botRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConsentService consentService;
    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionTariffRepository tariffRepository;
    @Autowired
    private UserConsentRepository userConsentRepository;
    @Autowired
    private PostgresChatMemoryRepository chatMemoryRepository;
    @Autowired
    private BotUserErasureRepository erasureRepository;

    @Test
    void eraseForBot_deletesDataAndRecordsErasure_idempotentOnRepeat() {
        BotEntity bot = botRepository.findByUsername("integration_test_bot").orElseThrow();
        UserEntity user = userRepository.findByTelegramId(141614461L).orElseThrow();
        long botId = bot.getId();
        long tgId = user.getTelegramId();

        ConsentDocumentEntity doc = consentService.createDraft(
                ConsentCode.PRIVACY_POLICY,
                "Erasure test policy",
                "body",
                null,
                ConsentChangeType.MINOR);
        consentService.publish(doc.getId(), false);
        consentService.bindDocumentToBot(botId, ConsentCode.PRIVACY_POLICY, doc.getId());
        consentService.recordAcceptance(user.getId(), doc.getId(), null, null, null, "test");

        Long tariffId = tariffRepository.findByBotIdAndDefaultPersonalTrue(botId).orElseThrow().getId();
        subscriptionService.createOrGet(user.getId(), botId, tariffId, null);

        String conversationId = "bot:" + botId + ":user:" + tgId;
        ChatMemoryEntity memory = new ChatMemoryEntity();
        memory.setConversationId(conversationId);
        memory.setMessageIndex(0);
        memory.setRole("user");
        memory.setContent("hello");
        chatMemoryRepository.save(memory);

        assertFalse(erasureRepository.existsByBotIdAndTelegramIdHash(
                botId, telegramIdHashService.hashForBot(botId, tgId)));

        ErasureResult first = userDataErasureService.eraseForBot(botId, user.getId(), tgId);
        assertFalse(first.alreadyErased());
        assertEquals(1, first.subscriptionsDeleted());
        assertTrue(first.consentsDeleted() >= 1);
        assertEquals(1, first.chatMemoryRowsDeleted());

        assertTrue(subscriptionRepository.findByOwnerUserIdAndBotId(user.getId(), botId).isEmpty());
        assertEquals(0, chatMemoryRepository.countByConversationId(conversationId));
        assertTrue(userConsentRepository.findByUserId(user.getId()).stream()
                .noneMatch(uc -> uc.getDocumentId().equals(doc.getId())));
        assertTrue(erasureRepository.existsByBotIdAndTelegramIdHash(
                botId, telegramIdHashService.hashForBot(botId, tgId)));
        assertTrue(userRepository.findById(user.getId()).isPresent());

        ErasureResult second = userDataErasureService.eraseForBot(botId, user.getId(), tgId);
        assertTrue(second.alreadyErased());
        assertEquals(0, second.subscriptionsDeleted());
    }
}
