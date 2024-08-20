package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AiTextProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiTextProcessor.class);

    private static final String ELEMENTS_PROMPT = """
            Выбери из текста сновидения все неодушевленные образы и предметы вместе с их свойствами и характеристиками, 
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
            Интерпретируй это сновидения по Юнгу, операясь на мои личные ассоциации:
            %s
            И персонажей моего сновидения, которые могут представлять мою персону, тень, аниму или анимуса:
            %s
            Учитывай взаимодейстаие этих персонажей и объектов между собой в контексте сновидения""";

    public static List<String> extractItemsAndSplit(ChatSession openaiChat, String userName, String text, String prompt) {
        String query = String.format("%s %s", prompt, text);
        String response = openaiChat.send(query, userName);

        int startIndex = response.indexOf('[') + 1;
        int endIndex = response.lastIndexOf(']');

        if (startIndex > 0 && endIndex > startIndex) {
            String extracted = response.substring(startIndex, endIndex);
            List<String> list = Arrays.asList(extracted.split(","));
            list = list.stream().map(e -> e.replace("\"", "")).map(String::trim).toList();
            return list;
        } else {
            LOGGER.error("Brackets not found or incorrect order.");
            return List.of();
        }
    }

    public static List<String> extractElements(ChatSession openaiChat, String userName, String text) {
        return extractItemsAndSplit(openaiChat, userName, text, ELEMENTS_PROMPT);
    }

    public static List<String> extractActors(ChatSession openaiChat, String userName, String text) {
        return extractItemsAndSplit(openaiChat, userName, text, ACTORS_PROMPT);
    }

    public static String interpretDream(ChatSession openaiChat, String userName, Dream dream) {

        String query = String.format(INTERPRET_PROMPT,
                dream.getAssociations().entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue())
                        .collect(Collectors.joining("\n")),
                dream.getPersons().entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue())
                        .collect(Collectors.joining("\n"))
        );

        return openaiChat.send(query, userName);
    }
}
