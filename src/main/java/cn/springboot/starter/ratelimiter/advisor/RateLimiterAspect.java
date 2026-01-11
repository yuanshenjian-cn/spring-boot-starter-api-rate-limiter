package cn.springboot.starter.ratelimiter.advisor;

import cn.springboot.starter.ratelimiter.algorithm.FixedWindowCounterAlgorithm;
import cn.springboot.starter.ratelimiter.algorithm.LeakyBucketAlgorithm;
import cn.springboot.starter.ratelimiter.algorithm.RateLimitAlgorithm;
import cn.springboot.starter.ratelimiter.algorithm.TokenBucketAlgorithm;
import cn.springboot.starter.ratelimiter.exception.RateLimitException;
import cn.springboot.starter.ratelimiter.storage.InMemoryRateLimitStorage;
import cn.springboot.starter.ratelimiter.storage.RedisRateLimitScriptFactory;
import cn.springboot.starter.ratelimiter.storage.RedisRateLimitStorage;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 使用注解实现限流功能的切面
 * 该切面拦截标记了 {@link RateLimiter} 注解的方法并应用限流逻辑
 *
 * @author Yuan Shenjian
 */
@Aspect
@Component
@Slf4j
public class RateLimiterAspect {
    private final ExpressionParser parser = new SpelExpressionParser();
    private final StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();

    private final StringRedisTemplate redisTemplate;

    /**
     * 切面构造函数
     *
     * @param redisTemplate 用于基于 Redis 的限流的 Redis 模板（可以为 null）
     */
    public RateLimiterAspect(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 拦截标记了 {@link RateLimiter} 注解的方法调用并应用限流逻辑
     *
     * @param point        表示被拦截方法的连接点
     * @param rateLimiter 限流注解
     * @return 如果允许则返回被拦截方法的结果，否则抛出 RateLimitException
     * @throws Throwable 如果被拦截方法抛出异常
     */
    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint point, RateLimiter rateLimiter) throws Throwable {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        String key = generateKey(method, point.getArgs(), rateLimiter.key());

        boolean allowed = switch (rateLimiter.storageType()) {
            case LOCAL_MEMORY -> checkLocalMemoryRateLimit(key, rateLimiter);
            case REDIS -> checkRedisRateLimit(key, rateLimiter);
        };

        if (!allowed) {
            log.warn("限流超出配额，键值: {}", key);
            throw new RateLimitException(rateLimiter.message());
        }

        return point.proceed();
    }

    /**
     * 基于方法和参数生成限流键
     *
     * @param method       被调用的方法
     * @param args         方法参数
     * @param keyTemplate 注解中的键模板
     * @return 生成的限流键
     */
    private String generateKey(Method method, Object[] args, String keyTemplate) {
        if (keyTemplate != null && !keyTemplate.isEmpty()) {
            EvaluationContext context = new MethodBasedEvaluationContext(
                    null, method, args, discoverer);
            return parser.parseExpression(keyTemplate).getValue(context, String.class);
        } else {
            return method.getDeclaringClass().getName() + ":" + method.getName();
        }
    }

    /**
     * 使用本地内存存储检查限流
     *
     * @param key         限流键
     * @param rateLimiter 限流注解
     * @return 如果请求被允许则返回 true，否则返回 false
     */
    private boolean checkLocalMemoryRateLimit(String key, RateLimiter rateLimiter) {
        switch (rateLimiter.algorithm()) {
            case TOKEN_BUCKET:
                RateLimitAlgorithm tokenBucketAlgorithm = new TokenBucketAlgorithm(
                        rateLimiter.capacity(),
                        rateLimiter.refillRate(),
                        1000
                );
                InMemoryRateLimitStorage tokenBucketStorage = new InMemoryRateLimitStorage(tokenBucketAlgorithm);
                return tokenBucketStorage.isAllowed(key, rateLimiter.permits());

            case LEAKY_BUCKET:
                RateLimitAlgorithm leakyBucketAlgorithm = new LeakyBucketAlgorithm(
                        rateLimiter.limit(),
                        rateLimiter.refillRate()
                );
                InMemoryRateLimitStorage leakyBucketStorage = new InMemoryRateLimitStorage(leakyBucketAlgorithm);
                return leakyBucketStorage.isAllowed(key, rateLimiter.permits());

            case FIXED_WINDOW:
                RateLimitAlgorithm fixedWindowAlgorithm = new FixedWindowCounterAlgorithm(
                        rateLimiter.limit(),
                        rateLimiter.windowSize() * 1000
                );
                InMemoryRateLimitStorage fixedWindowStorage = new InMemoryRateLimitStorage(fixedWindowAlgorithm);
                return fixedWindowStorage.isAllowed(key, rateLimiter.permits());

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + rateLimiter.algorithm());
        }
    }

    /**
     * 使用 Redis 存储检查限流
     *
     * @param key         限流键
     * @param rateLimiter 限流注解
     * @return 如果请求被允许则返回 true，否则返回 false
     */
    private boolean checkRedisRateLimit(String key, RateLimiter rateLimiter) {
        if (redisTemplate == null) {
            // 如果用户选择了Redis存储但没有配置Redis，则记录警告并拒绝请求
            log.warn("选择了Redis存储但Redis模板不可用。键值 {} 的限流将失败", key);
            return false; // 拒绝请求而不是抛出异常
        }

        switch (rateLimiter.algorithm()) {
            case FIXED_WINDOW:
                RedisScript<Long> fixedWindowScript = RedisRateLimitScriptFactory.createFixedWindowCounterScript();
                RedisRateLimitStorage fixedWindowRedisStorage = new RedisRateLimitStorage(redisTemplate, fixedWindowScript);
                return fixedWindowRedisStorage.isAllowed(key, rateLimiter.limit(), rateLimiter.windowSize(), rateLimiter.permits());

            case TOKEN_BUCKET:
                RedisScript<Long> tokenBucketScript = RedisRateLimitScriptFactory.createTokenBucketScript();
                RedisRateLimitStorage tokenBucketRedisStorage = new RedisRateLimitStorage(redisTemplate, tokenBucketScript);
                return tokenBucketRedisStorage.isAllowedForTokenBucket(key, rateLimiter.capacity(), rateLimiter.refillRate(), rateLimiter.permits());

            case LEAKY_BUCKET:
                RedisScript<Long> leakyBucketScript = RedisRateLimitScriptFactory.createFixedWindowCounterScript();
                RedisRateLimitStorage leakyBucketRedisStorage = new RedisRateLimitStorage(redisTemplate, leakyBucketScript);
                return leakyBucketRedisStorage.isAllowedForLeakyBucket(key, rateLimiter.limit(), rateLimiter.refillRate(), rateLimiter.permits());

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + rateLimiter.algorithm());
        }
    }
}