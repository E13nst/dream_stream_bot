package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Распознаёт команды в сообщениях и делегирует обработку в соответствующий {@link BotCommand}.
 * Поддерживает {@code /command} и {@code /command@bot_username} с аргументами.
 */
@Component
public class CommandDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDispatcher.class);

    private final Map<String, BotCommand> byName;
    private final MessageSender messageSender;

    public CommandDispatcher(List<BotCommand> commands, MessageSender messageSender) {
        this.byName = commands.stream().collect(Collectors.toMap(
                c -> c.name().toLowerCase(Locale.ROOT),
                c -> c,
                (a, b) -> {
                    throw new IllegalStateException("Duplicate command name: " + a.name());
                }));
        this.messageSender = messageSender;
        LOGGER.info("📋 Registered {} bot commands: {}", byName.size(), byName.keySet());
    }

    /**
     * Если сообщение содержит команду — обрабатывает её и возвращает {@code true}.
     * Если команды нет или обработчик не зарегистрирован — возвращает {@code false}
     * (бот сам решает, что делать с сообщением).
     */
    public boolean tryDispatch(CommandContext ctx) {
        String text = ctx.getMessage() != null ? ctx.getMessage().getText() : null;
        if (text == null || text.isBlank() || !text.startsWith("/")) {
            return false;
        }

        ParsedCommand parsed = parse(text, ctx.getBotUsername());
        if (parsed == null) {
            return false;
        }
        if (parsed.targetBot != null && !parsed.targetBot.equalsIgnoreCase(ctx.getBotUsername())) {
            return false;
        }

        BotCommand handler = byName.get(parsed.name.toLowerCase(Locale.ROOT));
        if (handler == null) {
            return false;
        }
        if (!handler.appliesIn(ctx.getChatScope())) {
            LOGGER.info("⏭ Command /{} skipped — not applicable in {}", parsed.name, ctx.getChatScope());
            return true;
        }

        CommandContext enriched = new CommandContext(
                ctx.getUpdate(), ctx.getMessage(), ctx.getBotEntity(), ctx.getSender(),
                ctx.getBotUsername(), ctx.getUser(), parsed.name, parsed.args, ctx.getChatScope());

        try {
            List<OutgoingMessage> responses = handler.handle(enriched);
            if (responses != null && !responses.isEmpty()) {
                messageSender.sendAll(ctx.getSender(), responses);
            }
        } catch (Exception e) {
            LOGGER.error("❌ Command /{} failed: {}", parsed.name, e.getMessage(), e);
        }
        return true;
    }

    private static ParsedCommand parse(String text, String botUsername) {
        String trimmed = text.trim();
        int sp = indexOfWhitespace(trimmed);
        String head = sp < 0 ? trimmed : trimmed.substring(0, sp);
        String args = sp < 0 ? "" : trimmed.substring(sp + 1).trim();
        if (!head.startsWith("/")) {
            return null;
        }
        String body = head.substring(1);
        if (body.isEmpty()) {
            return null;
        }
        int at = body.indexOf('@');
        String name = at < 0 ? body : body.substring(0, at);
        String target = at < 0 ? null : body.substring(at + 1);
        if (name.isEmpty()) {
            return null;
        }
        return new ParsedCommand(name, target, args);
    }

    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private record ParsedCommand(String name, String targetBot, String args) {}
}
