package com.example.tiketbioskop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    // Schedules store local cinema time, so "now" is read in the cinema's zone, not the server's.
    // A bean, so tests can move time forward instead of waiting for it
    @Bean
    public Clock clock(@Value("${bioskop.timezone}") ZoneId zone) {
        return Clock.system(zone);
    }

    // Runs @Scheduled jobs (clearing expired reservations). Off in tests: they call the job directly,
    // so a background run can't change data in the middle of a test
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "bioskop.scheduling", havingValue = "true", matchIfMissing = true)
    static class Scheduling {
    }
}
