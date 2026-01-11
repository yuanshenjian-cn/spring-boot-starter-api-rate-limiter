package cn.springboot.starter.ratelimiter.advisor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 限流注解
 * 该注解可以应用于方法上以启用限流功能
 *
 * @author Yuan Shenjian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * 限流键，支持 SpEL 表达式
     * @return 限流键
     */
    String key() default "";

    /**
     * 要使用的限流算法
     * @return 算法
     */
    Algorithm algorithm() default Algorithm.TOKEN_BUCKET;

    /**
     * 限流数据的存储类型
     * @return 存储类型
     */
    StorageType storageType() default StorageType.LOCAL_MEMORY;

    /**
     * 时间窗口内允许的最大请求数
     * @return 限制数量
     */
    long limit() default 10;

    /**
     * 时间窗口大小（单位：秒）
     * @return 窗口大小（秒）
     */
    long windowSize() default 60;

    /**
     * 桶容量（用于令牌桶算法）
     * @return 桶容量
     */
    long capacity() default 10;

    /**
     * 填充速率（用于令牌桶）或泄漏速率（用于漏桶）
     * @return 填充/泄漏速率
     */
    long refillRate() default 1;

    /**
     * 每个请求所需的许可数量
     * @return 许可数量
     */
    int permits() default 1;

    /**
     * 超过限流时返回的消息
     * @return 错误消息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 限流算法的枚举
     */
    enum Algorithm {
        /** 令牌桶算法 */
        TOKEN_BUCKET,
        /** 漏桶算法 */
        LEAKY_BUCKET,
        /** 固定窗口计数器算法 */
        FIXED_WINDOW
    }

    /**
     * 限流数据存储类型的枚举
     */
    enum StorageType {
        /** 本地内存存储 */
        LOCAL_MEMORY,
        /** Redis 存储 */
        REDIS
    }
}