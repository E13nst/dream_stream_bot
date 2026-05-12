package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Доступ к package-private {@link SubscriptionTariffService#validate} из того же пакета. */
class SubscriptionTariffCommerceValidationTest {

    private static SubscriptionTariffEntity basePaidTariff() {
        SubscriptionTariffEntity e = new SubscriptionTariffEntity();
        e.setBotId(1L);
        e.setCode("PAID");
        e.setTitle("Paid");
        e.setScope(TariffScope.PERSONAL);
        e.setAccessMode(TariffAccessMode.PAID_TERM);
        e.setSortOrder(0);
        e.setActive(true);
        e.setDefaultPersonal(false);
        e.setDefaultGroup(false);
        e.setReferralEnabled(false);
        return e;
    }

    @Test
    void rejectsPriceWithoutPaidTermDays() {
        SubscriptionTariffEntity e = basePaidTariff();
        e.setPriceAmountMinor(100L);
        e.setPaidTermDays(null);
        assertThatThrownBy(() -> SubscriptionTariffService.validate(e))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("дней");
    }

    @Test
    void rejectsPaidTermDaysWithoutPrice() {
        SubscriptionTariffEntity e = basePaidTariff();
        e.setPaidTermDays(30);
        assertThatThrownBy(() -> SubscriptionTariffService.validate(e))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("без цены");
    }
}
