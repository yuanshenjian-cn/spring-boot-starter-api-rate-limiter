package cn.springboot.starter.ratelimiter.core;

import cn.springboot.starter.ratelimiter.config.RateLimiterProperties;
import cn.springboot.starter.ratelimiter.core.exception.RateLimitException;
import cn.springboot.starter.ratelimiter.core.storage.RedisRateLimitScriptFactory;
import cn.springboot.starter.ratelimiter.core.storage.RedisRateLimitStorage;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 固定窗口限流切面
 * 该切面拦截标记了固定窗口限流注解的方法并应用限流逻辑
 *
 * @author Yuan Shenjian
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "rate-limiter.enabled", havingValue = "true", matchIfMissing = true)
public class FixedWindowRateLimiterAspect extends AbstractRateLimiterAspect {

    /**
     * 构造函数
     *
     * @param redisTemplate 用于基于 Redis 的限流的 Redis 模板（可以为 null）
     * @param properties 限流器配置属性
     */
    public FixedWindowRateLimiterAspect(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                        RateLimiterProperties properties) {
        super(redisTemplate, properties, null);
    }

    /**
     * 拦截固定窗口限流注解的方法调用并应用限流逻辑
     *
     * @param point        表示被拦截方法的连接点
     * @param rateLimiter 限流注解
     * @return 如果允许则返回被拦截方法的结果，否则抛出 RateLimitException
     * @throws Throwable 如果被拦截方法抛出异常
     */
    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint point, FixedWindowRateLimiter rateLimiter) throws Throwable {
        Method method = getMethod(point);
        String key = generateKey(method, point.getArgs(), rateLimiter.key());

        long startTime = System.nanoTime();
        boolean allowed = checkFixedWindowRateLimit(key, rateLimiter);
        long executionTime = System.nanoTime() - startTime;

        if (!allowed) {
            log.warn("固定窗口限流超出配额，键值: {}", key);

            throw new RateLimitException(rateLimiter.message());
        }

        return point.proceed();
    }

    /**
     * 使用 Redis 存储检查固定窗口限流
     *
     * @param key         限流键
     * @param rateLimiter 限流注解
     * @return 如果请求被允许则返回 true，否则返回 false
     */
    private boolean checkFixedWindowRateLimit(String key, FixedWindowRateLimiter rateLimiter) {
        if (redisTemplate == null) {
            // 如果用户选择了Redis存储但没有配置Redis，则记录警告并拒绝请求
            log.warn("选择了Redis存储但Redis模板不可用。键值 {} 的限流将失败", key);
            return false; // 拒绝请求而不是抛出异常
        }

        RedisScript<Long> fixedWindowScript = RedisRateLimitScriptFactory.createFixedWindowCounterScript();
        RedisRateLimitStorage fixedWindowRedisStorage = new RedisRateLimitStorage(redisTemplate, fixedWindowScript);
        return fixedWindowRedisStorage.isAllowed(key, rateLimiter.limit(), rateLimiter.windowSize(), rateLimiter.permits());
    }
}