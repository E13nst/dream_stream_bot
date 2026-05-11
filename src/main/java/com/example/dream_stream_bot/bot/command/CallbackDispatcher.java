package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CallbackDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackDispatcher.class);

    private final Map<String, CallbackHandler> byPrefix;
    private final MessageSender messageSender;

    public CallbackDispatcher(List<CallbackHandler> handlers, MessageSender messageSender) {
        this.byPrefix = handlers.stream().collect(Collectors.toMap(
                h -> h.prefix().toLowerCase(Locale.ROOT),
                h -> h,
                (a, b) -> {
                    throw new IllegalStateException("Duplicate callback prefix: " + a.prefix());
                }));
        this.messageSender = messageSender;
        LOGGER.info("📋 Registered {} callback handlers: {}", byPrefix.size(), byPrefix.keySet());
    }

    /**
     * Возвращает {@code true}, если callback был обработан.
     */
    public boolean tryDispatch(CallbackContext base) {
        CallbackQuery cq = base.getCallbackQuery();
        if (cq == null || cq.getData() == null) {
            return false;
        }
        String data = cq.getData();
        int sep = data.indexOf(':');
        String prefix = sep < 0 ? data : data.substring(0, sep);
        String payload = sep < 0 ? "" : data.substring(sep + 1);

        CallbackHandler handler = byPrefix.get(prefix.toLowerCase(Locale.ROOT));
        if (handler == null) {
            return false;
        }
        CallbackContext enriched = new CallbackContext(
                cq, base.getBotEntity(), base.getSender(), base.getBotUsername(),
                base.getUser(), prefix, payload);
        try {
            List<OutgoingMessage> responses = handler.handle(enriched);
            answerCallback(base.getSender(), cq.getId());
            if (responses != null && !responses.isEmpty()) {
                messageSender.sendAll(base.getSender(), responses);
            }
        } catch (Exception e) {
            LOGGER.error("❌ Callback {} failed: {}", prefix, e.getMessage(), e);
            answerCallback(base.getSender(), cq.getId());
        }
        return true;
    }

    private void answerCallback(AbsSender sender, String callbackId) {
        try {
            AnswerCallbackQuery a = new AnswerCallbackQuery();
            a.setCallbackQueryId(callbackId);
            sender.execute(a);
        } catch (TelegramApiException e) {
            LOGGER.warn("⚠️ answerCallbackQuery failed: {}", e.getMessage());
        }
    }
}
