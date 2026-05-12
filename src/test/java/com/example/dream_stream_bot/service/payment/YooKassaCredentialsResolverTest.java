package com.example.dream_stream_bot.service.payment;

import com.example.dream_stream_bot.config.properties.YooKassaProperties;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class YooKassaCredentialsResolverTest {

    @Test
    void prefersBotCredentialsWhenBothPresent() {
        YooKassaProperties props = new YooKassaProperties();
        props.setShopId("global_shop");
        props.setSecretKey("global_secret");
        YooKassaCredentialsResolver resolver = new YooKassaCredentialsResolver(props);

        BotEntity bot = new BotEntity();
        bot.setYookassaShopId(" bot_shop ");
        bot.setYookassaSecretKey(" bot_secret ");

        Optional<YooKassaCredentials> c = resolver.resolve(bot);
        assertThat(c).isPresent();
        assertThat(c.get().shopId()).isEqualTo("bot_shop");
        assertThat(c.get().secretKey()).isEqualTo("bot_secret");
    }

    @Test
    void fallsBackToGlobalProperties() {
        YooKassaProperties props = new YooKassaProperties();
        props.setShopId("shop");
        props.setSecretKey("secret");
        YooKassaCredentialsResolver resolver = new YooKassaCredentialsResolver(props);

        BotEntity bot = new BotEntity();
        Optional<YooKassaCredentials> c = resolver.resolve(bot);
        assertThat(c).isPresent();
        assertThat(c.get().shopId()).isEqualTo("shop");
    }

    @Test
    void emptyWhenBotIncompleteAndGlobalMissing() {
        YooKassaProperties props = new YooKassaProperties();
        YooKassaCredentialsResolver resolver = new YooKassaCredentialsResolver(props);

        BotEntity bot = new BotEntity();
        bot.setYookassaShopId("only_shop");

        assertThat(resolver.resolve(bot)).isEmpty();
    }
}
