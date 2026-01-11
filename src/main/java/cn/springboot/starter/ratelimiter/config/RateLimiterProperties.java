package cn.springboot.starter.ratelimiter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the rate limiter.
 * This class defines all configurable properties for the API rate limiter.
 *
 * @author Yuan Shenjian
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /**
     * Whether the rate limiter is enabled.
     */
    private boolean enabled = true;

    /**
     * The default storage type to use.
     */
    private StorageType defaultStorageType = StorageType.LOCAL_MEMORY;

    /**
     * The default algorithm to use.
     */
    private Algorithm defaultAlgorithm = Algorithm.TOKEN_BUCKET;

    /**
     * The default limit for rate limiting.
     */
    private long defaultLimit = 10;

    /**
     * The default window size in seconds.
     */
    private long defaultWindowSize = 60;

    /**
     * The default capacity for token bucket algorithm.
     */
    private long defaultCapacity = 10;

    /**
     * The default refill rate for token/leaky bucket algorithms.
     */
    private long defaultRefillRate = 1;

    /**
     * The default number of permits required for each request.
     */
    private int defaultPermits = 1;

    /**
     * The default message to return when rate limit is exceeded.
     */
    private String defaultMessage = "Too many requests, please try again later.";

    /**
     * Enum representing different storage types for rate limiting data.
     */
    public enum StorageType {
        /** Local memory storage */
        LOCAL_MEMORY,
        /** Redis storage */
        REDIS
    }

    /**
     * Enum representing different rate limiting algorithms.
     */
    public enum Algorithm {
        /** Token bucket algorithm */
        TOKEN_BUCKET,
        /** Leaky bucket algorithm */
        LEAKY_BUCKET,
        /** Fixed window counter algorithm */
        FIXED_WINDOW
    }
}