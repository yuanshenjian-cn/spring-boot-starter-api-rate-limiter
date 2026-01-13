package cn.springboot.starter.api_rate_limiter.core;

import cn.springboot.starter.api_rate_limiter.config.RateLimiterProperties;
import cn.springboot.starter.api_rate_limiter.core.exception.RateLimitException;
import cn.springboot.starter.api_rate_limiter.core.storage.RedisRateLimitStorage;
import cn.springboot.starter.api_rate_limiter.core.storage.script.TokenBucketScriptFactory;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 令牌桶限流切面
 * 该切面拦截标记了令牌桶限流注解的方法并应用限流逻辑
 *
 * @author Yuan Shenjian
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "rate-limiter.enabled", havingValue = "true", matchIfMissing = true)
public class TokenBucketRateLimiterAspect extends AbstractRateLimiterAspect {

    private final RedisScript<Long> tokenBucketScript;

    /**
     * 构造函数
     *
     * @param redisTemplate 用于基于 Redis 的限流的 Redis 模板（可以为 null）
     * @param properties 限流器配置属性
     * @param scriptFactory 令牌桶限流脚本工厂
     */
    public TokenBucketRateLimiterAspect(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                        RateLimiterProperties properties,
                                        @Autowired(required = false) TokenBucketScriptFactory scriptFactory) {
        super(redisTemplate, properties, null);
        this.tokenBucketScript = scriptFactory != null ? scriptFactory.createRateLimitScript() : null;
    }

    /**
     * 拦截令牌桶限流注解的方法调用并应用限流逻辑
     *
     * @param point        表示被拦截方法的连接点
     * @param rateLimiter 限流注解
     * @return 如果允许则返回被拦截方法的结果，否则抛出 RateLimitException
     * @throws Throwable 如果被拦截方法抛出异常
     */
    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint point, TokenBucketRateLimiter rateLimiter) throws Throwable {
        Method method = getMethod(point);
        String key = generateKey(method, point.getArgs(), rateLimiter.key());

        long startTime = System.nanoTime();
        boolean allowed = checkTokenBucketRateLimit(key, rateLimiter);
        long executionTime = System.nanoTime() - startTime;

        if (!allowed) {
            log.warn("令牌桶限流超出配额，键值: {}", key);

            throw new RateLimitException(rateLimiter.message());
        }

        return point.proceed();
    }

    /**
     * 使用 Redis 存储检查令牌桶限流（增强版）
     *
     * @param key         限流键
     * @param rateLimiter 限流注解
     * @return 如果请求被允许则返回 true，否则返回 false
     */
    private boolean checkTokenBucketRateLimit(String key, TokenBucketRateLimiter rateLimiter) {
        if (!checkRedisAndScriptAvailability(key, tokenBucketScript)) {
            return false;
        }

        RedisRateLimitStorage tokenBucketRedisStorage = new RedisRateLimitStorage(redisTemplate, tokenBucketScript);
        return tokenBucketRedisStorage.isAllowedForTokenBucket(key, rateLimiter.capacity(), rateLimiter.refillRate(), rateLimiter.refillIntervalSeconds(), rateLimiter.permits());
    }
}