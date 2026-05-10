package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.user.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AssistantBot extends AbstractTelegramBot {
    public AssistantBot(Long botId, BotService botService,
                       MessageHandlerService messageHandlerService, UserService userService) {
        super(botId, botService, messageHandlerService, userService);
    }

    @Override
    public void onUpdateReceived(Update update) {
        BotEntity botEntity = getBotEntity();
        if (botEntity == null) {
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            ensureUserExists(msg.getFrom());
            String conversationId = getConversationId(msg.getChatId());
            boolean isGroup = msg.isGroupMessage() || msg.isSuperGroupMessage();
            boolean isReplyToBot = msg.getReplyToMessage() != null &&
                    msg.getReplyToMessage().getFrom() != null &&
                    msg.getReplyToMessage().getFrom().getUserName() != null &&
                    msg.getReplyToMessage().getFrom().getUserName().equalsIgnoreCase(getBotUsername());
            boolean isMention = msg.getText().toLowerCase().contains("@" + getBotUsername().toLowerCase());
            boolean isName = msg.getText().toLowerCase().contains(botEntity.getName().toLowerCase());
            boolean isAlias = botEntity.getBotAliasesList().stream().anyMatch(alias -> !alias.isEmpty() && msg.getText().toLowerCase().contains(alias.toLowerCase()));
            boolean isTrigger = botEntity.getBotTriggersList().stream().anyMatch(trigger -> !trigger.isEmpty() && msg.getText().toLowerCase().contains(trigger.toLowerCase()));
            if (isGroup) {
                if (!(isReplyToBot || isMention || isName || isAlias || isTrigger)) {
                    return;
                }
                var responses = messageHandlerService.handleReplyToBotMessage(msg, conversationId, botEntity);
                for (var response : responses) {
                    sendWithLogging(response);
                }
            } else {
                var responses = messageHandlerService.handlePersonalMessage(msg, conversationId, botEntity);
                for (var response : responses) {
                    sendWithLogging(response);
                }
            }
        }
    }
}
