package com.example.dream_stream_bot.service.consent;

import com.example.dream_stream_bot.model.agent.AgentConfigEntity;
import com.example.dream_stream_bot.model.agent.DataLocality;
import com.example.dream_stream_bot.model.consent.ConsentChangeType;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.model.consent.ConsentDocumentRepository;
import com.example.dream_stream_bot.model.consent.UserConsentEntity;
import com.example.dream_stream_bot.model.consent.UserConsentRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис согласий: версионирование документов, фиксирование принятий, проверка
 * актуальности, отзыв.
 *
 * <p>При публикации новой версии с {@code change_type=MATERIAL} всем активным
 * подпискам выставляется {@code requiresConsentReacceptanceUntil = now + 14 дней}
 * — это включает грейс в {@code AccessGate}; по истечении задаётся
 * {@code BLOCKED_CONSENT} (см. {@link #escalateExpiredConsents()}).</p>
 */
@Service
public class ConsentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentService.class);
    public static final int MATERIAL_GRACE_DAYS = 14;

    private final ConsentDocumentRepository documentRepository;
    private final UserConsentRepository userConsentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TelegraphClient telegraphClient;
    private final BotService botService;
    private final ConsentPublicationNotifier publicationNotifier;

    public ConsentService(ConsentDocumentRepository documentRepository,
                          UserConsentRepository userConsentRepository,
                          SubscriptionRepository subscriptionRepository,
                          TelegraphClient telegraphClient,
                          BotService botService,
                          ConsentPublicationNotifier publicationNotifier) {
        this.documentRepository = documentRepository;
        this.userConsentRepository = userConsentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.telegraphClient = telegraphClient;
        this.botService = botService;
        this.publicationNotifier = publicationNotifier;
    }

    public Optional<ConsentDocumentEntity> getCurrent(ConsentCode code) {
        return documentRepository.findByCodeAndCurrentTrue(code);
    }

    public List<ConsentDocumentEntity> listVersions(ConsentCode code) {
        return documentRepository.findByCodeOrderByVersionDesc(code);
    }

    public List<ConsentDocumentEntity> listAll() {
        return documentRepository.findAll();
    }

    /**
     * Создаёт новую версию документа в статусе draft (is_current=false, published_at=null).
     */
    @Transactional
    public ConsentDocumentEntity createDraft(ConsentCode code, String title, String bodyMarkdown,
                                             String externalUrl, ConsentChangeType changeType) {
        int nextVersion = listVersions(code).stream()
                .map(ConsentDocumentEntity::getVersion)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        ConsentDocumentEntity entity = new ConsentDocumentEntity();
        entity.setCode(code);
        entity.setVersion(nextVersion);
        entity.setTitle(title != null && !title.isBlank() ? title : code.getDefaultTitle());
        entity.setBodyMarkdown(bodyMarkdown);
        entity.setExternalUrl(externalUrl);
        entity.setChangeType(changeType != null ? changeType : ConsentChangeType.MINOR);
        entity.setCurrent(false);
        return documentRepository.save(entity);
    }

    /**
     * Публикует draft как текущую версию, при необходимости — в Telegra.ph.
     * Помечает прежнюю текущую версию как is_current=false.
     */
    @Transactional
    public ConsentDocumentEntity publish(Long documentId, boolean publishToTelegraph) {
        ConsentDocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Consent document not found: " + documentId));

        if (publishToTelegraph && doc.getBodyMarkdown() != null && !doc.getBodyMarkdown().isBlank()) {
            TelegraphClient.PublishedPage page;
            if (doc.getTelegraphPath() != null && !doc.getTelegraphPath().isBlank()) {
                page = telegraphClient.edit(doc.getTelegraphPath(), doc.getTitle(), doc.getBodyMarkdown());
            } else {
                page = telegraphClient.publish(doc.getTitle(), doc.getBodyMarkdown());
            }
            doc.setTelegraphPath(page.path());
            doc.setExternalUrl(page.url());
        }

        documentRepository.findByCodeAndCurrentTrue(doc.getCode()).ifPresent(prev -> {
            if (!prev.getId().equals(doc.getId())) {
                prev.setCurrent(false);
                documentRepository.save(prev);
            }
        });
        doc.setCurrent(true);
        doc.setPublishedAt(OffsetDateTime.now());
        ConsentDocumentEntity saved = documentRepository.save(doc);

        if (saved.getChangeType() == ConsentChangeType.MATERIAL) {
            triggerMaterialGrace();
        }
        LOGGER.info("📰 Published consent document | code={} | version={} | telegraph={}",
                saved.getCode(), saved.getVersion(), saved.getExternalUrl());
        publicationNotifier.notifyPublished(saved);
        return saved;
    }

    /**
     * Регистрирует факт принятия пользователем текущей версии документа.
     * Если уже принимал — повторно не создаём.
     */
    @Transactional
    public UserConsentEntity recordAcceptance(Long userId, Long documentId, Long subscriptionId,
                                              Long chatId, Integer telegramMessageId, String acceptedVia) {
        Optional<UserConsentEntity> existing = userConsentRepository
                .findFirstByUserIdAndDocumentIdAndRevokedAtIsNull(userId, documentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        UserConsentEntity entity = new UserConsentEntity();
        entity.setUserId(userId);
        entity.setDocumentId(documentId);
        entity.setSubscriptionId(subscriptionId);
        entity.setChatId(chatId);
        entity.setTelegramMessageId(telegramMessageId);
        entity.setAcceptedVia(acceptedVia);
        return userConsentRepository.save(entity);
    }

    @Transactional
    public int revokeAll(Long userId) {
        int n = userConsentRepository.revokeAllForUser(userId, OffsetDateTime.now());
        LOGGER.info("🗑 Revoked {} consents for user {}", n, userId);
        return n;
    }

    public boolean hasAcceptedCurrent(Long userId, ConsentCode code) {
        Optional<ConsentDocumentEntity> current = getCurrent(code);
        if (current.isEmpty()) {
            return false;
        }
        return userConsentRepository.findFirstByUserIdAndDocumentIdAndRevokedAtIsNull(userId, current.get().getId())
                .isPresent();
    }

    /**
     * То же множество согласий, что использует {@link com.example.dream_stream_bot.service.access.AccessGate}
     * для допуска владельца/участника.
     */
    public boolean hasRequiredConsents(BotEntity bot, Long appUserId) {
        if (bot == null || appUserId == null) {
            return false;
        }
        if (!hasAcceptedCurrent(appUserId, ConsentCode.OFFER)) {
            return false;
        }
        if (!hasAcceptedCurrent(appUserId, ConsentCode.PRIVACY_POLICY)) {
            return false;
        }
        if (!hasAcceptedCurrent(appUserId, ConsentCode.PERSONAL_DATA)) {
            return false;
        }
        if (bot.requiresAgeConfirmation() && !hasAcceptedCurrent(appUserId, ConsentCode.AGE_18)) {
            return false;
        }
        AgentConfigEntity agent = bot.getAgentConfig();
        if (agent != null && agent.getDataLocality() == DataLocality.CROSS_BORDER) {
            return hasAcceptedCurrent(appUserId, ConsentCode.CROSS_BORDER);
        }
        return true;
    }

    /**
     * Участник группы (не покупатель оферты): без {@link ConsentCode#OFFER}.
     */
    public boolean hasParticipantConsents(BotEntity bot, Long appUserId) {
        if (bot == null || appUserId == null) {
            return false;
        }
        if (!hasAcceptedCurrent(appUserId, ConsentCode.PRIVACY_POLICY)) {
            return false;
        }
        if (!hasAcceptedCurrent(appUserId, ConsentCode.PERSONAL_DATA)) {
            return false;
        }
        if (bot.requiresAgeConfirmation() && !hasAcceptedCurrent(appUserId, ConsentCode.AGE_18)) {
            return false;
        }
        AgentConfigEntity agent = bot.getAgentConfig();
        if (agent != null && agent.getDataLocality() == DataLocality.CROSS_BORDER) {
            return hasAcceptedCurrent(appUserId, ConsentCode.CROSS_BORDER);
        }
        return true;
    }

    @Transactional
    public int revokeConsentsLinkedToBot(Long userId, Long botId) {
        if (userId == null || botId == null) {
            return 0;
        }
        int n = userConsentRepository.revokeLinkedToSubscriptionsOnBot(userId, botId, OffsetDateTime.now());
        LOGGER.info("🗑 Revoked {} consent rows for user {} on bot {}", n, userId, botId);
        return n;
    }

    private void triggerMaterialGrace() {
        OffsetDateTime until = OffsetDateTime.now().plusDays(MATERIAL_GRACE_DAYS);
        List<SubscriptionEntity> all = subscriptionRepository.findAll();
        for (SubscriptionEntity sub : all) {
            sub.setRequiresConsentReacceptanceUntil(until);
            subscriptionRepository.save(sub);
        }
        LOGGER.info("🚨 Material change — set grace until {} for {} subscriptions", until, all.size());
    }

    /**
     * Переводит подписки с истёкшим grace и неподтверждёнными согласиями в BLOCKED_CONSENT.
     * Вызывается по @Scheduled или вручную.
     */
    @Transactional
    public int escalateExpiredConsents() {
        OffsetDateTime now = OffsetDateTime.now();
        int blocked = 0;
        for (SubscriptionEntity sub : subscriptionRepository.findAll()) {
            OffsetDateTime until = sub.getRequiresConsentReacceptanceUntil();
            if (until == null || until.isAfter(now)) {
                continue;
            }
            BotEntity bot = botService.findById(sub.getBotId());
            if (bot == null) {
                continue;
            }
            if (hasRequiredConsents(bot, sub.getOwnerUserId())) {
                sub.setRequiresConsentReacceptanceUntil(null);
                subscriptionRepository.save(sub);
                continue;
            }
            sub.setStatus(SubscriptionStatus.BLOCKED_CONSENT);
            sub.setRequiresConsentReacceptanceUntil(null);
            subscriptionRepository.save(sub);
            blocked++;
        }
        if (blocked > 0) {
            LOGGER.warn("🚨 Escalated {} subscriptions to BLOCKED_CONSENT", blocked);
        }
        return blocked;
    }
}
