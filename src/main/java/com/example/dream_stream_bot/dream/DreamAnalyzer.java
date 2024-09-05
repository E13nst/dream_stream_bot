package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.ChatSession;
import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class DreamAnalyzer {

    public enum DreamStatus {
        NEW, HISTORY, ASSOCIATION, PERSONALITY, CONTEXT, SENSE, INTERPRETATION, COMPLETE
    }

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

    private static final InlineKeyboardMarkup defaultKeyboard = new InlineCommandKeyboard()
            .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
            .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
            .build();

    @Builder
    public DreamAnalyzer(ChatSession openaiChat, String userName, long telegramChatId, Dream dream) {
        this.openaiChat = openaiChat;
        this.telegramChatId = telegramChatId;
        this.userName = userName;
        this.state = new StateStart();
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

    public List<SendMessage> init() {
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

    static class StateStart implements AnalyzerState {

        @Override
        public DreamStatus getState() {
            return DreamStatus.NEW;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {
            analyzer.setState(new StateHistory());
            return null;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer dream, String text) {
            return null;
        }

    }

    static class StateHistory implements AnalyzerState {

        private static final String MSG_DESC_1 = "На первом шаге я помогу записать твой сон в виде истории.";
        private static final String MSG_DESC_2 = "Постарайся описать его как можно подробнее, включая все детали, которые помнишь. " +
                "На данном этапе важно собрать всю информацию, даже если она кажется разрозненной: " +
                "важно записать как можно больше деталей сна, включая образы, эмоции, диалоги и события.";
        private static final String MSG_DESC_3 = "Можешь отправлять описание сна в нескольких сообщениях. Когда будешь готов, " +
                "нажми кнопку **Продолжить**, чтобы перейти к следующему этапу анализа.";
        private static final String MSG_NEXT = "\u2705 Для перехода к следующему шагу нажми \"Продолжить\"";

        @Override
        public DreamStatus getState() {
            return DreamStatus.HISTORY;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {

            List<SendMessage> sendMessages = new ArrayList<>();
            Dream dream = analyzer.getDream();

            if (dream.getHistoryStr().isEmpty())
                return sendMessages;

            String rawText = AiTextProcessor.findElements(
                    analyzer.getOpenaiChat(),
                    analyzer.getUserName(),
                    analyzer.getDream().getHistoryStr());

            var elementStringList = AiTextProcessor.splitItems(rawText);

            if (elementStringList.isEmpty()) {
                dream.cleanHistory();
                sendMessages.add(analyzer.newTelegramMessage(rawText));
                return sendMessages;
            }

            dream.addAllElements(elementStringList.stream().map(DreamElement::new).collect(Collectors.toList()));

            var actorsList = AiTextProcessor.extractActors(
                    analyzer.getOpenaiChat(),
                    analyzer.getUserName(),
                    analyzer.getDream().getHistoryStr());

            dream.addAllActors(actorsList.stream().map(DreamActor::new).collect(Collectors.toList()));

            if (!dream.getAssociations().isEmpty()) {
                analyzer.setState(new StateAssociation(analyzer.getDream()));
            }

            return sendMessages;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
            analyzer.setState(new StateStart());
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer analyzer, String text) {

            List<SendMessage> messages = new ArrayList<>();

            if (analyzer.getDream().getHistoryStr().isEmpty()) {
                messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_3));
            } else {
                SendMessage message = analyzer.newTelegramMessage(MSG_NEXT, DreamAnalyzer.defaultKeyboard);
                messages.add(message);
            }

            analyzer.getDream().addHistory(text);
            return messages;
        }
    }

    static class StateAssociation implements AnalyzerState {

        private static final Logger LOGGER = LoggerFactory.getLogger(StateAssociation.class);

        private static final String MSG_DESC_1 = "Я выберу из твоего рассказа образы и предметы для подбора ассоциации.";
        private static final String MSG_DESC_2 = "Напиши, что каждый конкретный образ значит для тебя в контексте сна. " +
                "Если каждый образ вызывает несколько ассоциаций или воспоминаний — например, конкретного человека, " +
                "слова, фразы или ситуации — запиши все эти мысли.";
        private static final String MSG_DESC_3 = "Не переживай о правильности ассоциаций на этом этапе. Важно собрать разные варианты, " +
                "даже если они кажутся несвязанными. Наша цель — найти прямые ассоциации, которые возникают в связи с каждым образом.";
        private static final String MSG_DESC_4 = "Какую ассоциацию вызывает у тебя этот образ: ";
        private static final String MSG_END = "У нас получились такие ассоциации:";

        private final Iterator<DreamElement> iterator;
        private DreamElement currentElement;

        StateAssociation(Dream dream) {
            this.iterator = dream.getAssociations().iterator();
        }

        @Override
        public DreamStatus getState() {
            return DreamStatus.ASSOCIATION;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {
            if (!analyzer.getDream().getActors().isEmpty())
                analyzer.setState(new StatePersonality());
            else
                analyzer.setState(new StateInterpretation());
            return null;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
            analyzer.setState(new StateHistory());
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer analyzer, String msg) {

            List<SendMessage> messages = new ArrayList<>();

            if (currentElement == null) {
                messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_3));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_4));
            } else if (msg != null && !msg.isBlank()) {
                currentElement.setAssociation(msg);
                LOGGER.info("Association set for element {}", currentElement);
            }

            if (iterator.hasNext()) {
                currentElement = iterator.next();
                messages.add(analyzer.newTelegramMessage(currentElement.getName()));
            }
            else {
                messages.add(analyzer.newTelegramMessage(MSG_END));
                messages.add(analyzer.newTelegramMessage(analyzer.getDream().associationsCollectForResult(), defaultKeyboard));
            }
            return messages;
        }

    }

    static class StatePersonality implements AnalyzerState {

        private static final String MSG_DESC_1 = "Теперь мы будем работать с персонажами сновидения.";
        private static final String MSG_PERSON = "Какая черта твоей личности ассоциируется с этим персонажем?";
        private static final String MSG_END = "У нас получились такие персонажи:";

        private DreamActor currentActor;

        @Override
        public DreamStatus getState() {
            return DreamStatus.PERSONALITY;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {
            analyzer.getDream().initActorsIterator();
            analyzer.setState(new StateContext());
            return null;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
            analyzer.getDream().initActorsIterator();
            analyzer.setState(new StateAssociation(analyzer.getDream()));
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer analyzer, String answer) {

            List<SendMessage> messages = new ArrayList<>();

            if (currentActor == null) {
                messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
            } else {
                currentActor.setCharacteristic(answer);
            }

            if (analyzer.getDream().hasActor()) {
                currentActor = analyzer.getDream().nextActor();
                messages.add(analyzer.newTelegramMessage(MSG_PERSON));
                messages.add(analyzer.newTelegramMessage(currentActor.getName()));
            } else {
                messages.add(analyzer.newTelegramMessage(MSG_END));
                messages.add(analyzer.newTelegramMessage(analyzer.getDream().personsCollectForResult(), defaultKeyboard));
            }

            return messages;
        }

    }

    static class StateContext implements AnalyzerState {

        private static final String MSG_CONTEXT_DESC = "Теперь мы вместе проанализируем твои черты личности";
        private static final String MSG_CONTEXT = "Где в твоей жизни проявляются эти черты: %s?";
        private static final String MSG_CONTEXT_END = "Твои черты личности проявляются:";

        private DreamActor currentActor;

        @Override
        public DreamStatus getState() {
            return DreamStatus.CONTEXT;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {
            analyzer.getDream().initActorsIterator();
            analyzer.setState(new StateSense());
            return null;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
            analyzer.getDream().initActorsIterator();
            analyzer.setState(new StatePersonality());
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer analyzer, String answer) {

            List<SendMessage> messages = new ArrayList<>();

            if (currentActor == null) {
                messages.add(analyzer.newTelegramMessage(MSG_CONTEXT_DESC));
            } else {
                currentActor.setContext(answer);
            }

            if (analyzer.getDream().hasActor()) {
                currentActor = analyzer.getDream().nextActor();
                messages.add(analyzer.newTelegramMessage(String.format(MSG_CONTEXT, currentActor.getCharacteristic())));
            } else {
                messages.add(analyzer.newTelegramMessage(MSG_CONTEXT_END));
                messages.add(analyzer.newTelegramMessage(analyzer.getDream().contextCollectForResult(), defaultKeyboard));
            }

            return messages;
        }

    }

    static class StateSense implements AnalyzerState {

        private static final String MSG_SENSE_DESC = "Теперь мы вместе проанализируем твои черты личности";
        private static final String MSG_SENSE = "Что эти черты твоей личности для тебя значат: %s?";
        private static final String MSG_SENSE_END = "Мы определили такие значения твоих черт личности:";

        private DreamActor currentActor;

        @Override
        public DreamStatus getState() {
            return DreamStatus.SENSE;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {
            analyzer.getDream().initActorsIterator();
            analyzer.setState(new StateInterpretation());
            return null;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
            analyzer.getDream().initActorsIterator();
            analyzer.setState(new StateContext());
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer analyzer, String answer) {

            List<SendMessage> messages = new ArrayList<>();

            if (currentActor == null) {
                messages.add(analyzer.newTelegramMessage(MSG_SENSE_DESC));
            } else {
                currentActor.setSense(answer);
            }

            if (analyzer.getDream().hasActor()) {
                currentActor = analyzer.getDream().nextActor();
                messages.add(analyzer.newTelegramMessage(String.format(MSG_SENSE, currentActor.getCharacteristic())));
            } else {
                messages.add(analyzer.newTelegramMessage(MSG_SENSE_END));
                messages.add(analyzer.newTelegramMessage(analyzer.getDream().senseCollectForResult(), defaultKeyboard));
            }

            return messages;
        }

    }

    static class StateInterpretation implements AnalyzerState {

        @Override
        public DreamStatus getState() {
            return DreamStatus.INTERPRETATION;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {
            return null;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
            analyzer.setState(new StatePersonality());
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer analyzer, String text) {

            String response = AiTextProcessor.interpretDream(
                    analyzer.getOpenaiChat(),
                    analyzer.getUserName(),
                    analyzer.getDream()
            );

            analyzer.setState(new StateComplete());

            List<SendMessage> messages = new ArrayList<>();
            var keyboard = new InlineCommandKeyboard()
                    .addKey("Завершить", InlineButtons.CANCEL.toString())
                    .build();
            messages.add(analyzer.newTelegramMessage(response, keyboard));
            return messages;
        }
    }

    static class StateComplete implements AnalyzerState {

        @Override
        public DreamStatus getState() {
            return DreamStatus.COMPLETE;
        }

        @Override
        public List<SendMessage> next(DreamAnalyzer analyzer) {
            return null;
        }

        @Override
        public void prev(DreamAnalyzer analyzer) {
            analyzer.setState(new StateInterpretation());
        }

        @Override
        public List<SendMessage> processMessage(DreamAnalyzer analyzer, String text) {
            return new ArrayList<>();
        }
    }

    interface AnalyzerState {
        DreamStatus getState();
        List<SendMessage> next(DreamAnalyzer dream);
        void prev(DreamAnalyzer dream);
        List<SendMessage> processMessage(DreamAnalyzer dream, String text);
    }
}
