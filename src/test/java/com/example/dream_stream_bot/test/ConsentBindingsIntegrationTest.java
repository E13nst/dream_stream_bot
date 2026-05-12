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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsentBindingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConsentService consentService;
    @Autowired
    private BotRepository botRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConsentDocumentRepository consentDocumentRepository;

    @Test
    void createDraftRejectsSameNormalizedTitleForAnotherConsentCode() {
        consentService.createDraft(
                ConsentCode.PRIVACY_POLICY,
                "Политика X",
                "x",
                null,
                ConsentChangeType.MINOR);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                consentService.createDraft(
                        ConsentCode.PERSONAL_DATA,
                        "  политика  x  ",
                        "y",
                        null,
                        ConsentChangeType.MINOR));
        assertTrue(ex.getMessage().contains("занят"));
    }

    @Test
    void createDraftAllowsSameTitleForTwoVersionsOfSameCode() {
        ConsentDocumentEntity v1 = consentService.createDraft(
                ConsentCode.PERSONAL_DATA,
                "Одинаковый заголовок",
                "a",
                null,
                ConsentChangeType.MINOR);
        ConsentDocumentEntity v2 = consentService.createDraft(
                ConsentCode.PERSONAL_DATA,
                "Одинаковый заголовок",
                "b",
                null,
                ConsentChangeType.MINOR);
        assertTrue(v2.getVersion() > v1.getVersion());
    }

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPostPersistsPrivacyBinding() throws Exception {
        BotEntity bot = createBot("privacy-admin-binding-bot");
        ConsentDocumentEntity doc = consentService.createDraft(
                ConsentCode.PRIVACY_POLICY,
                "Privacy admin bind",
                "body",
                null,
                ConsentChangeType.MINOR);
        consentService.publish(doc.getId(), false);

        mockMvc.perform(post("/admin/bots/" + bot.getId() + "/consents")
                        .param("binding_OFFER", "")
                        .param("binding_PRIVACY_POLICY", String.valueOf(doc.getId())))
                .andExpect(status().is3xxRedirection());

        assertTrue(consentService.getActiveForBot(bot.getId(), ConsentCode.PRIVACY_POLICY).isPresent());
        assertEquals(doc.getId(),
                consentService.getActiveForBot(bot.getId(), ConsentCode.PRIVACY_POLICY).get().getId());
    }

    @Test
    void onboardingProgressUsesBotBoundDocumentNotGlobalCurrent() {
        BotEntity bot = createBot("bound-pd-noncurrent-bot");
        UserEntity user = createUser(910099L);

        ConsentDocumentEntity v1 = consentService.createDraft(
                ConsentCode.PERSONAL_DATA,
                "PD v1",
                "b1",
                null,
                ConsentChangeType.MINOR);
        consentService.publish(v1.getId(), false);
        ConsentDocumentEntity v2Draft = consentService.createDraftFrom(
                v1.getId(), "PD v2", "b2", null, ConsentChangeType.MINOR);
        consentService.publish(v2Draft.getId(), false);

        consentService.bindDocumentToBot(bot.getId(), ConsentCode.PERSONAL_DATA, v1.getId());
        consentService.recordAcceptance(user.getId(), v1.getId(), null, null, null, "test");

        assertTrue(consentService.hasAcceptedBotBoundDocument(user.getId(), bot.getId(), ConsentCode.PERSONAL_DATA),
                "user accepted the version bound to the bot");
        assertFalse(consentService.hasAcceptedCurrent(user.getId(), ConsentCode.PERSONAL_DATA),
                "global current is v2; user only accepted v1 bound to bot");
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
