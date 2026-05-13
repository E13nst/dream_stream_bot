package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.subscription.TrialUsageRepository;
import com.example.dream_stream_bot.model.subscription.ReferralBonusGrantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SubscriptionTariffService {

    static final String CODE_PERSONAL_TRIAL = "PERSONAL_TRIAL";
    static final String CODE_PERSONAL_FREE = "PERSONAL_FREE";

    private final SubscriptionTariffRepository tariffRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TrialUsageRepository trialUsageRepository;
    private final ReferralBonusGrantRepository referralBonusGrantRepository;

    public SubscriptionTariffService(SubscriptionTariffRepository tariffRepository,
                                     SubscriptionRepository subscriptionRepository,
                                     TrialUsageRepository trialUsageRepository,
                                     ReferralBonusGrantRepository referralBonusGrantRepository) {
        this.tariffRepository = tariffRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.trialUsageRepository = trialUsageRepository;
        this.referralBonusGrantRepository = referralBonusGrantRepository;
    }

    public SubscriptionTariffEntity require(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tariff id=" + id));
    }

    public List<SubscriptionTariffEntity> listByBot(Long botId) {
        return tariffRepository.findByBotIdOrderBySortOrderAscIdAsc(botId);
    }

    public List<SubscriptionTariffEntity> listPersonalTrialAndFreeEligible(long botId, long ownerUserId) {
        List<SubscriptionTariffEntity> tariffs = tariffRepository.findByBotIdOrderBySortOrderAscIdAsc(botId);
        List<SubscriptionTariffEntity> out = new ArrayList<>();
        for (SubscriptionTariffEntity t : tariffs) {
            if (!Boolean.TRUE.equals(t.isActive())) {
                continue;
            }
            if (t.getScope() != TariffScope.PERSONAL) {
                continue;
            }
            if (t.getAccessMode() == TariffAccessMode.FREE_UNLIMITED) {
                out.add(t);
            } else if (t.getAccessMode() == TariffAccessMode.TRIAL_ONBOARDING) {
                if (trialUsageRepository.findByTariffIdAndOwnerUserIdAndScopeChatId(t.getId(), ownerUserId, 0L).isEmpty()) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    /** Активные групповые тарифы бота (витрина мастера привязки). */
    public List<SubscriptionTariffEntity> listActiveGroupTariffs(long botId) {
        return tariffRepository.findByBotIdAndActiveTrueAndScopeOrderBySortOrderAscIdAsc(botId, TariffScope.GROUP);
    }

    public SubscriptionTariffEntity requireForBot(Long botId, Long tariffId) {
        SubscriptionTariffEntity t = require(tariffId);
        if (!t.getBotId().equals(botId)) {
            throw new IllegalArgumentException("Tariff " + tariffId + " belongs to bot " + t.getBotId() + ", expected " + botId);
        }
        return t;
    }

    public SubscriptionTariffEntity resolveDefaultPersonal(Long botId) {
        return tariffRepository.findByBotIdAndDefaultPersonalTrue(botId)
                .orElseGet(() -> tariffRepository.findByBotIdAndCode(botId, CODE_PERSONAL_TRIAL)
                        .orElseThrow(() -> new IllegalStateException("No personal default tariff for bot " + botId)));
    }

    public SubscriptionTariffEntity resolveDefaultGroup(Long botId) {
        return tariffRepository.findByBotIdAndDefaultGroupTrue(botId)
                .orElseGet(() -> tariffRepository.findByBotIdAndCode(botId, "GROUP_S")
                        .orElseThrow(() -> new IllegalStateException("No group default tariff for bot " + botId)));
    }

    /** Создаёт стандартный набор тарифов для нового бота (если ещё пусто). */
    @Transactional
    public void ensureDefaultTariffsForBot(Long botId) {
        if (tariffRepository.countByBotId(botId) > 0) {
            return;
        }
        int order = 0;
        saveNew(botId, CODE_PERSONAL_TRIAL, "Персональный (пробный период)", TariffScope.PERSONAL,
                TariffAccessMode.TRIAL_ONBOARDING, 3, null, order++, true, false,
                false, null, null, true, null);
        saveNew(botId, CODE_PERSONAL_FREE, "Персональный (бесплатно)", TariffScope.PERSONAL,
                TariffAccessMode.FREE_UNLIMITED, null, null, order++, false, false,
                false, null, null, true, null);
        saveNew(botId, "GROUP_TRIAL", "Группа (пробный период 3 дня)", TariffScope.GROUP,
                TariffAccessMode.TRIAL_ONBOARDING, 3, 50, order++, false, false,
                false, null, null, true, null);
        saveNew(botId, "GROUP_S", "Группа (до 10)", TariffScope.GROUP, TariffAccessMode.PAID_TERM,
                null, 10, order++, false, true,
                false, null, null, true, null);
        saveNew(botId, "GROUP_M", "Группа (до 25)", TariffScope.GROUP, TariffAccessMode.PAID_TERM,
                null, 25, order++, false, false,
                false, null, null, true, null);
        saveNew(botId, "GROUP_L", "Группа (до 50)", TariffScope.GROUP, TariffAccessMode.PAID_TERM,
                null, 50, order++, false, false,
                false, null, null, true, null);
    }

    private void saveNew(Long botId, String code, String title, TariffScope scope,
                         TariffAccessMode mode, Integer trialDays, Integer maxParticipants, int sortOrder,
                         boolean defaultPersonal, boolean defaultGroup,
                         boolean referralEnabled, Integer referralReferrerDays,
                         Integer referralReferredDays, boolean referralFirstPaymentOnly,
                         String activationInstruction) {
        SubscriptionTariffEntity e = new SubscriptionTariffEntity();
        e.setBotId(botId);
        e.setCode(code);
        e.setTitle(title);
        e.setScope(scope);
        e.setAccessMode(mode);
        e.setTrialDays(trialDays);
        e.setMaxParticipants(maxParticipants);
        e.setSortOrder(sortOrder);
        e.setDefaultPersonal(defaultPersonal);
        e.setDefaultGroup(defaultGroup);
        e.setReferralEnabled(referralEnabled);
        e.setReferralReferrerDays(referralReferrerDays);
        e.setReferralReferredDays(referralReferredDays);
        e.setReferralFirstPaymentOnly(referralFirstPaymentOnly);
        applyActivationInstruction(e, activationInstruction);
        validate(e);
        tariffRepository.save(e);
    }

    @Transactional
    public SubscriptionTariffEntity create(Long botId, String code, String title, TariffScope scope,
                                             TariffAccessMode accessMode, Integer trialDays,
                                             Integer maxParticipants, int sortOrder, boolean active,
                                             boolean defaultPersonal, boolean defaultGroup,
                                             boolean referralEnabled, Integer referralReferrerDays,
                                             Integer referralReferredDays, boolean referralFirstPaymentOnly,
                                             Long priceAmountMinor, String currency, Integer paidTermDays,
                                             String checkoutDescription, String detailDescription,
                                             String activationInstruction) {
        SubscriptionTariffEntity e = new SubscriptionTariffEntity();
        e.setBotId(botId);
        e.setCode(normalizeCode(code));
        e.setTitle(title.trim());
        e.setScope(scope);
        e.setAccessMode(accessMode);
        e.setTrialDays(trialDays);
        e.setMaxParticipants(maxParticipants);
        e.setSortOrder(sortOrder);
        e.setActive(active);
        e.setReferralEnabled(referralEnabled);
        e.setReferralReferrerDays(referralReferrerDays);
        e.setReferralReferredDays(referralReferredDays);
        e.setReferralFirstPaymentOnly(referralFirstPaymentOnly);
        applyCommerceFields(e, priceAmountMinor, currency, paidTermDays, checkoutDescription, detailDescription);
        applyActivationInstruction(e, activationInstruction);
        applyDefaultFlags(e, defaultPersonal, defaultGroup);
        validate(e);
        return tariffRepository.save(e);
    }

    @Transactional
    public SubscriptionTariffEntity update(Long id, Long botId, String code, String title, TariffScope scope,
                                             TariffAccessMode accessMode, Integer trialDays,
                                             Integer maxParticipants, int sortOrder, boolean active,
                                             boolean defaultPersonal, boolean defaultGroup,
                                             boolean referralEnabled, Integer referralReferrerDays,
                                             Integer referralReferredDays, boolean referralFirstPaymentOnly,
                                             Long priceAmountMinor, String currency, Integer paidTermDays,
                                             String checkoutDescription, String detailDescription,
                                             String activationInstruction) {
        SubscriptionTariffEntity e = requireForBot(botId, id);
        e.setCode(normalizeCode(code));
        e.setTitle(title.trim());
        e.setScope(scope);
        e.setAccessMode(accessMode);
        e.setTrialDays(trialDays);
        e.setMaxParticipants(maxParticipants);
        e.setSortOrder(sortOrder);
        e.setActive(active);
        e.setReferralEnabled(referralEnabled);
        e.setReferralReferrerDays(referralReferrerDays);
        e.setReferralReferredDays(referralReferredDays);
        e.setReferralFirstPaymentOnly(referralFirstPaymentOnly);
        applyCommerceFields(e, priceAmountMinor, currency, paidTermDays, checkoutDescription, detailDescription);
        applyActivationInstruction(e, activationInstruction);
        applyDefaultFlags(e, defaultPersonal, defaultGroup);
        validate(e);
        return tariffRepository.save(e);
    }

    @Transactional
    public void delete(Long id, Long botId) {
        SubscriptionTariffEntity e = requireForBot(botId, id);

        // Deletion must not fail if only historical/cancelled data remains.
        subscriptionRepository.deleteByTariffIdAndStatusIn(id, List.of(
                SubscriptionStatus.CANCELLED,
                SubscriptionStatus.EXPIRED,
                SubscriptionStatus.PENDING_CONSENT,
                SubscriptionStatus.AWAITING_ACTIVATION
        ));
        referralBonusGrantRepository.deleteByTariffId(id);
        trialUsageRepository.deleteByTariffId(id);

        long linkedSubscriptions = subscriptionRepository.countByTariffId(id);
        if (linkedSubscriptions > 0) {
            throw new IllegalStateException(
                    "Тариф используется активными/триальными подписками. Сначала переведите их на другой тариф или отмените.");
        }
        tariffRepository.delete(e);
    }

    private void applyDefaultFlags(SubscriptionTariffEntity e, boolean defaultPersonal, boolean defaultGroup) {
        if (defaultPersonal && e.getScope() != TariffScope.PERSONAL) {
            throw new IllegalArgumentException("Только персональный тариф может быть default для лички");
        }
        if (defaultGroup && e.getScope() != TariffScope.GROUP) {
            throw new IllegalArgumentException("Только групповой тариф может быть default для группы");
        }
        if (defaultPersonal) {
            tariffRepository.findByBotIdAndDefaultPersonalTrue(e.getBotId()).ifPresent(other -> {
                if (!Objects.equals(other.getId(), e.getId())) {
                    other.setDefaultPersonal(false);
                    tariffRepository.save(other);
                }
            });
        }
        if (defaultGroup) {
            tariffRepository.findByBotIdAndDefaultGroupTrue(e.getBotId()).ifPresent(other -> {
                if (!Objects.equals(other.getId(), e.getId())) {
                    other.setDefaultGroup(false);
                    tariffRepository.save(other);
                }
            });
        }
        e.setDefaultPersonal(defaultPersonal);
        e.setDefaultGroup(defaultGroup);
    }

    static void validate(SubscriptionTariffEntity e) {
        if (e.getScope() == TariffScope.PERSONAL) {
            if (e.getMaxParticipants() != null) {
                throw new IllegalArgumentException("У персонального тарифа max_participants должен быть пустым");
            }
        }
        if (e.getScope() == TariffScope.GROUP) {
            if (e.getMaxParticipants() == null || e.getMaxParticipants() <= 0) {
                throw new IllegalArgumentException("У группового тарифа нужен положительный max_participants");
            }
        }
        if (e.getAccessMode() == TariffAccessMode.FREE_UNLIMITED) {
            if (e.getScope() != TariffScope.PERSONAL) {
                throw new IllegalArgumentException("FREE доступен только для персональных тарифов");
            }
            if (e.getTrialDays() != null) {
                throw new IllegalArgumentException("FREE не задаёт trial_days");
            }
        }
        if (e.getAccessMode() == TariffAccessMode.TRIAL_ONBOARDING) {
            if (e.getTrialDays() == null || e.getTrialDays() <= 0) {
                throw new IllegalArgumentException("Для режима триала укажите trial_days ≥ 1");
            }
        }
        if (e.getAccessMode() == TariffAccessMode.PAID_TERM) {
            if (e.getTrialDays() != null) {
                throw new IllegalArgumentException("PAID_TERM не использует trial_days (оставьте пустым)");
            }
        }
        if (e.isReferralEnabled()) {
            int referrerDays = e.getReferralReferrerDays() == null ? 0 : e.getReferralReferrerDays();
            int referredDays = e.getReferralReferredDays() == null ? 0 : e.getReferralReferredDays();
            if (referrerDays < 0 || referredDays < 0) {
                throw new IllegalArgumentException("Реферальные дни не могут быть отрицательными");
            }
            if (referrerDays + referredDays <= 0) {
                throw new IllegalArgumentException("При включенной рефералке укажите дни хотя бы для одной стороны");
            }
        } else {
            e.setReferralReferrerDays(null);
            e.setReferralReferredDays(null);
        }

        if (e.getPriceAmountMinor() != null) {
            if (e.getPriceAmountMinor() <= 0) {
                throw new IllegalArgumentException("Цена должна быть положительной (в копейках)");
            }
            if (e.getAccessMode() != TariffAccessMode.PAID_TERM) {
                throw new IllegalArgumentException("Цена задаётся только для тарифов PAID_TERM");
            }
            if (e.getPaidTermDays() == null || e.getPaidTermDays() < 1) {
                throw new IllegalArgumentException("Укажите «дней за оплату» ≥ 1 для платного тарифа");
            }
        } else if (e.getPaidTermDays() != null) {
            throw new IllegalArgumentException("Нельзя задать «дней за оплату» без цены");
        }
        if (e.getCurrency() == null || e.getCurrency().isBlank()) {
            e.setCurrency("RUB");
        }
        if (e.getCurrency().length() > 8) {
            throw new IllegalArgumentException("Код валюты не длиннее 8 символов");
        }
    }

    private static void applyActivationInstruction(SubscriptionTariffEntity e, String raw) {
        if (raw != null && !raw.isBlank()) {
            e.setActivationInstruction(raw.trim());
        } else {
            e.setActivationInstruction(null);
        }
    }

    private static void applyCommerceFields(SubscriptionTariffEntity e, Long priceAmountMinor, String currency,
                                           Integer paidTermDays, String checkoutDescription,
                                           String detailDescription) {
        e.setPriceAmountMinor(priceAmountMinor);
        if (currency != null && !currency.isBlank()) {
            e.setCurrency(currency.trim().toUpperCase());
        }
        e.setPaidTermDays(paidTermDays);
        if (checkoutDescription != null && !checkoutDescription.isBlank()) {
            e.setCheckoutDescription(checkoutDescription.trim());
        } else {
            e.setCheckoutDescription(null);
        }
        if (detailDescription != null && !detailDescription.isBlank()) {
            e.setDetailDescription(detailDescription.trim());
        } else {
            e.setDetailDescription(null);
        }
    }

    private static String normalizeCode(String code) {
        String trimmed = code == null ? "" : code.trim().toUpperCase().replace('-', '_');
        if (!trimmed.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("Код тарифа: только латиница, цифры, подчёркивание");
        }
        return trimmed;
    }

    /** Для сообщений пользователю: «Персональный / Группа (до N)». */
    public String formatShortLabel(Long tariffId) {
        SubscriptionTariffEntity t = require(tariffId);
        if (t.getScope() == TariffScope.PERSONAL) {
            return t.getTitle();
        }
        return t.getTitle();
    }

    public boolean isGroupTariff(Long tariffId) {
        return require(tariffId).getScope() == TariffScope.GROUP;
    }
}
