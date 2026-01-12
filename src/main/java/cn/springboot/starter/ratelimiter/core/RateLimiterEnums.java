package cn.springboot.starter.ratelimiter.core;

/**
 * 限流器公共枚举定义
 * 该类定义了限流器中使用的公共枚举
 *
 * @author Yuan Shenjian
 */
public class RateLimiterEnums {

    /**
     * 限流数据存储类型的枚举
     */
    public enum StorageType {
        /** Redis 存储 */
        REDIS
    }
}