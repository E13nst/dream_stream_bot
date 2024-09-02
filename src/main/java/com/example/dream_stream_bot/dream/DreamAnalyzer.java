package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.ChatSession;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

public class DreamAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAnalyzer.class);

    @Getter
    private final ChatSession openaiChat;
    @Getter
    private final String userName;
    @Getter
    private final long telegramChatId;
    @Getter
    private final Dream dream;

    private AnalyzerState state;

    @Builder
    public DreamAnalyzer(ChatSession openaiChat, String userName, long telegramChatId, Dream dream) {
        this.openaiChat = openaiChat;
        this.telegramChatId = telegramChatId;
        this.userName = userName;
        this.state = new DreamStart();
        this.dream = dream == null ? new Dream() : dream;
    }

    public void setState(AnalyzerState state) {
        this.state = state;
    }

    public DreamStatus getState() {
        return state.getState();
    }

    public List<SendMessage> next() {
        return state.next(this);
    }

    public void previous() {
        state.prev(this);
    }

    public List<SendMessage> processMessage(String answer) {
        if (answer == null || answer.isBlank())
            LOGGER.warn("Received blank message");
        return state.processMessage(this, answer);
    }

    public List<SendMessage> processMessage() {
        return state.processMessage(this, "");
    }

    public SendMessage newTelegramMessage(String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(getTelegramChatId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(text);
        return sendMessage;
    }

    public SendMessage newTelegramMessage(String text, InlineKeyboardMarkup keyboard) {
        var message = newTelegramMessage(text);
        message.setReplyMarkup(keyboard);
        return message;
    }

}
