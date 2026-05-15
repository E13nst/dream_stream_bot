package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.payment.ReceiptEmailAwaitService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import com.example.dream_stream_bot.service.user.UserService;
import com.example.dream_stream_bot.util.EmailPatterns;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сохранение email для чека ЮKassa (54‑ФЗ), если у бота включён чек.
 */
@Component
public class BillingEmailCommand implements BotCommand {

    private final UserService userService;
    private final BotNavigationService botNavigationService;
    private final ReceiptEmailAwaitService receiptEmailAwaitService;

    public BillingEmailCommand(UserService userService,
                               BotNavigationService botNavigationService,
                               ReceiptEmailAwaitService receiptEmailAwaitService) {
        this.userService = userService;
        this.botNavigationService = botNavigationService;
        this.receiptEmailAwaitService = receiptEmailAwaitService;
    }

    @Override
    public String name() {
        return "billing_email";
    }

    @Override
    public boolean appliesIn(ChatScope scope) {
        return scope == ChatScope.PRIVATE;
    }

    @Override
    public Optional<String> menuDescription() {
        return Optional.empty();
    }

    @Override
    public Set<ChatScope> menuScopes() {
        return Set.of();
    }

    @Override
    public List<OutgoingMessage> handle(CommandContext ctx) {
        Message message = ctx.getMessage();
        UserEntity user = ctx.getUser();
        if (user == null) {
            return BotCommand.reply(OutgoingMessage.of(message.getChatId(),
                    "Не удалось определить профиль пользователя."));
        }
        String raw = ctx.getArgs() == null ? "" : ctx.getArgs().trim();
        if (raw.isEmpty()) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("Email для чека запрашивается после нажатия «Оплатить» в подписках: следующим сообщением "
                            + "отправьте только адрес (например name@example.ru).\n\n"
                            + "Либо сохраните его заранее одной командой:\n/billing_email name@example.ru")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        if (!EmailPatterns.isValidBillingEmail(raw)) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("Похоже, это не email. Пример: /billing_email name@example.ru")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        userService.updateBillingEmail(user.getId(), raw);
        if (ctx.getBotEntity() != null) {
            receiptEmailAwaitService.clearAwaiting(ctx.getBotEntity().getId(), user.getId());
        }
        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text("Email сохранён. Можете вернуться к оплате.")
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
    }
}
