package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackDispatcher;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.command.CommandDispatcher;
import com.example.dream_stream_bot.bot.error.BotUpdateErrorHandler;
import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public abstract class AbstractTelegramBot extends TelegramLongPollingBot {
    protected final Long botId;
    protected final BotService botService;
    protected MessageHandlerService messageHandlerService;
    protected final UserService userService;
    protected final MessageSender messageSender;
    protected final CommandDispatcher commandDispatcher;
    protected final CallbackDispatcher callbackDispatcher;
    protected final BotUpdateErrorHandler errorHandler;
    protected final EditedMessageHandler editedMessageHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTelegramBot.class);

    public AbstractTelegramBot(Long botId,
                               BotService botService,
                               MessageHandlerService messageHandlerService,
                               UserService userService,
                               MessageSender messageSender,
                               CommandDispatcher commandDispatcher,
                               CallbackDispatcher callbackDispatcher,
                               BotUpdateErrorHandler errorHandler,
                               EditedMessageHandler editedMessageHandler) {
        this.botId = botId;
        this.botService = botService;
        this.messageHandlerService = messageHandlerService;
        this.userService = userService;
        this.messageSender = messageSender;
        this.commandDispatcher = commandDispatcher;
        this.callbackDispatcher = callbackDispatcher;
        this.errorHandler = errorHandler;
        this.editedMessageHandler = editedMessageHandler;
    }

    public Long getBotId() {
        return botId;
    }

    /** Fresh row from DB (cached); used so prompt / keywords / token changes apply without restart. */
    protected BotEntity getBotEntity() {
        return botService.findById(botId);
    }

    /**
     * Гарантирует, что для пользователя Telegram существует запись в БД.
     * Возвращает entity (или {@code null}, если from — это бот).
     */
    protected UserEntity ensureUserExists(User from) {
        if (from == null || Boolean.TRUE.equals(from.getIsBot())) {
            return null;
        }
        try {
            return userService.findOrCreateByTelegramId(from.getId(), from.getUserName(), from.getFirstName(), from.getLastName());
        } catch (Exception e) {
            LOGGER.error("❌ Failed to register user telegramId={} | Error: {}", from.getId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        BotEntity b = getBotEntity();
        return b != null ? b.getUsername() : "";
    }

    @Override
    public String getBotToken() {
        BotEntity b = getBotEntity();
        return b != null ? b.getToken() : "";
    }

    @Override
    public final void onUpdateReceived(Update update) {
        if (errorHandler != null) {
            errorHandler.handle(this, getBotUsername(), update, this::dispatchUpdate);
        } else {
            dispatchUpdate(update);
        }
    }

    /**
     * Базовая маршрутизация: edited_message и callback_query обрабатываются централизованно,
     * остальное — отдаётся в реализацию бота {@link #doHandleUpdate(Update)}.
     */
    private void dispatchUpdate(Update update) {
        if (update.hasEditedMessage() && editedMessageHandler != null) {
            Message edited = update.getEditedMessage();
            editedMessageHandler.apply(buildConversationId(edited), edited);
            return;
        }
        if (update.hasCallbackQuery() && callbackDispatcher != null) {
            CallbackContext ctx = new CallbackContext(
                    update.getCallbackQuery(),
                    getBotEntity(),
                    this,
                    getBotUsername(),
                    ensureUserExists(update.getCallbackQuery().getFrom()),
                    null,
                    null);
            if (callbackDispatcher.tryDispatch(ctx)) {
                return;
            }
        }
        doHandleUpdate(update);
    }

    /** Реализация шаблонного метода — конкретные боты определяют только маршрутизацию апдейта. */
    protected abstract void doHandleUpdate(Update update);

    /**
     * Базовый id разговора. Подробная схема (с учётом threads) — см. {@link #buildConversationId(Message)}.
     * Сохраняется ради обратной совместимости с существующими ботами; новые потоки должны
     * использовать {@code buildConversationId(Message)}.
     */
    protected String getConversationId(Long chatId) {
        return buildConversationId(chatId, null, null, false);
    }

    /**
     * Конструирует id разговора по сообщению.
     * Схема: {@code bot:<bot_id>:chat:<chat_id>[:thread:<thread_id>]:user:<user_id>}.
     */
    protected String buildConversationId(Message message) {
        if (message == null) {
            return buildConversationId(null, null, null, false);
        }
        Long userId = message.getFrom() != null ? message.getFrom().getId() : null;
        Integer threadId = Boolean.TRUE.equals(message.getIsTopicMessage()) ? message.getMessageThreadId() : null;
        boolean isPrivate = message.isUserMessage();
        return buildConversationId(message.getChatId(), threadId, userId, isPrivate);
    }

    /**
     * @param isPrivate {@code message.isUserMessage()} — для лички схема {@code bot:<id>:user:<tgId>} без chat/thread.
     */
    protected String buildConversationId(Long chatId, Integer threadId, Long userId, boolean isPrivate) {
        StringBuilder sb = new StringBuilder("bot:").append(botId);
        if (isPrivate && userId != null) {
            sb.append(":user:").append(userId);
            return sb.toString();
        }
        if (chatId != null) {
            sb.append(":chat:").append(chatId);
        }
        if (threadId != null) {
            sb.append(":thread:").append(threadId);
        }
        if (userId != null) {
            sb.append(":user:").append(userId);
        }
        return sb.toString();
    }

    /**
     * Пытается распознать команду в сообщении и передать обработку зарегистрированному
     * {@link com.example.dream_stream_bot.bot.command.BotCommand}.
     * Возвращает {@code true}, если команда была обработана; в этом случае дальнейшую
     * обработку апдейта проводить не нужно.
     */
    protected boolean tryDispatchCommand(Update update) {
        if (commandDispatcher == null) {
            return false;
        }
        Message message = update.getMessage();
        if (message == null || message.getText() == null) {
            return false;
        }
        UserEntity user = ensureUserExists(message.getFrom());
        ChatScope scope = ChatScope.fromMessageType(
                message.isUserMessage(),
                message.isGroupMessage(),
                message.isSuperGroupMessage(),
                message.isChannelMessage());
        CommandContext ctx = new CommandContext(
                update, message, getBotEntity(), this, getBotUsername(), user, null, null, scope);
        return commandDispatcher.tryDispatch(ctx);
    }

    /**
     * Backward-compatible хелпер. Внутри проксирует через {@link MessageSender},
     * который автоматически проставляет {@code message_thread_id}, если он задан.
     */
    protected void sendWithLogging(SendMessage message) {
        OutgoingMessage.Builder b = OutgoingMessage.builder()
                .chatId(Long.valueOf(message.getChatId()))
                .text(message.getText());
        if (message.getMessageThreadId() != null) {
            b.messageThreadId(message.getMessageThreadId());
        }
        if (message.getReplyToMessageId() != null) {
            b.replyToMessageId(message.getReplyToMessageId());
        }
        if (message.getParseMode() != null) {
            b.parseMode(message.getParseMode());
        }
        if (message.getReplyMarkup() != null) {
            b.replyMarkup(message.getReplyMarkup());
        }
        messageSender.send(this, b.build());
    }
}
