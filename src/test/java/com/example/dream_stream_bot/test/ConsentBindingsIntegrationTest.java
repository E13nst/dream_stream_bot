package com.example.dream_stream_bot.test;

import com.example.dream_stream_bot.model.consent.ConsentChangeType;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.model.consent.ConsentDocumentRepository;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotRepository;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.model.user.UserRepository;
import com.example.dream_stream_bot.service.consent.ConsentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsentBindingsIntegrationTest {

    @Autowired
    private ConsentService consentService;
    @Autowired
    private BotRepository botRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConsentDocumentRepository consentDocumentRepository;

    @Test
    void requiresReacceptWhenBindingSwitchedToNewVersion() {
        BotEntity bot = createBot("binding-bot");
        UserEntity user = createUser(910001L);

        ConsentDocumentEntity v1 = consentService.createDraft(
                ConsentCode.PRIVACY_POLICY,
                "Policy v1",
                "text v1",
                null,
                ConsentChangeType.MINOR);
        consentService.publish(v1.getId(), false);
        ConsentDocumentEntity v2 = consentService.createDraftFrom(
                v1.getId(), "Policy v2", "text v2", null, ConsentChangeType.MINOR);
        consentService.publish(v2.getId(), false);

        consentService.bindDocumentToBot(bot.getId(), ConsentCode.PRIVACY_POLICY, v1.getId());
        assertFalse(consentService.hasRequiredConsents(bot, user.getId()));

        consentService.recordAcceptance(user.getId(), v1.getId(), null, null, null, "test");
        assertTrue(consentService.hasRequiredConsents(bot, user.getId()));

        consentService.bindDocumentToBot(bot.getId(), ConsentCode.PRIVACY_POLICY, v2.getId());
        assertFalse(consentService.hasRequiredConsents(bot, user.getId()));

        consentService.recordAcceptance(user.getId(), v2.getId(), null, null, null, "test");
        assertTrue(consentService.hasRequiredConsents(bot, user.getId()));
    }

    @Test
    void publishWithNewVersionKeepsOldVersionImmutableAndLatestListStable() {
        ConsentDocumentEntity v1 = consentService.createDraft(
                ConsentCode.PERSONAL_DATA,
                "Personal data v1",
                "body-1",
                null,
                ConsentChangeType.MINOR);
        ConsentDocumentEntity publishedV1 = consentService.publish(v1.getId(), false);

        ConsentDocumentEntity v2Draft = consentService.createDraftFrom(
                publishedV1.getId(),
                "Personal data v2",
                "body-2",
                null,
                ConsentChangeType.MATERIAL);
        ConsentDocumentEntity publishedV2 = consentService.publish(v2Draft.getId(), false);

        Long v1Id = Objects.requireNonNull(publishedV1.getId());
        Long v2Id = Objects.requireNonNull(publishedV2.getId());
        ConsentDocumentEntity reloadedV1 = consentDocumentRepository.findById(v1Id).orElseThrow();
        ConsentDocumentEntity reloadedV2 = consentDocumentRepository.findById(v2Id).orElseThrow();

        assertEquals("Personal data v1", reloadedV1.getTitle());
        assertEquals("body-1", reloadedV1.getBodyMarkdown());
        assertFalse(reloadedV1.isCurrent());
        assertTrue(reloadedV2.isCurrent());

        List<ConsentDocumentEntity> latest = consentService.listLatestVersions();
        long personalDataCount = latest.stream().filter(d -> d.getCode() == ConsentCode.PERSONAL_DATA).count();
        assertEquals(1L, personalDataCount);
        assertEquals(publishedV2.getId(),
                latest.stream()
                        .filter(d -> d.getCode() == ConsentCode.PERSONAL_DATA)
                        .findFirst()
                        .orElseThrow()
                        .getId());
    }

    private BotEntity createBot(String username) {
        BotEntity bot = new BotEntity();
        bot.setName("Test Bot");
        bot.setUsername(username);
        bot.setToken("token-" + username);
        bot.setType("assistant");
        bot.setIsActive(true);
        return botRepository.save(bot);
    }

    private UserEntity createUser(Long telegramId) {
        UserEntity user = new UserEntity();
        user.setTelegramId(telegramId);
        user.setUsername("user" + telegramId);
        return userRepository.save(user);
    }
}
