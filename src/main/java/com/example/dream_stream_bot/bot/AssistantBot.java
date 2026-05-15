package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.bot.command.CallbackDispatcher;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.command.CommandDispatcher;
import com.example.dream_stream_bot.bot.command.PrivateReplyNavigationRouter;
import com.example.dream_stream_bot.bot.error.BotUpdateErrorHandler;
import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.MessageSender.TypingKeepAliveHandle;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.access.AccessDecision;
import com.example.dream_stream_bot.service.access.AccessGate;
import com.example.dream_stream_bot.service.access.AccessReason;
import com.example.dream_stream_bot.service.access.GatingDedup;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.payment.ReceiptEmailAwaitService;
import com.example.dream_stream_bot.service.subscription.GroupLinkWizardService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.user.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

public class AssistantBot extends AbstractTelegramBot {

    private final AccessGate accessGate;
    private final GatingDedup gatingDedup;
    private final PrivateReplyNavigationRouter privateReplyNavigationRouter;
    private final GroupLinkWizardService groupLinkWizardService;
    private final ReceiptEmailAwaitService receiptEmailAwaitService;

    public AssistantBot(Long botId, BotService botService,
                        MessageHandlerService messageHandlerService, UserService userService,
                        MessageSender messageSender, CommandDispatcher commandDispatcher,
                        CallbackDispatcher callbackDispatcher,
                        BotUpdateErrorHandler errorHandler,
                        EditedMessageHandler editedMessageHandler,
                        AccessGate accessGate,
                        GatingDedup gatingDedup,
                        PrivateReplyNavigationRouter privateReplyNavigationRouter,
                        GroupLinkWizardService groupLinkWizardService,
                        ReceiptEmailAwaitService receiptEmailAwaitService) {
        super(botId, botService, messageHandlerService, userService, messageSender, commandDispatcher,
                callbackDispatcher, errorHandler, editedMessageHandler);
        this.accessGate = accessGate;
        this.gatingDedup = gatingDedup;
        this.privateReplyNavigationRouter = privateReplyNavigationRouter;
        this.groupLinkWizardService = groupLinkWizardService;
        this.receiptEmailAwaitService = receiptEmailAwaitService;
    }

    @Override
    protected void doHandleUpdate(Update update) {
        BotEntity botEntity = getBotEntity();
        if (botEntity == null) {
            return;
        }
        if (!update.hasMessage()) {
            return;
        }
        Message msg = update.getMessage();
        var user = ensureUserExists(msg.getFrom());
        ChatScope scope = ChatScope.fromMessageType(
                msg.isUserMessage(), msg.isGroupMessage(), msg.isSuperGroupMessage(), msg.isChannelMessage());

        if (scope == ChatScope.PRIVATE && user != null) {
            if (msg.hasText()) {
                String raw = msg.getText();
                // Команды (/start, /subscriptions, …) должны обрабатываться CommandDispatcher, а не напоминанием мастера.
                if (!raw.isBlank() && !raw.stripLeading().startsWith("/")) {
                    Integer threadId = Boolean.TRUE.equals(msg.getIsTopicMessage()) ? msg.getMessageThreadId() : null;
                    Optional<List<OutgoingMessage>> receipt = receiptEmailAwaitService.tryHandlePlainText(
                            msg.getChatId(), threadId, botEntity, user, raw);
                    if (receipt.isPresent()) {
                        messageSender.sendAll(this, receipt.get());
                        return;
                    }
                    Optional<List<OutgoingMessage>> remind =
                            groupLinkWizardService.tryPlainTextReminder(msg.getChatId(), botEntity, user, raw);
                    if (remind.isPresent()) {
                        messageSender.sendAll(this, remind.get());
                        return;
                    }
                }
            }
        }

        if (!msg.hasText()) {
            return;
        }
        if (tryDispatchCommand(update)) {
            return;
        }

        if (scope == ChatScope.PRIVATE) {
            CommandContext cmdCtx = new CommandContext(
                    update, msg, botEntity, this, getBotUsername(), user, null, null, scope);
            if (privateReplyNavigationRouter.tryRoute(cmdCtx)) {
                return;
            }
        }

        AccessDecision decision = accessGate.evaluate(botEntity, msg, scope, getBotUsername());
        if (!decision.isAllowed()) {
            sendStubIfNeeded(msg, decision);
            return;
        }

        String conversationId = buildConversationId(msg);
        Integer threadId = Boolean.TRUE.equals(msg.getIsTopicMessage()) ? msg.getMessageThreadId() : null;
        List<OutgoingMessage> responses;
        try (TypingKeepAliveHandle typing = messageSender.startTypingKeepAlive(this, msg.getChatId(), threadId)) {
            responses = scope.isGroupLike()
                    ? messageHandlerService.handleReplyToBotMessage(msg, conversationId, botEntity)
                    : messageHandlerService.handlePersonalMessage(msg, conversationId, botEntity);
        }
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
