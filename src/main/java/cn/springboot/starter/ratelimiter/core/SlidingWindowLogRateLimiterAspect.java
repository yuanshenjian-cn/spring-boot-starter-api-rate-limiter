package cn.springboot.starter.ratelimiter.core;

import cn.springboot.starter.ratelimiter.config.RateLimiterProperties;
import cn.springboot.starter.ratelimiter.core.exception.RateLimitException;
import cn.springboot.starter.ratelimiter.core.storage.RedisRateLimitStorage;
import cn.springboot.starter.ratelimiter.core.storage.script.SlidingWindowLogScriptFactory;
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
 * 滑动窗口日志限流切面
 * 该切面拦截标记了滑动窗口日志限流注解的方法并应用限流逻辑
 *
 * @author Yuan Shenjian
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "rate-limiter.enabled", havingValue = "true", matchIfMissing = true)
public class SlidingWindowLogRateLimiterAspect extends AbstractRateLimiterAspect {

    private final RedisScript<Long> slidingWindowLogScript;

    /**
     * 构造函数
     *
     * @param redisTemplate 用于基于 Redis 的限流的 Redis 模板（可以为 null）
     * @param properties 限流器配置属性
     * @param scriptFactory 滑动窗口日志限流脚本工厂
     */
    public SlidingWindowLogRateLimiterAspect(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                           RateLimiterProperties properties,
                                           @Autowired(required = false) SlidingWindowLogScriptFactory scriptFactory) {
        super(redisTemplate, properties, null);
        this.slidingWindowLogScript = scriptFactory != null ? scriptFactory.createRateLimitScript() : null;
    }

    /**
     * 拦截滑动窗口日志限流注解的方法调用并应用限流逻辑
     *
     * @param point        表示被拦截方法的连接点
     * @param rateLimiter 限流注解
     * @return 如果允许则返回被拦截方法的结果，否则抛出 RateLimitException
     * @throws Throwable 如果被拦截方法抛出异常
     */
    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint point, SlidingWindowLogRateLimiter rateLimiter) throws Throwable {
        Method method = getMethod(point);
        String key = generateKey(method, point.getArgs(), rateLimiter.key());

        long startTime = System.nanoTime();
        boolean allowed = checkSlidingWindowLogRateLimit(key, rateLimiter);
        long executionTime = System.nanoTime() - startTime;

        if (!allowed) {
            log.warn("滑动窗口日志限流超出配额，键值: {}", key);

            throw new RateLimitException(rateLimiter.message());
        }

        return point.proceed();
    }

    /**
     * 使用 Redis 存储检查滑动窗口日志限流
     *
     * @param key         限流键
     * @param rateLimiter 限流注解
     * @return 如果请求被允许则返回 true，否则返回 false
     */
    private boolean checkSlidingWindowLogRateLimit(String key, SlidingWindowLogRateLimiter rateLimiter) {
        if (!checkRedisAndScriptAvailability(key, slidingWindowLogScript)) {
            return false;
        }

        RedisRateLimitStorage slidingWindowLogRedisStorage = new RedisRateLimitStorage(redisTemplate, slidingWindowLogScript);
        return slidingWindowLogRedisStorage.isAllowed(key, rateLimiter.limit(), rateLimiter.windowSize(), rateLimiter.permits());
    }
}