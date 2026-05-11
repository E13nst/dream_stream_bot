package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.service.memory.ChatMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Обрабатывает {@code edited_message}: обновляет соответствующую запись в chat_memory.
 * Если в истории такого сообщения нет (например, было до запуска фичи или
 * вне зоны сохранения) — операция тихо игнорируется.
 */
@Component
public class EditedMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditedMessageHandler.class);

    private final ChatMemoryService chatMemoryService;

    public EditedMessageHandler(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    public boolean apply(String conversationId, Message editedMessage) {
        if (editedMessage == null || !editedMessage.hasText()) {
            return false;
        }
        try {
            boolean updated = chatMemoryService.applyEdit(
                    conversationId, editedMessage.getMessageId(), editedMessage.getText());
            if (!updated) {
                LOGGER.info("✏️ Edit ignored (no matching record) | conv={} | tg_msg_id={}",
                        conversationId, editedMessage.getMessageId());
            }
            return updated;
        } catch (Exception e) {
            LOGGER.error("❌ Failed to apply edited_message | conv={} | tg_msg_id={} | error={}",
                    conversationId, editedMessage.getMessageId(), e.getMessage(), e);
            return false;
        }
    }
}
