package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
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
        LOGGER.info("🚀 MessageHandlerService initialized");
    }

    // Обработчик ответов на сообщения бота в чате
    public List<SendMessage> handleReplyToBotMessage(Message message) {
        User user = message.getFrom();
        LOGGER.info("💬 Handling reply to bot message | User: {} (@{}) | Text: '{}'", 
            user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50));

        List<SendMessage> sendMessages = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(message.getChatId());

        String response = aiService.completion(message.getChat().getId(), message.getText(), chatUserName(message.getFrom()));
        sendMessages.add(msgFactory.createReplyToMessage(response, message.getMessageId()));
        
        LOGGER.info("💬 Reply message prepared | User: {} (@{}) | Response length: {} chars", 
            user.getFirstName(), user.getUserName(), response.length());
        
        return sendMessages;
    }

    public List<SendMessage> handlePersonalMessage(Message message) {
        User user = message.getFrom();
        LOGGER.info("💭 Handling personal message | User: {} (@{}) | Text: '{}'", 
            user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50));

        List<SendMessage> sendMessages = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(user.getId());

        if (dreamService.getUserDream(user.getId()) == null) {
            LOGGER.info("🤖 No active dream session | User: {} (@{}) | Sending AI response", 
                user.getFirstName(), user.getUserName());
            
            String response = aiService.completion(user.getId(), message.getText(), chatUserName(user));
            sendMessages.add(msgFactory.createMarkdownMessage(response));
            return sendMessages;
        }

        DreamState currentState = dreamService.getUserDream(user.getId()).getCurrentState();
        LOGGER.info("🌙 Processing dream state | User: {} (@{}) | State: {}", 
            user.getFirstName(), user.getUserName(), currentState);

        switch (currentState) {

            case HISTORY -> {
                LOGGER.info("📝 Adding dream text | User: {} (@{}) | Length: {} chars", 
                    user.getFirstName(), user.getUserName(), message.getText().length());
                
                dreamService.addDreamText(user.getId(), message.getText());

                DreamState nexState = DreamState.ASSOCIATION;

                InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(nexState, "Поиск ассоциаций");
                sendMessages.add(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("+++++++++++++++++++++++++++++++++++++")
                        .parseMode("Markdown")
                        .replyMarkup(keyboard)
                        .build());
                
                LOGGER.info("🔍 Dream text added, moving to association phase | User: {} (@{})", 
                    user.getFirstName(), user.getUserName());
            }

            case ASSOCIATION -> {
                LOGGER.info("💭 Setting dream association | User: {} (@{}) | Association: '{}'", 
                    user.getFirstName(), user.getUserName(), truncateText(message.getText(), 30));
                
                dreamService.setCurrentDreamAssociation(user.getId(), message.getText());

                String element = dreamService.getFirstUnassociatedDreamElement(user.getId());

                if (element != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", element)));
                    LOGGER.info("🔍 Next element requested | User: {} (@{}) | Element: '{}'", 
                        user.getFirstName(), user.getUserName(), element);
                } else {
                    DreamState nexState = DreamState.PERSONALITY;
                    String nexStateDescription = dreamService.getDreamStateDescription(nexState);

                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(nexState, "Разбор персонажей");
                    sendMessages.add(msgFactory.createMarkdownMessage(nexStateDescription, keyboard));
                    
                    LOGGER.info("👥 All associations collected, moving to personality phase | User: {} (@{})", 
                        user.getFirstName(), user.getUserName());
                }
            }

            case PERSONALITY -> {
                var actor = dreamService.getUserDream(user.getId()).getNextActor();

                if (actor != null) {
                    LOGGER.info("👤 Setting actor characteristic | User: {} (@{}) | Actor: '{}' | Characteristic: '{}'", 
                        user.getFirstName(), user.getUserName(), actor.getPerson(), truncateText(message.getText(), 30));
                    
                    actor.setCharacteristic(message.getText());
                }

                var nextActor = dreamService.getUserDream(user.getId()).getNextActor();

                if (nextActor != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", nextActor.getPerson())));
                    LOGGER.info("👤 Next actor requested | User: {} (@{}) | Actor: '{}'", 
                        user.getFirstName(), user.getUserName(), nextActor.getPerson());
                } else {
                    DreamState nexState = DreamState.CONTEXT;
                    String nexStateDescription = dreamService.getDreamStateDescription(nexState);

                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(nexState, "Разбор черт личности персонажей");
                    sendMessages.add(msgFactory.createMarkdownMessage(nexStateDescription, keyboard));
                    
                    LOGGER.info("🎭 All actors processed, moving to context phase | User: {} (@{})", 
                        user.getFirstName(), user.getUserName());
                }
            }

            case CONTEXT -> {
                LOGGER.info("🌍 Updating actor context | User: {} (@{}) | Context: '{}'", 
                    user.getFirstName(), user.getUserName(), truncateText(message.getText(), 30));
                
                dreamService.getUserDream(user.getId()).updateCurrentActor(message.getText());

                var nextActor = dreamService.getUserDream(user.getId()).getNextActor();

                if (nextActor != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", nextActor.getCharacteristic())));
                    LOGGER.info("🌍 Next context requested | User: {} (@{}) | Characteristic: '{}'", 
                        user.getFirstName(), user.getUserName(), nextActor.getCharacteristic());
                } else {
                    DreamState nexState = DreamState.SENSE;
                    String nexStateDescription = dreamService.getDreamStateDescription(nexState);

                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(nexState, "Продолжить");
                    sendMessages.add(msgFactory.createMarkdownMessage(nexStateDescription, keyboard));
                    
                    LOGGER.info("💡 All contexts processed, moving to sense phase | User: {} (@{})", 
                        user.getFirstName(), user.getUserName());
                }
            }

            case SENSE -> {
                LOGGER.info("💭 Updating actor sense | User: {} (@{}) | Sense: '{}'", 
                    user.getFirstName(), user.getUserName(), truncateText(message.getText(), 30));
                
                dreamService.getUserDream(user.getId()).updateCurrentActor(message.getText());

                var nextActor = dreamService.getUserDream(user.getId()).getNextActor();

                if (nextActor != null) {
                    sendMessages.add(msgFactory.createMarkdownMessage(String.format("- *%s*:", nextActor.getCharacteristic())));
                    LOGGER.info("💭 Next sense requested | User: {} (@{}) | Characteristic: '{}'", 
                        user.getFirstName(), user.getUserName(), nextActor.getCharacteristic());
                } else {
                    DreamState nexState = DreamState.INTERPRETATION;
                    String nexStateDescription = dreamService.getDreamStateDescription(nexState);

                    InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(nexState, "Интерпретация");
                    sendMessages.add(msgFactory.createMarkdownMessage(nexStateDescription, keyboard));
                    
                    LOGGER.info("🔮 All senses processed, ready for interpretation | User: {} (@{})", 
                        user.getFirstName(), user.getUserName());
                }
            }

            default -> {
                LOGGER.info("🤖 Default AI response | User: {} (@{}) | State: {}", 
                    user.getFirstName(), user.getUserName(), currentState);
                
                String response = aiService.completion(user.getId(), message.getText(), chatUserName(user));
                sendMessages.add(msgFactory.createMarkdownMessage(response));
            }
        }
        return sendMessages;
    }

    public List<SendMessage> start(Message message) {
        User user = message.getFrom();
        LOGGER.info("🚀 Start command received | User: {} (@{})", user.getFirstName(), user.getUserName());

        TelegramMessageFactory msgFactory = new TelegramMessageFactory(user.getId());

        DreamState nexState = DreamState.HISTORY;
        String nexStateDescription = dreamService.getDreamStateDescription(nexState);

        List<SendMessage> sendMessages = new ArrayList<>();
        InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(nexState, "Начать");
        sendMessages.add(msgFactory.createMarkdownMessage(nexStateDescription, keyboard));
        return sendMessages;
    }

    public List<SendMessage> help(Message message) {
        User user = message.getFrom();
        LOGGER.info("❓ Help command received | User: {} (@{})", user.getFirstName(), user.getUserName());

        TelegramMessageFactory msgFactory = new TelegramMessageFactory(user.getId());

        DreamState nexState = DreamState.HISTORY;
        String nexStateDescription = dreamService.getDreamStateDescription(nexState);

        List<SendMessage> sendMessages = new ArrayList<>();
        InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(nexState, "Начать");
        sendMessages.add(msgFactory.createMarkdownMessage(nexStateDescription, keyboard));
        return sendMessages;
    }

    public List<SendMessage> handleCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        var user = callbackQuery.getFrom();
        String data = callbackQuery.getData();
        
        LOGGER.info("🔘 Callback query handled | User: {} (@{}) | Data: '{}'", 
            user.getFirstName(), user.getUserName(), data);

        List<SendMessage> responseMessageList = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(user.getId());

        switch (DreamState.fromString(data)) {
            case HISTORY -> {
                LOGGER.info("📝 Starting dream analysis | User: {} (@{})", user.getFirstName(), user.getUserName());
                
                String response = dreamService.create(user.getId());
                InlineKeyboardMarkup keyboard = KeyboardFactory.simpleWithCommand(DreamState.HISTORY, "Начать");
                responseMessageList.add(msgFactory.createMarkdownMessage(response, keyboard));
            }
            case ASSOCIATION -> {
                LOGGER.info("🔍 Starting element extraction | User: {} (@{})", user.getFirstName(), user.getUserName());
                
                String response = dreamService.findDreamElements(user.getId());
                responseMessageList.add(msgFactory.createMarkdownMessage(response));
            }
            case PERSONALITY -> {
                LOGGER.info("👥 Starting actor extraction | User: {} (@{})", user.getFirstName(), user.getUserName());
                
                String response = dreamService.findDreamActors(user.getId());
                responseMessageList.add(msgFactory.createMarkdownMessage(response));
            }
            case CONTEXT -> {
                LOGGER.info("🌍 Starting context analysis | User: {} (@{})", user.getFirstName(), user.getUserName());
                
                String response = dreamService.stepDescription(user.getId());
                responseMessageList.add(msgFactory.createMarkdownMessage(response));
            }
            case SENSE -> {
                LOGGER.info("💭 Starting sense analysis | User: {} (@{})", user.getFirstName(), user.getUserName());
                
                String response = dreamService.stepDescription(user.getId());
                responseMessageList.add(msgFactory.createMarkdownMessage(response));
            }
            case INTERPRETATION -> {
                LOGGER.info("🔮 Starting dream interpretation | User: {} (@{})", user.getFirstName(), user.getUserName());
                
                String response = dreamService.interpretUserDream(user.getId(), chatUserName(user));
                responseMessageList.add(msgFactory.createMarkdownMessage(response));
            }
            default -> {
                LOGGER.warn("⚠️ Unknown callback data | User: {} (@{}) | Data: '{}'", 
                    user.getFirstName(), user.getUserName(), data);
                
                responseMessageList.add(msgFactory.createMarkdownMessage("Неизвестная команда"));
            }
        }
        
        LOGGER.info("🔘 Callback response prepared | User: {} (@{}) | Messages: {}", 
            user.getFirstName(), user.getUserName(), responseMessageList.size());
        
        return responseMessageList;
    }

    public static String chatUserName(User user) {
        if (user.getUserName() != null) {
            return user.getUserName();
        } else if (user.getFirstName() != null) {
            return transliterateUserName(user);
        } else {
            return "User" + user.getId();
        }
    }

    public static String transliterateUserName(User user) {
        return Junidecode.unidecode(user.getFirstName());
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "null";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
