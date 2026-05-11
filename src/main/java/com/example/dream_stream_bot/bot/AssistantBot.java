package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.bot.command.CallbackDispatcher;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandDispatcher;
import com.example.dream_stream_bot.bot.error.BotUpdateErrorHandler;
import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.access.AccessDecision;
import com.example.dream_stream_bot.service.access.AccessGate;
import com.example.dream_stream_bot.service.access.AccessReason;
import com.example.dream_stream_bot.service.access.GatingDedup;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.user.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AssistantBot extends AbstractTelegramBot {

    private final AccessGate accessGate;
    private final GatingDedup gatingDedup;

    public AssistantBot(Long botId, BotService botService,
                        MessageHandlerService messageHandlerService, UserService userService,
                        MessageSender messageSender, CommandDispatcher commandDispatcher,
                        CallbackDispatcher callbackDispatcher,
                        BotUpdateErrorHandler errorHandler,
                        EditedMessageHandler editedMessageHandler,
                        AccessGate accessGate,
                        GatingDedup gatingDedup) {
        super(botId, botService, messageHandlerService, userService, messageSender, commandDispatcher,
                callbackDispatcher, errorHandler, editedMessageHandler);
        this.accessGate = accessGate;
        this.gatingDedup = gatingDedup;
    }

    @Override
    protected void doHandleUpdate(Update update) {
        BotEntity botEntity = getBotEntity();
        if (botEntity == null) {
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        if (tryDispatchCommand(update)) {
            return;
        }

        Message msg = update.getMessage();
        ensureUserExists(msg.getFrom());
        ChatScope scope = ChatScope.fromMessageType(
                msg.isUserMessage(), msg.isGroupMessage(), msg.isSuperGroupMessage(), msg.isChannelMessage());

        AccessDecision decision = accessGate.evaluate(botEntity, msg, scope, getBotUsername());
        if (!decision.isAllowed()) {
            sendStubIfNeeded(msg, decision);
            return;
        }

        String conversationId = buildConversationId(msg);
        var responses = scope.isGroupLike()
                ? messageHandlerService.handleReplyToBotMessage(msg, conversationId, botEntity)
                : messageHandlerService.handlePersonalMessage(msg, conversationId, botEntity);
        messageSender.sendAll(this, responses);

        if (decision.hasUserMessage()) {
            sendStubIfNeeded(msg, decision);
        }
    }

    private void sendStubIfNeeded(Message msg, AccessDecision decision) {
        if (!decision.hasUserMessage()) {
            return;
        }
        Long userId = msg.getFrom() != null ? msg.getFrom().getId() : 0L;
        String key = GatingDedup.key("stub", botId, msg.getChatId(), userId, decision.getReason().name());
        if (!gatingDedup.acquire(key)) {
            return;
        }
        Integer threadId = Boolean.TRUE.equals(msg.getIsTopicMessage()) ? msg.getMessageThreadId() : null;
        messageSender.send(this, OutgoingMessage.builder()
                .chatId(msg.getChatId())
                .messageThreadId(threadId)
                .text(decision.getUserMessage())
                .build());
    }

    @SuppressWarnings("unused")
    private static boolean isReasonSilent(AccessReason reason) {
        return reason == AccessReason.GROUP_TRIGGER_NOT_MATCHED;
    }
}
