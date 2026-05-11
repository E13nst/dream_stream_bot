package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class SubscriptionTariffService {

    static final String CODE_PERSONAL_TRIAL = "PERSONAL_TRIAL";
    static final String CODE_PERSONAL_FREE = "PERSONAL_FREE";

    private final SubscriptionTariffRepository tariffRepository;

    public SubscriptionTariffService(SubscriptionTariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    public SubscriptionTariffEntity require(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tariff id=" + id));
    }

    public List<SubscriptionTariffEntity> listByBot(Long botId) {
        return tariffRepository.findByBotIdOrderBySortOrderAscIdAsc(botId);
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
                TariffAccessMode.TRIAL_ONBOARDING, 3, null, order++, true, false);
        saveNew(botId, CODE_PERSONAL_FREE, "Персональный (бесплатно)", TariffScope.PERSONAL,
                TariffAccessMode.FREE_UNLIMITED, null, null, order++, false, false);
        saveNew(botId, "GROUP_S", "Группа (до 10)", TariffScope.GROUP, TariffAccessMode.PAID_TERM,
                null, 10, order++, false, true);
        saveNew(botId, "GROUP_M", "Группа (до 25)", TariffScope.GROUP, TariffAccessMode.PAID_TERM,
                null, 25, order++, false, false);
        saveNew(botId, "GROUP_L", "Группа (до 50)", TariffScope.GROUP, TariffAccessMode.PAID_TERM,
                null, 50, order++, false, false);
    }

    private void saveNew(Long botId, String code, String title, TariffScope scope,
                         TariffAccessMode mode, Integer trialDays, Integer maxParticipants, int sortOrder,
                         boolean defaultPersonal, boolean defaultGroup) {
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
        validate(e);
        tariffRepository.save(e);
    }

    @Transactional
    public SubscriptionTariffEntity create(Long botId, String code, String title, TariffScope scope,
                                             TariffAccessMode accessMode, Integer trialDays,
                                             Integer maxParticipants, int sortOrder, boolean active,
                                             boolean defaultPersonal, boolean defaultGroup) {
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
        applyDefaultFlags(e, defaultPersonal, defaultGroup);
        validate(e);
        return tariffRepository.save(e);
    }

    @Transactional
    public SubscriptionTariffEntity update(Long id, Long botId, String code, String title, TariffScope scope,
                                             TariffAccessMode accessMode, Integer trialDays,
                                             Integer maxParticipants, int sortOrder, boolean active,
                                             boolean defaultPersonal, boolean defaultGroup) {
        SubscriptionTariffEntity e = requireForBot(botId, id);
        e.setCode(normalizeCode(code));
        e.setTitle(title.trim());
        e.setScope(scope);
        e.setAccessMode(accessMode);
        e.setTrialDays(trialDays);
        e.setMaxParticipants(maxParticipants);
        e.setSortOrder(sortOrder);
        e.setActive(active);
        applyDefaultFlags(e, defaultPersonal, defaultGroup);
        validate(e);
        return tariffRepository.save(e);
    }

    @Transactional
    public void delete(Long id, Long botId) {
        SubscriptionTariffEntity e = requireForBot(botId, id);
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
