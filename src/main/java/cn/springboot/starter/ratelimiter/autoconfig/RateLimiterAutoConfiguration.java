package cn.springboot.starter.ratelimiter.autoconfig;

import cn.springboot.starter.ratelimiter.storage.RedisRateLimitScriptFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * API限流器的自动配置
 * 该类为限流功能提供自动配置
 *
 * @author Yuan Shenjian
 */
@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterAutoConfiguration {

    /**
     * 为固定窗口计数器算法创建Redis脚本
     *
     * @return 固定窗口计数器的Redis脚本
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisScript<Long> fixedWindowCounterRedisScript() {
        return RedisRateLimitScriptFactory.createFixedWindowCounterScript();
    }

    /**
     * 为令牌桶算法创建Redis脚本
     *
     * @return 令牌桶的Redis脚本
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisScript<Long> tokenBucketRedisScript() {
        return RedisRateLimitScriptFactory.createTokenBucketScript();
    }

    /**
     * 创建限流器属性 Bean
     *
     * @return 限流器属性
     */
    @Bean
    public RateLimiterProperties rateLimiterProperties() {
        return new RateLimiterProperties();
    }
}