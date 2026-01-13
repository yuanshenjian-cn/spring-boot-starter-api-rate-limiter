package cn.springboot.starter.api_rate_limiter.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 漏桶限流注解
 * 该注解可以应用于方法上以启用漏桶限流功能
 *
 * @author Yuan Shenjian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LeakyBucketRateLimiter {

    /**
     * 限流键，支持 SpEL 表达式
     * @return 限流键
     */
    String key() default "";

    /**
     * 桶容量（最大请求数）
     * @return 桶容量
     */
    long capacity() default 10;

    /**
     * 泄漏速率（每秒处理请求数）
     * @return 泄漏速率
     */
    long leakRate() default 1;

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
}