package cn.springboot.starter.ratelimiter.config;

import cn.springboot.starter.ratelimiter.core.handler.RateLimitExceptionHandler;
import cn.springboot.starter.ratelimiter.core.storage.script.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * API限流器的自动配置
 * 该类为限流功能提供自动配置
 *
 * @author Yuan Shenjian
 */
@Configuration
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimiterProperties.class)
@ComponentScan(basePackages = "cn.springboot.starter.ratelimiter")
public class RateLimiterAutoConfiguration {

    /**
     * 为固定窗口计数器算法创建ScriptFactory
     *
     * @return 固定窗口计数器的ScriptFactory
     */
    @Bean
    public FixedWindowCounterScriptFactory fixedWindowCounterScriptFactory() {
        return new FixedWindowCounterScriptFactory();
    }

    /**
     * 为令牌桶算法创建ScriptFactory
     *
     * @return 令牌桶的ScriptFactory
     */
    @Bean
    public TokenBucketScriptFactory tokenBucketScriptFactory() {
        return new TokenBucketScriptFactory();
    }

    /**
     * 为漏桶算法创建ScriptFactory
     *
     * @return 漏桶的ScriptFactory
     */
    @Bean
    public LeakyBucketScriptFactory leakyBucketScriptFactory() {
        return new LeakyBucketScriptFactory();
    }

    /**
     * 为滑动窗口日志算法创建ScriptFactory
     *
     * @return 滑动窗口日志的ScriptFactory
     */
    @Bean
    public SlidingWindowLogScriptFactory slidingWindowLogScriptFactory() {
        return new SlidingWindowLogScriptFactory();
    }

    /**
     * 为滑动窗口计数器算法创建ScriptFactory
     *
     * @return 滑动窗口计数器的ScriptFactory
     */
    @Bean
    public SlidingWindowCounterScriptFactory slidingWindowCounterScriptFactory() {
        return new SlidingWindowCounterScriptFactory();
    }
}