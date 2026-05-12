package com.example.dream_stream_bot.service.payment;

import com.example.dream_stream_bot.config.properties.YooKassaProperties;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.model.user.UserRepository;
import com.example.dream_stream_bot.service.subscription.SubscriptionTariffService;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionCheckoutService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCheckoutService.class);

    private final BotService botService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTariffService tariffService;
    private final SubscriptionPaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final YooKassaCredentialsResolver credentialsResolver;
    private final YooKassaHttpClient yooKassaHttpClient;
    private final YooKassaProperties yooKassaProperties;

    public SubscriptionCheckoutService(BotService botService,
                                       SubscriptionRepository subscriptionRepository,
                                       SubscriptionTariffService tariffService,
                                       SubscriptionPaymentRepository paymentRepository,
                                       UserRepository userRepository,
                                       YooKassaCredentialsResolver credentialsResolver,
                                       YooKassaHttpClient yooKassaHttpClient,
                                       YooKassaProperties yooKassaProperties) {
        this.botService = botService;
        this.subscriptionRepository = subscriptionRepository;
        this.tariffService = tariffService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.credentialsResolver = credentialsResolver;
        this.yooKassaHttpClient = yooKassaHttpClient;
        this.yooKassaProperties = yooKassaProperties;
    }

    /**
     * Результат создания платежа: наш внутренний id строки и URL оплаты.
     */
    public record CheckoutResult(long localPaymentId, String confirmationUrl) {}

    @Transactional
    public CheckoutResult createCheckout(Long botId, Long ownerUserId, Long tariffId) {
        BotEntity bot = botService.findById(botId);
        if (bot == null) {
            throw new IllegalArgumentException("Бот не найден.");
        }
        YooKassaCredentials credentials = credentialsResolver.resolve(bot)
                .orElseThrow(() -> new IllegalStateException(
                        "Оплата не настроена для этого бота. Обратитесь к поддержке."));

        SubscriptionEntity subscription = subscriptionRepository.findPersonal(botId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Подписка не найдена. Нажмите /start в этом боте."));

        validateSubscriptionStatus(subscription);

        SubscriptionTariffEntity tariff = tariffService.requireForBot(botId, tariffId);
        validateTariffForCheckout(tariff);

        if (!subscription.getTariffId().equals(tariffId)) {
            subscription.setTariffId(tariffId);
            subscriptionRepository.save(subscription);
        }

        UserEntity user = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        if (bot.isYookassaReceiptEnabled()) {
            String email = user.getBillingEmail();
            if (email == null || email.isBlank()) {
                throw new IllegalStateException(
                        "Для оплаты с чеком укажите email: команда /billing_email ваш@email.ru");
            }
        }

        long amountMinor = tariff.getPriceAmountMinor();
        String currency = tariff.getCurrency() != null ? tariff.getCurrency().trim().toUpperCase() : "RUB";
        String idempotencyKey = UUID.randomUUID().toString();

        SubscriptionPaymentEntity row = new SubscriptionPaymentEntity();
        row.setSubscriptionId(subscription.getId());
        row.setTariffId(tariffId);
        row.setBotId(botId);
        row.setOwnerUserId(ownerUserId);
        row.setProvider("yookassa");
        row.setIdempotencyKey(idempotencyKey);
        row.setAmountMinor(amountMinor);
        row.setCurrency(currency);
        row.setStatus(SubscriptionPaymentStatus.PENDING);
        row = paymentRepository.save(row);

        String amountStr = formatAmountMinor(amountMinor);
        String itemTitle = tariff.getCheckoutDescription() != null && !tariff.getCheckoutDescription().isBlank()
                ? tariff.getCheckoutDescription().trim()
                : tariff.getTitle();
        String description = "Подписка «" + tariff.getTitle() + "»";

        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("value", amountStr);
        amount.put("currency", currency);
        body.put("amount", amount);
        Map<String, Object> confirmation = new LinkedHashMap<>();
        confirmation.put("type", "redirect");
        confirmation.put("return_url", yooKassaProperties.resolveReturnUrl(bot.getUsername()));
        body.put("confirmation", confirmation);
        body.put("capture", Boolean.TRUE);
        body.put("description", description);

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("subscription_id", String.valueOf(subscription.getId()));
        metadata.put("tariff_id", String.valueOf(tariffId));
        metadata.put("bot_id", String.valueOf(botId));
        metadata.put("owner_user_id", String.valueOf(ownerUserId));
        metadata.put("payment_record_id", String.valueOf(row.getId()));
        body.put("metadata", metadata);

        if (bot.isYookassaReceiptEnabled()) {
            body.put("receipt", buildReceipt(user.getBillingEmail().trim(), itemTitle, amountStr, currency));
        }

        try {
            JsonNode resp = yooKassaHttpClient.createPayment(credentials, idempotencyKey, body);
            String paymentId = YooKassaHttpClient.paymentId(resp);
            String url = YooKassaHttpClient.confirmationUrl(resp);
            if (paymentId == null || url == null) {
                throw new IllegalStateException("ЮKassa вернула неполный ответ");
            }
            row.setProviderPaymentId(paymentId);
            paymentRepository.save(row);
            LOGGER.info("Created YooKassa payment localId={} providerId={}", row.getId(), paymentId);
            return new CheckoutResult(row.getId(), url);
        } catch (RuntimeException e) {
            row.setStatus(SubscriptionPaymentStatus.FAILED);
            paymentRepository.save(row);
            throw e;
        }
    }

    /**
     * Есть ли у бота цена у тарифа и настроены глобально/локально креды — для показа CTA оплаты.
     */
    public boolean isPaidCheckoutAvailable(BotEntity bot, SubscriptionTariffEntity tariff) {
        if (bot == null || tariff == null) {
            return false;
        }
        if (!bot.getId().equals(tariff.getBotId())) {
            return false;
        }
        if (!tariff.isActive() || tariff.getAccessMode() != TariffAccessMode.PAID_TERM) {
            return false;
        }
        if (tariff.getPriceAmountMinor() == null || tariff.getPriceAmountMinor() <= 0) {
            return false;
        }
        return credentialsResolver.resolve(bot).isPresent();
    }

    private static void validateSubscriptionStatus(SubscriptionEntity subscription) {
        SubscriptionStatus st = subscription.getStatus();
        if (st == SubscriptionStatus.PENDING_CONSENT || st == SubscriptionStatus.BLOCKED_CONSENT
                || st == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Сначала завершите онбординг и примите документы (/start).");
        }
    }

    private static void validateTariffForCheckout(SubscriptionTariffEntity tariff) {
        if (!Boolean.TRUE.equals(tariff.isActive())) {
            throw new IllegalArgumentException("Этот тариф недоступен.");
        }
        if (tariff.getScope() != TariffScope.PERSONAL) {
            throw new IllegalArgumentException("Оплата в боте доступна только для персональных тарифов.");
        }
        if (tariff.getAccessMode() != TariffAccessMode.PAID_TERM) {
            throw new IllegalArgumentException("Тариф не предполагает оплату срока.");
        }
        if (tariff.getPriceAmountMinor() == null || tariff.getPriceAmountMinor() <= 0
                || tariff.getPaidTermDays() == null || tariff.getPaidTermDays() < 1) {
            throw new IllegalArgumentException("Для тарифа не задана цена или срок оплаченного периода.");
        }
    }

    private static String formatAmountMinor(long minor) {
        return BigDecimal.valueOf(minor).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static Map<String, Object> buildReceipt(String email, String itemTitle, String amountStr, String currency) {
        Map<String, Object> receipt = new LinkedHashMap<>();
        Map<String, String> customer = new LinkedHashMap<>();
        customer.put("email", email);
        receipt.put("customer", customer);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("description", itemTitle.length() > 128 ? itemTitle.substring(0, 128) : itemTitle);
        item.put("quantity", "1");
        Map<String, Object> itemAmount = new LinkedHashMap<>();
        itemAmount.put("value", amountStr);
        itemAmount.put("currency", currency);
        item.put("amount", itemAmount);
        item.put("vat_code", 1);
        item.put("payment_mode", "full_payment");
        item.put("payment_subject", "service");
        receipt.put("items", java.util.List.of(item));
        return receipt;
    }
}
