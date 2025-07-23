package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AssistantBot extends AbstractTelegramBot {
    public AssistantBot(BotEntity botEntity, MessageHandlerService messageHandlerService) {
        super(botEntity, messageHandlerService);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            String conversationId = getConversationId(msg.getChatId());
            var responses = messageHandlerService.handlePersonalMessage(msg, conversationId, botEntity);
            for (var response : responses) {
                sendWithLogging(response);
            }
        }
    }
} 