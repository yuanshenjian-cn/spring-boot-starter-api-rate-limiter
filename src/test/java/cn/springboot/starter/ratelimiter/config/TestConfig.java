package cn.springboot.starter.ratelimiter.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    public RateLimiterProperties rateLimiterProperties() {
        return new RateLimiterProperties();
    }
}