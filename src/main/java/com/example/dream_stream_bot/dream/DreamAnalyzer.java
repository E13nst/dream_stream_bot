package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.ChatSession;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

public class DreamAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAnalyzer.class);

    @Getter
    private final ChatSession openaiChat;
    @Getter
    private final String userName;
    @Getter
    private final Dream dream = new Dream();
    @Getter
    private final long telegramChatId;

    private AnalyzerState state;

    public DreamAnalyzer(ChatSession openaiChat, String userName, long telegramChatId) {
        this.openaiChat = openaiChat;
        this.telegramChatId = telegramChatId;
        this.userName = userName;
        this.state = new DreamNew();
    }

    public void setState(AnalyzerState state) {
        this.state = state;
    }

    public void next() {
        state.next(this);
    }

    public void previous() {
        state.prev(this);
    }

    public DreamStatus getState() {
        return state.getState();
    }

    public List<SendMessage> execute(String text) {
            return state.execute(this, text);
    }

    public List<SendMessage> init() {
        return state.init(this);
    }

    public SendMessage newTelegramMessage(String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(getTelegramChatId());
        sendMessage.setText(text);
        return sendMessage;
    }
}
