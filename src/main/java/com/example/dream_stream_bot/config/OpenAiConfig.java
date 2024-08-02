package com.example.dream_stream_bot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")
public class OpenAiConfig {
    @Value("${openai.token}")
    String token;
    String prompt;
}
