package cn.springboot.starter.ratelimiter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for API rate limiting.
 * This annotation can be applied to methods to enable rate limiting functionality.
 *
 * @author Yuan Shenjian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * The key for rate limiting, supports SpEL expressions.
     * @return the rate limiting key
     */
    String key() default "";

    /**
     * The rate limiting algorithm to use.
     * @return the algorithm
     */
    Algorithm algorithm() default Algorithm.TOKEN_BUCKET;

    /**
     * The storage type for rate limiting data.
     * @return the storage type
     */
    StorageType storageType() default StorageType.LOCAL_MEMORY;

    /**
     * The maximum number of requests allowed within the time window.
     * @return the limit
     */
    long limit() default 10;

    /**
     * The time window size in seconds.
     * @return the window size in seconds
     */
    long windowSize() default 60;

    /**
     * The capacity of the bucket (for token bucket algorithm).
     * @return the bucket capacity
     */
    long capacity() default 10;

    /**
     * The refill rate (for token bucket) or leak rate (for leaky bucket).
     * @return the refill/leak rate
     */
    long refillRate() default 1;

    /**
     * The number of permits required for each request.
     * @return the number of permits
     */
    int permits() default 1;

    /**
     * The message to return when rate limit is exceeded.
     * @return the error message
     */
    String message() default "Too many requests, please try again later.";

    /**
     * Enum representing different rate limiting algorithms.
     */
    enum Algorithm {
        /** Token bucket algorithm */
        TOKEN_BUCKET,
        /** Leaky bucket algorithm */
        LEAKY_BUCKET,
        /** Fixed window counter algorithm */
        FIXED_WINDOW
    }

    /**
     * Enum representing different storage types for rate limiting data.
     */
    enum StorageType {
        /** Local memory storage */
        LOCAL_MEMORY,
        /** Redis storage */
        REDIS
    }
}