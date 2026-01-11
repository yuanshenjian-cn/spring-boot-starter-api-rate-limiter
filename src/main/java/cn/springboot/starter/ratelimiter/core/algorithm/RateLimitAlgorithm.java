package cn.springboot.starter.ratelimiter.core.algorithm;

/**
 * 限流算法接口
 *
 * @author Yuan Shenjian
 */
public interface RateLimitAlgorithm {

    /**
     * 判断请求是否被允许
     *
     * @param key 限流键
     * @param permits 许可数量
     * @return 是否允许请求
     */
    boolean isAllowed(String key, int permits);

    /**
     * 获取算法名称
     *
     * @return 算法名称
     */
    String getName();
}