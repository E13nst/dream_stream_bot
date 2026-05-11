package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.bot.command.CallbackDispatcher;
import com.example.dream_stream_bot.bot.command.CommandDispatcher;
import com.example.dream_stream_bot.bot.error.BotUpdateErrorHandler;
import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.user.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CopyCatBot extends AbstractTelegramBot {
    public CopyCatBot(Long botId, BotService botService,
                      MessageHandlerService messageHandlerService, UserService userService,
                      MessageSender messageSender, CommandDispatcher commandDispatcher,
                      CallbackDispatcher callbackDispatcher,
                      BotUpdateErrorHandler errorHandler,
                      EditedMessageHandler editedMessageHandler) {
        super(botId, botService, messageHandlerService, userService, messageSender, commandDispatcher,
                callbackDispatcher, errorHandler, editedMessageHandler);
    }

    @Override
    protected void doHandleUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        if (tryDispatchCommand(update)) {
            return;
        }
        Message msg = update.getMessage();
        ensureUserExists(msg.getFrom());
        Integer threadId = Boolean.TRUE.equals(msg.getIsTopicMessage()) ? msg.getMessageThreadId() : null;
        messageSender.send(this, OutgoingMessage.builder()
                .chatId(msg.getChatId())
                .messageThreadId(threadId)
                .text(msg.getText())
                .build());
    }
}
