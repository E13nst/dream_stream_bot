package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.access.GatingDedup;
import com.example.dream_stream_bot.service.telegram.TelegramBotApiService;
import com.example.dream_stream_bot.service.user.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Мягкое предупреждение владельцу групповой подписки при превышении лимита участников за месяц.
 */
@Service
public class OwnerParticipantLimitNotifier {

    private final UserService userService;
    private final TelegramBotApiService telegramBotApiService;
    private final GatingDedup gatingDedup;

    public OwnerParticipantLimitNotifier(UserService userService,
                                        TelegramBotApiService telegramBotApiService,
                                        GatingDedup gatingDedup) {
        this.userService = userService;
        this.telegramBotApiService = telegramBotApiService;
        this.gatingDedup = gatingDedup;
    }

    public void notifySoftLimitExceeded(BotEntity bot, SubscriptionEntity subscription, long active, int max) {
        String key = GatingDedup.key("participant_soft", bot.getId(), subscription.getScopeChatId(),
                subscription.getOwnerUserId(), "cap");
        if (!gatingDedup.acquire(key)) {
            return;
        }
        Optional<UserEntity> owner = userService.findById(subscription.getOwnerUserId());
        if (owner.isEmpty()) {
            return;
        }
        String text = "👥 По подписке группы в чате " + subscription.getScopeChatId()
                + " за расчётный месяц зафиксировано " + active + " активных участников при лимите "
                + max + ". Рассмотрите апгрейд тарифа или сокращение числа обращающихся к боту.";
        telegramBotApiService.sendTextMessage(bot, owner.get().getTelegramId(), text, null);
    }
}
