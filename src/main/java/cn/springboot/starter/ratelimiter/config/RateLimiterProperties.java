package cn.springboot.starter.ratelimiter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流器配置属性
 * 该类定义了 API 限流器的所有可配置属性
 *
 * @author Yuan Shenjian
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /**
     * 限流器是否启用
     */
    private boolean enabled = true;

    /**
     * 是否默认启用 Redis 存储。如果为 false，则使用本地内存
     */
    private boolean useRedisByDefault = false;

    /**
     * 限流的默认限制数量
     */
    private long defaultLimit = 10;

    /**
     * 限流的默认时间窗口（单位：秒）
     */
    private long defaultWindowSize = 60;

    /**
     * 超过限流时返回的默认消息
     */
    private String defaultMessage = "请求过于频繁，请稍后再试";
}