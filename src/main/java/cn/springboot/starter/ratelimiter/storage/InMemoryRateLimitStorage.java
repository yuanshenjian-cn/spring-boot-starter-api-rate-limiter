package cn.springboot.starter.ratelimiter.storage;

import cn.springboot.starter.ratelimiter.algorithm.RateLimitAlgorithm;

/**
 * 基于内存的限流存储实现
 *
 * @author Yuan Shenjian
 */
public class InMemoryRateLimitStorage {

    private final RateLimitAlgorithm algorithm;

    /**
     * 构造函数
     *
     * @param algorithm 限流算法
     */
    public InMemoryRateLimitStorage(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * 判断请求是否被允许
     *
     * @param key 限流键
     * @param permits 许可数量
     * @return 是否允许请求
     */
    public boolean isAllowed(String key, int permits) {
        return algorithm.isAllowed(key, permits);
    }
}