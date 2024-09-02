package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class AiTextProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiTextProcessor.class);

    private static final String ELEMENTS_PROMPT = """
            Выбери из текста сновидения все неодушевленные образы, предметы, символы и ситуации вместе с их свойствами и характеристиками, 
            которые можно использовать для анализа этого сновидения по Юнгу. Не давай своих интерпретаций. 
            Результат должен быть без лишних комментариев в виде списка в формате json, который будет содержать 
            эти предметы по такому образцу:\s
            ["красный спортивный автомобиль","чистая холодная вода"]
            Список не должен включать персонажей и действующих лиц.Текст для анализа:
            """;

    private static final String ACTORS_PROMPT = """
            Выбери из текста сновидения всех персонажей и действующих лиц вместе с их характеристиками.
            Список не должен включать меня самого. Не давай своих интерпретаций.
            Результат должен быть в виде списка без лишних комментариев
            в формате json, который будет содержать этих персонажей,
            например: ["красивая девушка","молчаливый незнакомец"]
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

    public static String findElements(ChatSession openaiChat, String userName, String text) {
        String query = String.format("%s %s", ELEMENTS_PROMPT, text);
        return openaiChat.send(query, userName);
    }

    public static List<String> splitItems(String rawText) {

        int startIndex = rawText.indexOf('[') + 1;
        int endIndex = rawText.lastIndexOf(']');

        if (startIndex > 0 && endIndex > startIndex) {
            String extracted = rawText.substring(startIndex, endIndex);
            List<String> list = Arrays.asList(extracted.split(","));
            list = list.stream().map(e -> e.replace("\"", "")).map(String::trim).toList();
            return list;
        } else {
            LOGGER.error("Elements not found");
            return List.of();
        }
    }

    public static List<String> extractAndSplitItems(ChatSession openaiChat, String userName, String text, String prompt) {
        String query = String.format("%s %s", prompt, text);
        String response = openaiChat.send(query, userName);

        return splitItems(response);
    }

    public static List<String> extractElements(ChatSession openaiChat, String userName, String text) {
        return extractAndSplitItems(openaiChat, userName, text, ELEMENTS_PROMPT);
    }

    public static List<String> extractActors(ChatSession openaiChat, String userName, String text) {
        return extractAndSplitItems(openaiChat, userName, text, ACTORS_PROMPT);
    }

    public static String interpretDream(ChatSession openaiChat, String userName, Dream dream) {

        String query = String.format(INTERPRET_PROMPT,
                dream.associationsCollectForResult(),
                dream.personsCollectForResult(),
                dream.contextCollectForResult(),
                dream.senseCollectForResult(),
                dream.getHistoryStr()
        );

        return openaiChat.send(query, userName);
    }
}
