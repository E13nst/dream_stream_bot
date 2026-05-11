package com.example.dream_stream_bot.bot.message;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

/**
 * Описание исходящего сообщения, не привязанное к конкретному API.
 * Учитывает форум-топики ({@code messageThreadId}) и reply-цитирование.
 */
public final class OutgoingMessage {

    private final Long chatId;
    private final Integer messageThreadId;
    private final Integer replyToMessageId;
    private final String text;
    private final String parseMode;
    private final ReplyKeyboard replyMarkup;
    private final boolean disableWebPagePreview;

    private OutgoingMessage(Builder b) {
        this.chatId = b.chatId;
        this.messageThreadId = b.messageThreadId;
        this.replyToMessageId = b.replyToMessageId;
        this.text = b.text;
        this.parseMode = b.parseMode;
        this.replyMarkup = b.replyMarkup;
        this.disableWebPagePreview = b.disableWebPagePreview;
    }

    public Long getChatId() { return chatId; }
    public Integer getMessageThreadId() { return messageThreadId; }
    public Integer getReplyToMessageId() { return replyToMessageId; }
    public String getText() { return text; }
    public String getParseMode() { return parseMode; }
    public ReplyKeyboard getReplyMarkup() { return replyMarkup; }
    public boolean isDisableWebPagePreview() { return disableWebPagePreview; }

    public static Builder builder() {
        return new Builder();
    }

    public static OutgoingMessage of(Long chatId, String text) {
        return builder().chatId(chatId).text(text).build();
    }

    public static OutgoingMessage markdown(Long chatId, String text) {
        return builder().chatId(chatId).text(text).parseMode("Markdown").build();
    }

    public static final class Builder {
        private Long chatId;
        private Integer messageThreadId;
        private Integer replyToMessageId;
        private String text;
        private String parseMode;
        private ReplyKeyboard replyMarkup;
        private boolean disableWebPagePreview;

        public Builder chatId(Long v) { this.chatId = v; return this; }
        public Builder messageThreadId(Integer v) { this.messageThreadId = v; return this; }
        public Builder replyToMessageId(Integer v) { this.replyToMessageId = v; return this; }
        public Builder text(String v) { this.text = v; return this; }
        public Builder parseMode(String v) { this.parseMode = v; return this; }
        public Builder replyMarkup(ReplyKeyboard v) { this.replyMarkup = v; return this; }
        public Builder disableWebPagePreview(boolean v) { this.disableWebPagePreview = v; return this; }

        public OutgoingMessage build() {
            if (chatId == null) {
                throw new IllegalArgumentException("chatId is required");
            }
            if (text == null) {
                throw new IllegalArgumentException("text is required");
            }
            return new OutgoingMessage(this);
        }
    }
}
