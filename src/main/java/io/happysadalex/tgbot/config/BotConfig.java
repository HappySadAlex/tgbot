package io.happysadalex.tgbot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class BotConfig {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.sourceUrl}")
    private String botUrl;

    @Value("${bot.policyUrl}")
    private String policyUrl;

}