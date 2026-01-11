package cn.springboot.starter.ratelimiter.config;

import cn.springboot.starter.ratelimiter.storage.RedisRateLimitScriptFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Auto-configuration for the API rate limiter.
 * This class provides automatic configuration for the rate limiting functionality.
 *
 * @author Yuan Shenjian
 */
@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterAutoConfiguration {

    /**
     * Creates a Redis script for the fixed window counter algorithm.
     *
     * @return the Redis script for fixed window counter
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisScript<Long> fixedWindowCounterRedisScript() {
        return RedisRateLimitScriptFactory.createFixedWindowCounterScript();
    }

    /**
     * Creates a Redis script for the token bucket algorithm.
     *
     * @return the Redis script for token bucket
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisScript<Long> tokenBucketRedisScript() {
        return RedisRateLimitScriptFactory.createTokenBucketScript();
    }

    /**
     * Creates the rate limiter properties bean.
     *
     * @return the rate limiter properties
     */
    @Bean
    public RateLimiterProperties rateLimiterProperties() {
        return new RateLimiterProperties();
    }
}