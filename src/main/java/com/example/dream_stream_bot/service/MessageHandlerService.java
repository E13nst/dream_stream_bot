package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.DreamCommand;
import com.example.dream_stream_bot.model.DreamState;
import com.example.dream_stream_bot.model.InlineKeyboardMarkupBuilder;
import com.example.dream_stream_bot.model.KeyboardFactory;
import jakarta.annotation.PostConstruct;
import net.gcardone.junidecode.Junidecode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerService.class);

    @Autowired
    private DreamService dreamService;
    @Autowired
    private BotConfig botConfig;
    @Autowired
    private AIService aiService;

    @PostConstruct
    public void init() {
    }

    // Обработчик ответов на сообщения бота в чате
    public List<SendMessage> handleReplyToBotMessage(Message message) {

        List<SendMessage> sendMessages = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(message.getChatId());

        String response = aiService.completion(message.getChat().getId(), message.getText(), chatUserName(message.getFrom()));
        sendMessages.add(msgFactory.createReplyToMessage(response, message.getMessageId()));
        return sendMessages;
    }

    public List<SendMessage> handlePersonalMessage(Message message) {

        List<SendMessage> sendMessages = new ArrayList<>();
        User user = message.getFrom();

        TelegramMessageFactory msgFactory = new TelegramMessageFactory(user.getId());

        if (dreamService.getUserDream(user.getId()) == null) {
            String response = aiService.completion(user.getId(), message.getText(), chatUserName(user));
            sendMessages.add(msgFactory.createMarkdownMessage(response));
            return sendMessages;
        }

        switch (dreamService.getUserDream(user.getId()).getCurrentState()) {

            case HISTORY -> {
                dreamService.addDreamText(user.getId(), message.getText());

                InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand("Поиск ассоциаций", DreamCommand.FIND_ASSOCIATIONS);
                sendMessages.add(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("+++++++++++++++++++++++++++++++++++++")
                        .parseMode("Markdown")
                        .replyMarkup(keyboard)
                        .build());
            }

            case ASSOCIATION -> {
                dreamService.setCurrentDreamAssociation(user.getId(), message.getText());

                String element = dreamService.getFirstUnassociatedDreamElement(user.getId());

                if (element != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", element)));
                } else {
                    String response = dreamService.getUserDream(user.getId()).associationsCollectForResult();
                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand("Разбор персонажей", DreamCommand.FIND_ACTORS);
                    sendMessages.add(msgFactory.createMarkdownMessage(response, keyboard));
                }
            }

            case PERSONALITY -> {
                var actor = dreamService.getUserDream(user.getId()).getNextActor();

                if (actor != null) {
                    actor.setCharacteristic(message.getText());
                }

                var nextActor = dreamService.getUserDream(user.getId()).getNextActor();

                if (nextActor != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", nextActor.getPerson())));

                } else {
                    String response = dreamService.getUserDream(user.getId()).personsCollectForResult();
                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand("Разбор черт личности персонажей", DreamCommand.SET_CONTEXT);
                    sendMessages.add(msgFactory.createMarkdownMessage(response, keyboard));
                }

            }

            case CONTEXT -> {
                dreamService.getUserDream(user.getId()).updateCurrentActor(message.getText());

                var nextActor = dreamService.getUserDream(user.getId()).getNextActor();

                if (nextActor != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", nextActor.getCharacteristic())));

                } else {
                    String response = dreamService.getUserDream(user.getId()).contextCollectForResult();
                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand("Продолжить", DreamCommand.SET_SENSE);
                    sendMessages.add(msgFactory.createMarkdownMessage(response, keyboard));
                }

            }

            case SENSE -> {
                dreamService.getUserDream(user.getId()).updateCurrentActor(message.getText());

                var nextActor = dreamService.getUserDream(user.getId()).getNextActor();

                if (nextActor != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", nextActor.getCharacteristic())));

                } else {
                    String response = dreamService.getUserDream(user.getId()).senseCollectForResult();
                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand("Интерпретация", DreamCommand.INTERPRETATION);
                    sendMessages.add(msgFactory.createMarkdownMessage(response, keyboard));
                }

            }

            default -> {
                String response = aiService.completion(user.getId(), message.getText(), chatUserName(user));
                sendMessages.add(msgFactory.createMarkdownMessage(response));
            }
        }
        return sendMessages;
    }

    public List<SendMessage> start(Message message) {

        List<SendMessage> responseMessageList = new ArrayList<>();

        var inlineKeyboardMarkup = new InlineKeyboardMarkupBuilder()
                .addKey("Начать \u2705", DreamCommand.CREATE.toString())
                .addKey("Отмена \u274C", DreamCommand.CANCEL.toString())
                .build();

        String msgStart = botConfig.getStartDescription();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(msgStart);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.enableMarkdown(true);

        responseMessageList.add(sendMessage);
        return responseMessageList;
    }

    public List<SendMessage> help(long chatId) {

        List<SendMessage> responseMessageList = new ArrayList<>();

        String response = "Hi, nice to meet you!";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(response);
        responseMessageList.add(sendMessage);
        return responseMessageList;
    }

    public List<SendMessage> handleCallbackQuery(CallbackQuery callbackQuery) {

        long chatId = callbackQuery.getMessage().getChatId();
        var user = callbackQuery.getFrom();

        List<SendMessage> responseMessageList = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(user.getId());

        switch (DreamCommand.fromString(callbackQuery.getData())) {
            case CREATE -> dreamService.create(user.getId());
            case FIND_ASSOCIATIONS -> {
                dreamService.changeDreamState(user.getId(), DreamState.ASSOCIATION);
                dreamService.findDreamElements(user.getId());
            }
            case FIND_ACTORS -> {
                dreamService.changeDreamState(user.getId(), DreamState.PERSONALITY);
                dreamService.findDreamActors(user.getId());
            }
            case SET_CONTEXT -> dreamService.changeDreamState(user.getId(), DreamState.CONTEXT);
            case SET_SENSE -> dreamService.changeDreamState(user.getId(), DreamState.SENSE);
            case INTERPRETATION -> {
                dreamService.changeDreamState(user.getId(), DreamState.ASSOCIATION);
                String result = dreamService.interpretUserDream(user.getId(), chatUserName(user));
                responseMessageList.add(msgFactory.createMarkdownMessage(result));
                dreamService.removeUserDream(user.getId());
            }
            case CANCEL -> dreamService.removeUserDream(user.getId());

        }

        responseMessageList.add(msgFactory.createMarkdownMessage(dreamService.stepDescription(user.getId())));
        return responseMessageList;
    }

    public static String chatUserName(User user) {
        return user.getLastName() == null ? user.getFirstName() : user.getFirstName() + " " + user.getLastName();
    }

    public static String transliterateUserName(User user) {
        return Junidecode.unidecode(user.getFirstName().replace(" ", "_"))
                .replaceAll("[^a-zA-Z0-9_-]", "");
    }
}
