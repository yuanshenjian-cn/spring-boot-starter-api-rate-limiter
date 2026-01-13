package cn.springboot.starter.ratelimiter.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 令牌桶限流注解（增强版）
 * 该注解可以应用于方法上以启用令牌桶限流功能
 * 支持更灵活的时间单位配置，如每分钟、每小时、每天等
 *
 * @author Yuan Shenjian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TokenBucketRateLimiter {

    /**
     * 限流键，支持 SpEL 表达式
     * @return 限流键
     */
    String key() default "";

    /**
     * 桶容量（最大令牌数）
     * @return 桶容量
     */
    long capacity() default 10;

    /**
     * 填充速率（在指定时间单位内填充的令牌数）
     * @return 填充速率
     */
    long refillRate() default 1;

    /**
     * 填充时间单位（秒）
     * @return 时间单位（秒）
     */
    long refillIntervalSeconds() default 1;

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