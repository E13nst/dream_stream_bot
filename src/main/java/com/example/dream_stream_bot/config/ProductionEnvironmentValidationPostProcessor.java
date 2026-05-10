package com.example.dream_stream_bot.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Runs very early in the Spring Boot lifecycle (before bean creation) so missing prod
 * configuration fails with one aggregated message instead of deep stack traces.
 */
public class ProductionEnvironmentValidationPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ProductionEnvironmentValidator.validateIfProd(environment);
    }
}
