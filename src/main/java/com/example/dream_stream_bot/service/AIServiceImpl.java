package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.model.Dream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    private static final String ELEMENTS_PROMPT = """
            Выбери из текста сновидения все неодушевленные образы, предметы, символы и ситуации вместе с их свойствами и характеристиками, 
            которые можно использовать для анализа этого сновидения по Юнгу. Список не должен включать персонажей и действующих лиц. Не давай своих интерпретаций, 
            результат должен быть без лишних комментариев в виде JSON array.
            
//            , который будет содержать 
//            эти предметы по такому образцу:\s
//            ["красный спортивный автомобиль", "чистая холодная вода"]
            Текст для анализа:
            """;

    private static final String ACTORS_PROMPT = """
            Выбери из текста сновидения всех персонажей и действующих лиц вместе с их характеристиками.
            Список не должен включать меня самого. Не давай своих интерпретаций.
            Результат должен быть в формате JSON array, который будет содержать этих персонажей,
            например: ["красивая девушка", "молчаливый незнакомец"]
            Текст для анализа:
            """;

    private static final String INTERPRET_PROMPT = """
            Интерпретируй мое сновидение по Юнгу, учитывая взаимодейстаие персонажей и объектов между собой.
            Операясь на мои личные ассоциации c ключевыми элементами в этом сновидении:
            %s
            Проведи архитипический анализ персонажей этого сновидения, которые могут представлять собой архетипы,
            такие как Тень, Анима/Анимус, Персона и другие, учитывая динамику сновидения.
            Учитывай, какую конкретно мою черту характера какой комплекс он отражает:
            %s
            Так же, где в реальной жизни присутствует такая же динамика с чем это ассоциируется:
            %s
            Что эта черта моей личности означает для меня:
            %s
            Сновидение для анализа:
            "%s"
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Autowired
    public AIServiceImpl(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @Override
    public String completion(long chatId, String message, String userName) {

        logger.info("[{}]: {}", userName, message);

        return chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .user(String.format("User %s says:\n%s", userName, message))
                .call()
                .content();
    }

    @Override
    public String completion(long chatId, String message) {

        logger.info("completion: {}", message);

        return chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                    .user(message)
                    .call()
                    .content();
    }

    @Override
    public String findElements(long chatId, String text) {
        String query = String.format("%s %s", ELEMENTS_PROMPT, text);
        return completion(chatId, query);
    }

    @Override
    public String findActors(long chatId, String text) {
        String query = String.format("%s %s", ACTORS_PROMPT, text);
        return completion(chatId, query);
    }

    @Override
    public List<String> extractAndSplitItems(long chatId, String text, String userName, String prompt) {
        String query = String.format("%s %s", prompt, text);
        return splitItems(completion(chatId, query, userName));
    }

    @Override
    public List<String> extractElements(long chatId, String text, String userName) {
        return extractAndSplitItems(chatId, text, userName, ELEMENTS_PROMPT);
    }

    @Override
    public List<String> extractActors(long chatId, String text, String userName) {
        return extractAndSplitItems(chatId, text, userName, ACTORS_PROMPT);
    }

    @Override
    public String interpretDream(long chatId, String userName, Dream dream) {

        String text = String.format(INTERPRET_PROMPT,
                dream.associationsCollectForResult(),
                dream.personsCollectForResult(),
                dream.contextCollectForResult(),
                dream.senseCollectForResult(),
                dream.getHistory()
        );

        return completion(chatId, text, userName);
    }

    @Override
    public String interpretDream(long chatId, Dream dream) {

        String text = String.format(INTERPRET_PROMPT,
                dream.associationsCollectForResult(),
                dream.personsCollectForResult(),
                dream.contextCollectForResult(),
                dream.senseCollectForResult(),
                dream.getHistory()
        );

        return completion(chatId, text);
    }

    //TODO перенести
    static List<String> splitItems(String rawText) {

        int startIndex = rawText.indexOf('[') + 1;
        int endIndex = rawText.lastIndexOf(']');

        if (startIndex > 0 && endIndex > startIndex) {
            String extracted = rawText.substring(startIndex, endIndex);
            List<String> list = Arrays.asList(extracted.split(","));
            list = list.stream().map(e -> e.replace("\"", "")).map(String::trim).toList();
            return list;
        } else {
            logger.error("Elements not found");
            return List.of();
        }
    }
}
