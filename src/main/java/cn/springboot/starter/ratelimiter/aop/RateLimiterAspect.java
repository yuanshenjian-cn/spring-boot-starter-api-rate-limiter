package cn.springboot.starter.ratelimiter.aop;

import cn.springboot.starter.ratelimiter.algorithm.FixedWindowCounterAlgorithm;
import cn.springboot.starter.ratelimiter.algorithm.LeakyBucketAlgorithm;
import cn.springboot.starter.ratelimiter.algorithm.TokenBucketAlgorithm;
import cn.springboot.starter.ratelimiter.annotation.RateLimiter;
import cn.springboot.starter.ratelimiter.exception.RateLimitException;
import cn.springboot.starter.ratelimiter.storage.InMemoryRateLimitStorage;
import cn.springboot.starter.ratelimiter.storage.RedisRateLimitScriptFactory;
import cn.springboot.starter.ratelimiter.storage.RedisRateLimitStorage;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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
 * Aspect for implementing rate limiting functionality using annotations.
 * This aspect intercepts methods annotated with {@link RateLimiter} and applies rate limiting logic.
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
     * Constructor for the aspect.
     *
     * @param redisTemplate the Redis template for Redis-based rate limiting
     */
    public RateLimiterAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Intercepts method calls annotated with {@link RateLimiter} and applies rate limiting logic.
     *
     * @param point the join point representing the intercepted method
     * @param rateLimiter the rate limiter annotation
     * @return the result of the intercepted method if allowed, otherwise throws RateLimitException
     * @throws Throwable if the intercepted method throws an exception
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
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitException(rateLimiter.message());
        }

        return point.proceed();
    }

    /**
     * Generates a rate limiting key based on the method and arguments.
     *
     * @param method the method being called
     * @param args the method arguments
     * @param keyTemplate the key template from the annotation
     * @return the generated rate limiting key
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
     * Checks rate limiting using local memory storage.
     *
     * @param key the rate limiting key
     * @param rateLimiter the rate limiter annotation
     * @return true if the request is allowed, false otherwise
     */
    private boolean checkLocalMemoryRateLimit(String key, RateLimiter rateLimiter) {
        switch (rateLimiter.algorithm()) {
            case TOKEN_BUCKET:
                TokenBucketAlgorithm tokenBucketAlgorithm = new TokenBucketAlgorithm(
                        rateLimiter.capacity(),
                        rateLimiter.refillRate(),
                        1000
                );
                InMemoryRateLimitStorage tokenBucketStorage = new InMemoryRateLimitStorage(tokenBucketAlgorithm);
                return tokenBucketStorage.isAllowed(key, rateLimiter.permits());

            case LEAKY_BUCKET:
                LeakyBucketAlgorithm leakyBucketAlgorithm = new LeakyBucketAlgorithm(
                        rateLimiter.limit(),
                        rateLimiter.refillRate()
                );
                InMemoryRateLimitStorage leakyBucketStorage = new InMemoryRateLimitStorage(leakyBucketAlgorithm);
                return leakyBucketStorage.isAllowed(key, rateLimiter.permits());

            case FIXED_WINDOW:
                FixedWindowCounterAlgorithm fixedWindowAlgorithm = new FixedWindowCounterAlgorithm(
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
     * Checks rate limiting using Redis storage.
     *
     * @param key the rate limiting key
     * @param rateLimiter the rate limiter annotation
     * @return true if the request is allowed, false otherwise
     */
    private boolean checkRedisRateLimit(String key, RateLimiter rateLimiter) {
        if (redisTemplate == null) {
            throw new IllegalStateException("Redis template is not available but Redis storage is selected");
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