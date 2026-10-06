package com.salohi.hrms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
