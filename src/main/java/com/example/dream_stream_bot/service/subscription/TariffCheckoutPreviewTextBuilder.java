package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Текст экрана «условия тарифа перед оплатой» (личка, без Markdown).
 */
@Service
public class TariffCheckoutPreviewTextBuilder {

    public enum PayFooter {
        /** Экран перед созданием платежа ЮKassa. */
        YOOKASSA_HINT,
        /** Мастер группы: после выбора тарифа, до привязки чата. */
        GROUP_CONNECT_NEXT
    }

    /**
     * @param includeParticipantsCapLine для групповых тарифов — строка «участников до N», если {@code maxParticipants != null}.
     */
    public String buildCheckoutPreviewText(SubscriptionTariffEntity tariff, boolean includeParticipantsCapLine,
                                           PayFooter payFooter) {
        Integer days = tariff.getPaidTermDays();
        String termLine = days == null ? "⌛ Срок: —" : ("⌛ Срок: " + daysPluralRu(days));

        StringBuilder mid = new StringBuilder();
        mid.append("💰 Цена: ").append(formatRubMinor(tariff.getPriceAmountMinor())).append(" ₽\n");
        mid.append(termLine);
        if (includeParticipantsCapLine && tariff.getMaxParticipants() != null) {
            mid.append("\n👥 Участников до: ").append(tariff.getMaxParticipants());
        }

        String detail = tariff.getDetailDescription();
        if (detail == null || detail.isBlank()) {
            detail = payFooter == PayFooter.GROUP_CONNECT_NEXT
                    ? "• После подключения группы и согласий вы сможете перейти к оплате."
                    : "• Доступ активируется после успешной оплаты.";
        }

        String footer = payFooter == PayFooter.GROUP_CONNECT_NEXT
                ? "Нажмите «🔗 Выбрать группу», укажите чат в Telegram, затем подтвердите выбор в этом чате."
                : "Нажмите «Оплатить через ЮKassa», когда будете готовы перейти к оплате.";

        return """
                ✅ Вы выбрали: %s

                %s

                %s

                %s"""
                .formatted(trimTitle(tariff.getTitle()), mid.toString().strip(), detail.trim(), footer)
                .strip();
    }

    /** Превью для персональной оплаты (футер про ЮKassa). */
    public String buildPersonalCheckoutPreviewText(SubscriptionTariffEntity tariff) {
        return buildCheckoutPreviewText(tariff, false, PayFooter.YOOKASSA_HINT);
    }

    private static String trimTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private static String formatRubMinor(Long minor) {
        if (minor == null) {
            return "?";
        }
        return BigDecimal.valueOf(minor).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY).toPlainString();
    }

    static String daysPluralRu(int n) {
        int abs = Math.abs(n);
        int mod100 = abs % 100;
        int mod10 = abs % 10;
        if (mod100 >= 11 && mod100 <= 19) {
            return abs + " дней";
        }
        if (mod10 == 1) {
            return abs + " день";
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return abs + " дня";
        }
        return abs + " дней";
    }
}
