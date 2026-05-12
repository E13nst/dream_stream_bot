package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Текст карточки личной подписки пользователя на конкретного бота (без Markdown).
 */
@Service
public class SubscriptionCardTextBuilder {

    private static final DateTimeFormatter DATE_RU = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public String buildPersonalCard(BotEntity bot, SubscriptionEntity sub, SubscriptionTariffEntity tariff) {
        String header = headerLine(sub.getStatus(), tariff);
        String tariffLine = "• Тариф: " + tariffTitle(tariff, sub);
        String untilLine = "• Окончание: " + expiryLine(sub, tariff);

        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n\n");
        sb.append(tariffLine).append("\n");
        sb.append(untilLine).append("\n\n");
        sb.append("Спасибо, что с нами!");
        appendBillingHint(bot, sb);
        return sb.toString().trim();
    }

    private static void appendBillingHint(BotEntity bot, StringBuilder sb) {
        if (bot != null && bot.isYookassaReceiptEnabled()) {
            sb.append("\n\nДля чека по 54‑ФЗ укажите email: /billing_email ваш@example.ru");
        }
    }

    private static String tariffTitle(SubscriptionTariffEntity tariff, SubscriptionEntity sub) {
        if (tariff != null && tariff.getTitle() != null && !tariff.getTitle().isBlank()) {
            return tariff.getTitle().trim();
        }
        return sub.getTariffId() != null ? "тариф #" + sub.getTariffId() : "—";
    }

    private static String headerLine(SubscriptionStatus status, SubscriptionTariffEntity tariff) {
        boolean trialTariff = tariff != null && tariff.getAccessMode() == TariffAccessMode.TRIAL_ONBOARDING;
        return switch (status) {
            case PENDING_CONSENT -> "Подписка оформляется";
            case AWAITING_ACTIVATION -> "Ожидаем активации или оплаты";
            case TRIAL -> "Активная подписка (пробный период)";
            case ACTIVE -> trialTariff
                    ? "Активная подписка (пробный период)"
                    : "Активная подписка";
            case EXPIRED -> "Подписка истекла";
            case CANCELLED -> "Подписка отменена";
            case BLOCKED_CONSENT -> "Нужно принять обновлённые условия";
        };
    }

    private static String expiryLine(SubscriptionEntity sub, SubscriptionTariffEntity tariff) {
        TariffAccessMode mode = tariff != null ? tariff.getAccessMode() : null;
        if (mode == TariffAccessMode.FREE_UNLIMITED && sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return "без ограничения срока";
        }
        OffsetDateTime exp = sub.getExpiresAt();
        if (exp != null) {
            return DATE_RU.format(exp.toLocalDate());
        }
        return switch (sub.getStatus()) {
            case PENDING_CONSENT ->
                    "появится после принятия условий и активации доступа";
            case AWAITING_ACTIVATION ->
                    "появится после оплаты или активации администратором";
            case BLOCKED_CONSENT ->
                    "станет доступно после принятия документов через /start";
            case EXPIRED -> "—";
            case CANCELLED -> "—";
            case TRIAL, ACTIVE ->
                    mode == TariffAccessMode.TRIAL_ONBOARDING
                            ? "уточняется по тарифу (если что-то не так — /start)"
                            : "уточняется (если нужно — напишите в поддержку)";
        };
    }
}
