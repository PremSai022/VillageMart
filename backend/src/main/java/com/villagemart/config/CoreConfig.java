package com.villagemart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableAsync
@EnableScheduling
public class CoreConfig {

    @Bean
    public Clock systemClock(AppProperties properties) {
        String tz = properties.timezone() == null || properties.timezone().isBlank() ? "Asia/Kolkata" : properties.timezone();
        return Clock.system(ZoneId.of(tz));
    }
}
