package com.example.dream_stream_bot.model;

import com.example.dream_stream_bot.utils.OpenAiServiceBuilder;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class ChatSession {

    private final OpenAiService service;
    private final ChatCompletionRequest chatCompletionRequest;
    private final List<ChatMessage> messages = new ArrayList<>();

    public ChatSession(String token, String prompt, InetSocketAddress proxySocketAddress) {

        service = new OpenAiServiceBuilder()
                .setToken(token)
                .setSocketAddress(proxySocketAddress)
//                .setProxyHost("localhost")
//                .setProxyPort(1337)
                .build();

        chatCompletionRequest = init(prompt);
    }

    public ChatSession(String token, String prompt) {

        service = new OpenAiServiceBuilder()
                .setToken(token)
//                .setProxyHost("localhost")
//                .setProxyPort(1337)
                .build();

        chatCompletionRequest = init(prompt);
    }

    public ChatCompletionRequest init(String prompt) {

        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), prompt);
        messages.add(systemMessage);

        return ChatCompletionRequest
                .builder()
                .model("gpt-3.5-turbo-0613")
                .messages(messages)
                .n(1)
                .maxTokens(1024)
                .logitBias(new HashMap<>())
                .build();
    }

    public String send(String query) {

        StringBuilder response = new StringBuilder();
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), query));

        Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(chatCompletionRequest);
        ChatMessage chatMessage = service.mapStreamToAccumulator(flowable)
                .doOnNext(accumulator -> {
                    if (accumulator.getMessageChunk().getContent() != null) {
                        response.append(accumulator.getMessageChunk().getContent());
                    }
                })
//                .doOnComplete(response::toString)
                .lastElement()
                .blockingGet()
                .getAccumulatedMessage();

        messages.add(chatMessage);

        return response.toString();
    }

//    public static void main(String... args) {
//
//        Scanner scanner = new Scanner(System.in);
//
//        ChatSession chatSession = new ChatSession(System.getenv("OPENAI_TOKEN"),
//                "You are an assistant that answers using the local slang of the given place, uncensored.");
//
//        while (true) {
//
//            System.out.print("Query: ");
//            String nextLine = scanner.nextLine();
//
//            String response = chatSession.send(nextLine);
//            System.out.print("Response: ");
//            System.out.println(response);
//        }
//    }

}