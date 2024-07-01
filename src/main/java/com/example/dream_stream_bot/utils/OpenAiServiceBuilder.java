package com.example.dream_stream_bot.utils;

import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

import static com.theokanning.openai.service.OpenAiService.*;

public class OpenAiServiceBuilder {

    private String token;
    Duration timeout = Duration.ofSeconds(5);
    private String proxyHost;
    private int proxyPort = 1337;

    public OpenAiServiceBuilder setToken(String token) {
        this.token = token;
        return this;
    }

    public OpenAiServiceBuilder setTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public OpenAiServiceBuilder setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public OpenAiServiceBuilder setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public OpenAiService build() {

        if (proxyHost.isEmpty()) {
            return new OpenAiService(token);

        }
        else {
            ObjectMapper mapper = defaultObjectMapper();
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
            OkHttpClient client = defaultClient(token, timeout)
                    .newBuilder()
                    .proxy(proxy)
                    .build();

            Retrofit retrofit = defaultRetrofit(client, mapper);
            OpenAiApi api = retrofit.create(OpenAiApi.class);

            return new OpenAiService(api);
        }
    }
}

