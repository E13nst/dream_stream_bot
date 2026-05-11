package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.PeriodSource;
import com.example.dream_stream_bot.model.subscription.ReferralBonusGrantEntity;
import com.example.dream_stream_bot.model.subscription.ReferralBonusGrantRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.user.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReferralBonusService {

    private final SubscriptionTariffRepository tariffRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final ReferralBonusGrantRepository referralBonusGrantRepository;
    private final UserService userService;

    public ReferralBonusService(SubscriptionTariffRepository tariffRepository,
                                SubscriptionRepository subscriptionRepository,
                                SubscriptionPeriodRepository periodRepository,
                                ReferralBonusGrantRepository referralBonusGrantRepository,
                                UserService userService) {
        this.tariffRepository = tariffRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.periodRepository = periodRepository;
        this.referralBonusGrantRepository = referralBonusGrantRepository;
        this.userService = userService;
    }

    public Optional<PreparedBonus> prepareForPayment(SubscriptionEntity referredSubscription,
                                                     SubscriptionPeriodEntity paymentPeriod) {
        if (referredSubscription == null || paymentPeriod == null || paymentPeriod.getId() == null) {
            return Optional.empty();
        }
        if (referralBonusGrantRepository.existsByPaymentPeriodId(paymentPeriod.getId())) {
            return Optional.empty();
        }
        SubscriptionTariffEntity tariff = tariffRepository.findById(referredSubscription.getTariffId()).orElse(null);
        if (tariff == null || !tariff.isReferralEnabled()) {
            return Optional.empty();
        }
        if (tariff.isReferralFirstPaymentOnly()) {
            long paymentPeriods = periodRepository.countBySubscriptionIdAndSource(
                    referredSubscription.getId(), PeriodSource.PAYMENT);
            if (paymentPeriods > 1) {
                return Optional.empty();
            }
        }

        UserEntity referred = userService.findById(referredSubscription.getOwnerUserId()).orElse(null);
        if (referred == null || referred.getReferredByUserId() == null) {
            return Optional.empty();
        }
        Optional<SubscriptionEntity> referrerSub = subscriptionRepository.findPersonal(
                referredSubscription.getBotId(), referred.getReferredByUserId());
        if (referrerSub.isEmpty()) {
            return Optional.empty();
        }

        int referrerDays = tariff.getReferralReferrerDays() == null ? 0 : tariff.getReferralReferrerDays();
        int referredDays = tariff.getReferralReferredDays() == null ? 0 : tariff.getReferralReferredDays();
        if (referrerDays <= 0 && referredDays <= 0) {
            return Optional.empty();
        }

        return Optional.of(new PreparedBonus(
                tariff.getId(),
                paymentPeriod.getId(),
                referredSubscription,
                referrerSub.get(),
                referred.getId(),
                referred.getReferredByUserId(),
                referredDays,
                referrerDays
        ));
    }

    public void markGranted(PreparedBonus prepared) {
        ReferralBonusGrantEntity entity = new ReferralBonusGrantEntity();
        entity.setBotId(prepared.referredSubscription().getBotId());
        entity.setTariffId(prepared.tariffId());
        entity.setPaymentPeriodId(prepared.paymentPeriodId());
        entity.setReferredUserId(prepared.referredUserId());
        entity.setReferrerUserId(prepared.referrerUserId());
        entity.setReferredSubscriptionId(prepared.referredSubscription().getId());
        entity.setReferrerSubscriptionId(prepared.referrerSubscription().getId());
        entity.setReferredBonusDays(prepared.referredBonusDays());
        entity.setReferrerBonusDays(prepared.referrerBonusDays());
        referralBonusGrantRepository.save(entity);
    }

    public record PreparedBonus(
            Long tariffId,
            Long paymentPeriodId,
            SubscriptionEntity referredSubscription,
            SubscriptionEntity referrerSubscription,
            Long referredUserId,
            Long referrerUserId,
            int referredBonusDays,
            int referrerBonusDays
    ) {}
}
