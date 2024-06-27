package com.example.dream_stream_bot.dreamstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

import static com.theokanning.openai.service.OpenAiService.*;

public class OpenAiApiWithProxy {

    Duration timeout = Duration.ofSeconds(5);
    String token;
    String host = "localhost";
    int port = 1337;

    public OpenAiApiWithProxy(String token) {
        this.token = token;
    }

    public OpenAiApiWithProxy(String token, String host, int port) {
        this.token = token;
        this.host = host;
        this.port = port;
    }

    public OpenAiApi GetApi() {

        ObjectMapper mapper = defaultObjectMapper();
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
        OkHttpClient client = defaultClient(token, timeout)
                .newBuilder()
                .proxy(proxy)
                .build();

        Retrofit retrofit = defaultRetrofit(client, mapper);
        return retrofit.create(OpenAiApi.class);
    }
}
