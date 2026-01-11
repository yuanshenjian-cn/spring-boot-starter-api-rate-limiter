package cn.springboot.starter.ratelimiter.core.storage;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * 基于Redis的限流存储实现
 *
 * @author Yuan Shenjian
 */
public class RedisRateLimitStorage {
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimitScript;

    /**
     * 构造函数
     *
     * @param redisTemplate Redis模板
     * @param rateLimitScript 限流脚本
     */
    public RedisRateLimitStorage(StringRedisTemplate redisTemplate, RedisScript<Long> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
    }

    /**
     * 判断请求是否被允许
     *
     * @param key 限流键
     * @param limit 限制数量
     * @param windowSizeInSeconds 窗口大小（秒）
     * @param permits 许可数量
     * @return 是否允许请求
     */
    public boolean isAllowed(String key, long limit, long windowSizeInSeconds, int permits) {
        Long result = redisTemplate.execute(rateLimitScript, List.of(key),
                String.valueOf(limit),
                String.valueOf(windowSizeInSeconds),
                String.valueOf(permits));
        return result == 1L;
    }

    /**
     * 令牌桶算法判断请求是否被允许
     *
     * @param key 限流键
     * @param capacity 桶容量
     * @param refillRate 填充速率
     * @param permits 许可数量
     * @return 是否允许请求
     */
    public boolean isAllowedForTokenBucket(String key, long capacity, long refillRate, int permits) {
        Long result = redisTemplate.execute(rateLimitScript, List.of(key),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(permits));

        return result == 1L;
    }

    /**
     * 漏桶算法判断请求是否被允许
     *
     * @param key 限流键
     * @param limit 限制数量
     * @param leakRate 泄露速率
     * @param permits 许可数量
     * @return 是否允许请求
     */
    public boolean isAllowedForLeakyBucket(String key, long limit, long leakRate, int permits) {
        return isAllowed(key, limit, 1, permits);
    }
}