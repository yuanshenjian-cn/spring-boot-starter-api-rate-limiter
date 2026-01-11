package cn.springboot.starter.ratelimiter.demo;

import cn.springboot.starter.ratelimiter.core.RateLimiter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例控制器，演示如何使用限流注解
 *
 * @author Yuan Shenjian
 */
@RestController
public class ExampleController {

    /**
     * 测试本地内存限流功能
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-local-memory")
    @RateLimiter(
        key = "'test-local:' + #userId",
        algorithm = RateLimiter.Algorithm.TOKEN_BUCKET,
        storageType = RateLimiter.StorageType.LOCAL_MEMORY,
        limit = 5,
        windowSize = 60,
        message = "本地内存限流：请求过于频繁，请稍后再试"
    )
    public String testLocalMemory(@RequestParam(defaultValue = "1") String userId) {
        return "本地内存限流测试通过，用户ID：" + userId;
    }

    /**
     * 测试Redis限流功能
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-redis")
    @RateLimiter(
        key = "'test-redis:' + #userId",
        algorithm = RateLimiter.Algorithm.FIXED_WINDOW,
        storageType = RateLimiter.StorageType.REDIS,
        limit = 3,
        windowSize = 60,
        message = "Redis限流：请求过于频繁，请稍后再试"
    )
    public String testRedis(@RequestParam(defaultValue = "1") String userId) {
        return "Redis限流测试通过，用户ID：" + userId;
    }

    /**
     * 测试漏水桶限流功能
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-leaky-bucket")
    @RateLimiter(
        key = "'test-leaky:' + #userId",
        algorithm = RateLimiter.Algorithm.LEAKY_BUCKET,
        storageType = RateLimiter.StorageType.LOCAL_MEMORY,
        limit = 5,
        refillRate = 1,
        message = "漏水桶限流：请求过于频繁，请稍后再试"
    )
    public String testLeakyBucket(@RequestParam(defaultValue = "1") String userId) {
        return "漏水桶限流测试通过，用户ID：" + userId;
    }
}