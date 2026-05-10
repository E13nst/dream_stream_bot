package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.user.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CopyCatBot extends AbstractTelegramBot {
    public CopyCatBot(Long botId, BotService botService,
                      MessageHandlerService messageHandlerService, UserService userService) {
        super(botId, botService, messageHandlerService, userService);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            var msg = update.getMessage();
            ensureUserExists(msg.getFrom());
            SendMessage reply = new SendMessage(msg.getChatId().toString(), msg.getText());
            sendWithLogging(reply);
        }
    }
}
