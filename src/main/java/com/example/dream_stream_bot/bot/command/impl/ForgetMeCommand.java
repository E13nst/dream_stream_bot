package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.privacy.ErasureResult;
import com.example.dream_stream_bot.service.privacy.UserDataErasureService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * /forget_me — полное удаление данных пользователя в рамках этого бота:
 * подписки, согласия, память диалога, участие в группах; фиксация факта в журнале.
 */
@Component
public class ForgetMeCommand implements BotCommand {

    private final UserDataErasureService userDataErasureService;
    private final BotNavigationService botNavigationService;

    public ForgetMeCommand(UserDataErasureService userDataErasureService,
                          BotNavigationService botNavigationService) {
        this.userDataErasureService = userDataErasureService;
        this.botNavigationService = botNavigationService;
    }

    @Override
    public String name() {
        return "forget_me";
    }

    @Override
    public boolean appliesIn(ChatScope scope) {
        return scope == ChatScope.PRIVATE;
    }

    @Override
    public Optional<String> menuDescription() {
        return Optional.of("Отозвать согласия и удалить мои данные");
    }

    @Override
    public Set<ChatScope> menuScopes() {
        return Set.of(ChatScope.PRIVATE);
    }

    @Override
    public List<OutgoingMessage> handle(CommandContext ctx) {
        Message message = ctx.getMessage();
        UserEntity user = ctx.getUser();
        BotEntity bot = ctx.getBotEntity();
        Long botId = bot != null ? bot.getId() : null;
        Long tgUserId = message.getFrom() != null ? message.getFrom().getId() : null;
        if (user == null || botId == null || tgUserId == null) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("Не удалось выполнить запрос. Напишите /forget_me в чате с ботом после /start.")
                    .build());
        }

        ErasureResult result = userDataErasureService.eraseForBot(botId, user.getId(), tgUserId);
        String text = formatResult(result);

        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
    }

    private static String formatResult(ErasureResult result) {
        if (result.alreadyErased()) {
            return "Данные по этому боту уже были удалены ранее. При необходимости начните заново через /start.";
        }
        return "Выполнено: удалено подписок: " + result.subscriptionsDeleted()
                + "; записей участия в группах: " + result.participantsRemoved()
                + "; записей согласий: " + result.consentsDeleted()
                + "; реферальных начислений: " + result.referralGrantsDeleted()
                + "; сообщений из памяти диалога: " + result.chatMemoryRowsDeleted()
                + ".";
    }
}
