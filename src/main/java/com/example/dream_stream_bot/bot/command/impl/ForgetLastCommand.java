package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.memory.ChatMemoryService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;

/**
 * /forget_last — удалить последнюю пару user→assistant из истории диалога.
 *
 * Работает в рамках текущего conversation (личка / группа / форум-топик).
 */
@Component
public class ForgetLastCommand implements BotCommand {

    private final ChatMemoryService chatMemoryService;
    private final BotNavigationService botNavigationService;

    public ForgetLastCommand(ChatMemoryService chatMemoryService, BotNavigationService botNavigationService) {
        this.chatMemoryService = chatMemoryService;
        this.botNavigationService = botNavigationService;
    }

    @Override
    public String name() {
        return "forget_last";
    }

    @Override
    public Optional<String> menuDescription() {
        return Optional.empty();
    }

    @Override
    public List<OutgoingMessage> handle(CommandContext ctx) {
        Message message = ctx.getMessage();
        Long botId = ctx.getBotEntity() != null ? ctx.getBotEntity().getId() : null;
        String conversationId = buildConversationId(botId, message);
        int deleted = chatMemoryService.forgetLast(conversationId);

        String text = deleted > 0
                ? "🗑 Удалил последний ваш запрос и мой ответ из памяти этого диалога."
                : "В памяти этого диалога нечего удалять.";
        Integer threadId = ctx.getMessageThreadId();

        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(threadId)
                .text(text)
                .replyMarkup(message.isUserMessage() ? botNavigationService.privateMainKeyboard() : null)
                .build());
    }

    private static String buildConversationId(Long botId, Message message) {
        Long userId = message.getFrom() != null ? message.getFrom().getId() : null;
        Integer threadId = Boolean.TRUE.equals(message.getIsTopicMessage()) ? message.getMessageThreadId() : null;
        boolean isPrivate = message.isUserMessage();
        StringBuilder sb = new StringBuilder("bot:").append(botId);
        if (isPrivate && userId != null) {
            sb.append(":user:").append(userId);
            return sb.toString();
        }
        if (message.getChatId() != null) {
            sb.append(":chat:").append(message.getChatId());
        }
        if (threadId != null) {
            sb.append(":thread:").append(threadId);
        }
        if (userId != null) {
            sb.append(":user:").append(userId);
        }
        return sb.toString();
    }
}
