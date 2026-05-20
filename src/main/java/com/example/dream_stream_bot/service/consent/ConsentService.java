package com.example.dream_stream_bot.service.consent;

import com.example.dream_stream_bot.model.consent.BotConsentBindingEntity;
import com.example.dream_stream_bot.model.consent.BotConsentBindingRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final BotConsentBindingRepository botConsentBindingRepository;
    private final UserConsentRepository userConsentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TelegraphClient telegraphClient;
    private final BotService botService;
    private final ConsentPublicationNotifier publicationNotifier;

    public ConsentService(ConsentDocumentRepository documentRepository,
                          BotConsentBindingRepository botConsentBindingRepository,
                          UserConsentRepository userConsentRepository,
                          SubscriptionRepository subscriptionRepository,
                          TelegraphClient telegraphClient,
                          BotService botService,
                          ConsentPublicationNotifier publicationNotifier) {
        this.documentRepository = documentRepository;
        this.botConsentBindingRepository = botConsentBindingRepository;
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

    public ConsentDocumentEntity requireDocument(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Consent document id is required");
        }
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consent document not found: " + id));
    }

    public List<ConsentDocumentEntity> listLatestVersions() {
        Map<ConsentCode, ConsentDocumentEntity> latest = new LinkedHashMap<>();
        for (ConsentDocumentEntity document : documentRepository.findAll()) {
            ConsentDocumentEntity current = latest.get(document.getCode());
            if (current == null || current.getVersion() < document.getVersion()) {
                latest.put(document.getCode(), document);
            }
        }
        return latest.values().stream()
                .sorted(Comparator.comparing(ConsentDocumentEntity::getCode))
                .toList();
    }

    public Optional<ConsentDocumentEntity> getActiveForBot(Long botId, ConsentCode code) {
        if (botId == null || code == null) {
            return Optional.empty();
        }
        Optional<BotConsentBindingEntity> binding = botConsentBindingRepository
                .findFirstByBotIdAndConsentCodeAndActiveTrue(botId, code);
        if (binding.isEmpty()) {
            return Optional.empty();
        }
        Long documentId = binding.get().getDocumentId();
        if (documentId == null) {
            return Optional.empty();
        }
        return documentRepository.findById(documentId);
    }

    public List<ConsentCode> requiredCodesForBot(Long botId, boolean includeOffer) {
        if (botId == null) {
            return List.of();
        }
        EnumSet<ConsentCode> codes = EnumSet.noneOf(ConsentCode.class);
        for (BotConsentBindingEntity binding : botConsentBindingRepository.findByBotIdAndActiveTrueOrderByConsentCodeAsc(botId)) {
            if (!includeOffer && binding.getConsentCode() == ConsentCode.OFFER) {
                continue;
            }
            codes.add(binding.getConsentCode());
        }
        return new ArrayList<>(codes);
    }

    public Map<ConsentCode, ConsentDocumentEntity> activeDocumentsByBot(Long botId) {
        Map<ConsentCode, ConsentDocumentEntity> result = new LinkedHashMap<>();
        if (botId == null) {
            return result;
        }
        for (BotConsentBindingEntity binding : botConsentBindingRepository.findByBotIdAndActiveTrueOrderByConsentCodeAsc(botId)) {
            Long documentId = binding.getDocumentId();
            if (documentId == null) {
                continue;
            }
            documentRepository.findById(documentId)
                    .ifPresent(document -> result.put(binding.getConsentCode(), document));
        }
        return result;
    }

    @Transactional
    public BotConsentBindingEntity bindDocumentToBot(Long botId, ConsentCode code, Long documentId) {
        if (botId == null || code == null || documentId == null) {
            throw new IllegalArgumentException("botId, code and documentId are required");
        }
        ConsentDocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Consent document not found: " + documentId));
        if (doc.getCode() != code) {
            throw new IllegalArgumentException("Document code mismatch: expected " + code + ", got " + doc.getCode());
        }

        List<BotConsentBindingEntity> activeRows = botConsentBindingRepository
                .findAllByBotIdAndConsentCodeAndActiveTrue(botId, code);
        if (activeRows.size() == 1 && documentId.equals(activeRows.get(0).getDocumentId())) {
            return activeRows.get(0);
        }
        for (BotConsentBindingEntity binding : activeRows) {
            binding.setActive(false);
            botConsentBindingRepository.save(binding);
        }

        BotConsentBindingEntity newBinding = new BotConsentBindingEntity();
        newBinding.setBotId(botId);
        newBinding.setConsentCode(code);
        newBinding.setDocumentId(documentId);
        newBinding.setActive(true);
        BotConsentBindingEntity saved = botConsentBindingRepository.save(newBinding);

        if (doc.getChangeType() == ConsentChangeType.MATERIAL) {
            triggerMaterialGraceForBot(botId);
        }
        return saved;
    }

    @Transactional
    public void clearBindingForBot(Long botId, ConsentCode code) {
        if (botId == null || code == null) {
            return;
        }
        List<BotConsentBindingEntity> activeRows = botConsentBindingRepository
                .findAllByBotIdAndConsentCodeAndActiveTrue(botId, code);
        for (BotConsentBindingEntity binding : activeRows) {
            binding.setActive(false);
            botConsentBindingRepository.save(binding);
        }
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

        String resolvedTitle = title != null && !title.isBlank() ? title.trim() : code.getDefaultTitle();
        assertTitleNotSharedWithOtherConsentCode(code, resolvedTitle);

        ConsentDocumentEntity entity = new ConsentDocumentEntity();
        entity.setCode(code);
        entity.setVersion(nextVersion);
        entity.setTitle(resolvedTitle);
        entity.setBodyMarkdown(bodyMarkdown);
        entity.setExternalUrl(externalUrl);
        entity.setChangeType(changeType != null ? changeType : ConsentChangeType.MINOR);
        entity.setCurrent(false);
        return documentRepository.save(entity);
    }

    /**
     * Одинаковые заголовки у документов с разными {@link ConsentCode}
     * делают строки в привязках к боту неотличимыми (один текст — разный юридический смысл).
     * Версии одного кода с одинаковым названием по-прежнему разрешены.
     */
    private static String normalizeConsentTitleKey(String t) {
        if (t == null) {
            return "";
        }
        return t.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private void assertTitleNotSharedWithOtherConsentCode(ConsentCode code, String resolvedTitle) {
        if (resolvedTitle == null || resolvedTitle.isBlank()) {
            return;
        }
        String key = normalizeConsentTitleKey(resolvedTitle);
        if (key.isEmpty()) {
            return;
        }
        for (ConsentDocumentEntity row : documentRepository.findAll()) {
            if (row.getCode() == code) {
                continue;
            }
            if (normalizeConsentTitleKey(row.getTitle()).equals(key)) {
                throw new IllegalArgumentException(
                        "Заголовок «" + resolvedTitle.trim() + "» уже занят документом с кодом "
                                + row.getCode().name() + " (id=" + row.getId() + "). "
                                + "Задайте разные названия для разных кодов согласия (политика, оферта, …), "
                                + "чтобы в привязках к боту не перепутать документы.");
            }
        }
    }

    @Transactional
    public ConsentDocumentEntity createDraftFrom(Long sourceDocumentId,
                                                 String title,
                                                 String bodyMarkdown,
                                                 String externalUrl,
                                                 ConsentChangeType changeType) {
        ConsentDocumentEntity source = requireDocument(sourceDocumentId);
        return createDraft(
                source.getCode(),
                title != null ? title : source.getTitle(),
                bodyMarkdown != null ? bodyMarkdown : source.getBodyMarkdown(),
                externalUrl != null ? externalUrl : source.getExternalUrl(),
                changeType != null ? changeType : source.getChangeType()
        );
    }

    /**
     * Публикует draft как текущую версию, при необходимости — в Telegra.ph.
     * Помечает прежнюю текущую версию как is_current=false.
     */
    @Transactional
    public ConsentDocumentEntity publish(Long documentId, boolean publishToTelegraph) {
        if (documentId == null) {
            throw new IllegalArgumentException("Consent document id is required");
        }
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
     * Согласия для доступа к боту. Оферта принимается отдельно в покупательском сценарии.
     */
    public boolean hasRequiredConsents(BotEntity bot, Long appUserId) {
        if (bot == null || appUserId == null) {
            return false;
        }
        for (ConsentCode code : requiredCodesForBot(bot.getId(), false)) {
            if (!hasAcceptedActiveDocument(appUserId, bot.getId(), code)) {
                return false;
            }
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
        for (ConsentCode code : requiredCodesForBot(bot.getId(), false)) {
            if (!hasAcceptedActiveDocument(appUserId, bot.getId(), code)) {
                return false;
            }
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

    /**
     * Полное удаление записей принятия согласий по документам, привязанным к боту.
     */
    @Transactional
    public int deleteConsentsForUserOnBot(Long userId, Long botId) {
        if (userId == null || botId == null) {
            return 0;
        }
        int n = userConsentRepository.deleteForUserOnBot(userId, botId);
        LOGGER.info("🗑 Deleted {} consent rows for user {} on bot {}", n, userId, botId);
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

    private void triggerMaterialGraceForBot(Long botId) {
        OffsetDateTime until = OffsetDateTime.now().plusDays(MATERIAL_GRACE_DAYS);
        List<SubscriptionEntity> subscriptions = subscriptionRepository.findByBotId(botId);
        for (SubscriptionEntity sub : subscriptions) {
            sub.setRequiresConsentReacceptanceUntil(until);
            subscriptionRepository.save(sub);
        }
        LOGGER.info("🚨 Material change binding on bot={} — set grace until {} for {} subscriptions",
                botId, until, subscriptions.size());
    }

    private boolean hasAcceptedActiveDocument(Long userId, Long botId, ConsentCode code) {
        Optional<ConsentDocumentEntity> active = getActiveForBot(botId, code);
        if (active.isEmpty()) {
            return false;
        }
        return userConsentRepository.findFirstByUserIdAndDocumentIdAndRevokedAtIsNull(userId, active.get().getId())
                .isPresent();
    }

    /**
     * Принята ли пользователем именно та версия документа, которая привязана к боту
     * ({@link #getActiveForBot(Long, ConsentCode)}), а не глобально текущая ({@link #getCurrent(ConsentCode)}).
     */
    public boolean hasAcceptedBotBoundDocument(Long userId, Long botId, ConsentCode code) {
        if (userId == null || botId == null || code == null) {
            return false;
        }
        return hasAcceptedActiveDocument(userId, botId, code);
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
