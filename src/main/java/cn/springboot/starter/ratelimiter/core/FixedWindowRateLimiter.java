package cn.springboot.starter.ratelimiter.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 固定窗口限流注解
 * 该注解可以应用于方法上以启用固定窗口限流功能
 *
 * @author Yuan Shenjian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FixedWindowRateLimiter {

    /**
     * 限流键，支持 SpEL 表达式
     * @return 限流键
     */
    String key() default "";

    /**
     * 限流数据的存储类型
     * @return 存储类型
     */
    RateLimiterEnums.StorageType storageType() default RateLimiterEnums.StorageType.REDIS;

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