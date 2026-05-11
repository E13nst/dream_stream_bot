package com.example.dream_stream_bot.service.consent;

import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.TelegramBotApiService;
import com.example.dream_stream_bot.service.user.UserService;
import com.example.dream_stream_bot.service.subscription.SubscriptionTariffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Уведомления о публикации новой версии документа: владельцу в личку + одно сообщение в группу (если подписка групповая).
 */
@Service
public class ConsentPublicationNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentPublicationNotifier.class);

    private final SubscriptionRepository subscriptionRepository;
    private final BotService botService;
    private final UserService userService;
    private final TelegramBotApiService telegramBotApiService;
    private final SubscriptionTariffService subscriptionTariffService;

    public ConsentPublicationNotifier(SubscriptionRepository subscriptionRepository,
                                      BotService botService,
                                      UserService userService,
                                      TelegramBotApiService telegramBotApiService,
                                      SubscriptionTariffService subscriptionTariffService) {
        this.subscriptionRepository = subscriptionRepository;
        this.botService = botService;
        this.userService = userService;
        this.telegramBotApiService = telegramBotApiService;
        this.subscriptionTariffService = subscriptionTariffService;
    }

    public void notifyPublished(ConsentDocumentEntity doc) {
        if (doc == null) {
            return;
        }
        List<SubscriptionEntity> subs = subscriptionRepository.findAll();
        Set<String> groupNotified = new HashSet<>();
        for (SubscriptionEntity sub : subs) {
            if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
                continue;
            }
            BotEntity bot = botService.findById(sub.getBotId());
            if (bot == null) {
                continue;
            }
            Optional<UserEntity> owner = userService.findById(sub.getOwnerUserId());
            if (owner.isEmpty()) {
                continue;
            }
            String un = bot.getUsername() != null ? bot.getUsername() : "";
            String msg = "Опубликована новая версия документа «" + doc.getTitle() + "» (v" + doc.getVersion() + ").\n"
                    + "Откройте @" + un + " и при необходимости подтвердите согласия через /start.";
            Map<String, Object> markup = inlineOpenBotUrl(un);
            telegramBotApiService.sendTextMessage(bot, owner.get().getTelegramId(), msg, markup);

            if (subscriptionTariffService.isGroupTariff(sub.getTariffId()) && sub.getScopeChatId() != null) {
                String gk = sub.getBotId() + ":" + sub.getScopeChatId();
                if (groupNotified.add(gk)) {
                    String gtext = "Обновлена версия документа «" + doc.getTitle() + "». Участникам может потребоваться подтвердить согласия в личке с ботом.";
                    telegramBotApiService.sendTextMessage(bot, sub.getScopeChatId(), gtext, null);
                }
            }
        }
        LOGGER.info("📨 Consent publication notifications sent for {} v{}", doc.getCode(), doc.getVersion());
    }

    private static Map<String, Object> inlineOpenBotUrl(String username) {
        if (username == null || username.isBlank()) {
            return Map.of();
        }
        String url = "https://t.me/" + username;
        Map<String, Object> open = new HashMap<>();
        open.put("text", "Открыть бота");
        open.put("url", url);
        return Map.of("inline_keyboard", List.of(List.of(open)));
    }
}
