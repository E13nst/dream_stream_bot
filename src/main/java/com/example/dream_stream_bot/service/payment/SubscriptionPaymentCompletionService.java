package com.example.dream_stream_bot.service.payment;

import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class SubscriptionPaymentCompletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionPaymentCompletionService.class);

    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTariffRepository tariffRepository;
    private final SubscriptionService subscriptionService;
    private final BotService botService;
    private final YooKassaCredentialsResolver credentialsResolver;
    private final YooKassaHttpClient yooKassaHttpClient;

    public SubscriptionPaymentCompletionService(SubscriptionPaymentRepository paymentRepository,
                                                SubscriptionRepository subscriptionRepository,
                                                SubscriptionTariffRepository tariffRepository,
                                                SubscriptionService subscriptionService,
                                                BotService botService,
                                                YooKassaCredentialsResolver credentialsResolver,
                                                YooKassaHttpClient yooKassaHttpClient) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tariffRepository = tariffRepository;
        this.subscriptionService = subscriptionService;
        this.botService = botService;
        this.credentialsResolver = credentialsResolver;
        this.yooKassaHttpClient = yooKassaHttpClient;
    }

    @Transactional
    public boolean completeByProviderPaymentId(String yooKassaPaymentId) {
        if (yooKassaPaymentId == null || yooKassaPaymentId.isBlank()) {
            return false;
        }
        Optional<SubscriptionPaymentEntity> locked = paymentRepository.findByProviderPaymentIdForUpdate(yooKassaPaymentId);
        return locked.map(this::finalizeIfPending).orElse(false);
    }

    @Transactional
    public boolean completeByLocalPaymentId(Long paymentRecordId, Long ownerUserId) {
        Optional<SubscriptionPaymentEntity> locked =
                paymentRepository.findByIdAndOwnerUserIdForUpdate(paymentRecordId, ownerUserId);
        return locked.map(this::finalizeIfPending).orElse(false);
    }

    private boolean finalizeIfPending(SubscriptionPaymentEntity pay) {
        if (pay.getStatus() == SubscriptionPaymentStatus.SUCCEEDED) {
            return true;
        }
        if (pay.getStatus() != SubscriptionPaymentStatus.PENDING) {
            return false;
        }
        String providerId = pay.getProviderPaymentId();
        if (providerId == null || providerId.isBlank()) {
            return false;
        }

        BotEntity bot = botService.findById(pay.getBotId());
        if (bot == null) {
            LOGGER.warn("Payment {} references missing bot {}", pay.getId(), pay.getBotId());
            return false;
        }
        Optional<YooKassaCredentials> cred = credentialsResolver.resolve(bot);
        if (cred.isEmpty()) {
            LOGGER.warn("No YooKassa credentials for bot {}", pay.getBotId());
            return false;
        }

        JsonNode remote = yooKassaHttpClient.getPayment(cred.get(), providerId);
        String status = YooKassaHttpClient.paymentStatus(remote);
        if (!"succeeded".equals(status)) {
            LOGGER.debug("Payment {} remote status={}", providerId, status);
            return false;
        }

        SubscriptionTariffEntity tariff = tariffRepository.findById(pay.getTariffId())
                .orElseThrow(() -> new IllegalStateException("Tariff missing id=" + pay.getTariffId()));
        if (tariff.getPaidTermDays() == null || tariff.getPaidTermDays() < 1) {
            throw new IllegalStateException("Tariff has no paid_term_days id=" + tariff.getId());
        }

        SubscriptionEntity sub = subscriptionRepository.findById(pay.getSubscriptionId())
                .orElseThrow(() -> new IllegalStateException("Subscription missing id=" + pay.getSubscriptionId()));

        sub.setTariffId(pay.getTariffId());
        subscriptionRepository.save(sub);

        subscriptionService.grantPayment(sub, tariff.getPaidTermDays(), null,
                "yookassa:" + providerId);

        pay.setStatus(SubscriptionPaymentStatus.SUCCEEDED);
        pay.setCompletedAt(OffsetDateTime.now());
        paymentRepository.save(pay);
        LOGGER.info("Payment succeeded localId={} providerId={} subscriptionId={}",
                pay.getId(), providerId, sub.getId());
        return true;
    }
}
