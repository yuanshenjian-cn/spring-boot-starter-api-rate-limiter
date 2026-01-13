package cn.springboot.starter.ratelimiter.demo;

import cn.springboot.starter.ratelimiter.core.FixedWindowRateLimiter;
import cn.springboot.starter.ratelimiter.core.LeakyBucketRateLimiter;
import cn.springboot.starter.ratelimiter.core.SlidingWindowCounterRateLimiter;
import cn.springboot.starter.ratelimiter.core.SlidingWindowLogRateLimiter;
import cn.springboot.starter.ratelimiter.core.TokenBucketRateLimiter;
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
     * 测试Redis限流功能 - 令牌桶算法（每秒1个令牌）
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-token-bucket")
    @TokenBucketRateLimiter(
        key = "'test-token-bucket:' + #userId",
        capacity = 5,
        refillRate = 1,
        refillIntervalSeconds = 1, // 每秒补充1个令牌
        message = "Redis令牌桶限流：请求过于频繁，请稍后再试"
    )
    public String testTokenBucket(@RequestParam(defaultValue = "1") String userId) {
        return "Redis令牌桶限流测试通过，用户ID：" + userId;
    }

    /**
     * 测试Redis限流功能 - 增强版令牌桶算法（每小时10次）
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-enhanced-token-bucket")
    @TokenBucketRateLimiter(
        key = "'test-enhanced-token-bucket:' + #userId",
        capacity = 10,
        refillRate = 10,
        refillIntervalSeconds = 3600, // 每小时补充10个令牌
        message = "Redis增强版令牌桶限流：每小时最多调用10次"
    )
    public String testEnhancedTokenBucket(@RequestParam(defaultValue = "1") String userId) {
        return "Redis增强版令牌桶限流测试通过，用户ID：" + userId;
    }

    /**
     * 测试Redis限流功能 - 固定窗口算法
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-fixed-window")
    @FixedWindowRateLimiter(
        key = "'test-fixed-window:' + #userId",
        limit = 3,
        windowSize = 60,
        message = "Redis固定窗口限流：请求过于频繁，请稍后再试"
    )
    public String testFixedWindow(@RequestParam(defaultValue = "1") String userId) {
        return "Redis固定窗口限流测试通过，用户ID：" + userId;
    }

    /**
     * 测试Redis限流功能 - 漏桶算法
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-leaky-bucket")
    @LeakyBucketRateLimiter(
        key = "'test-leaky-bucket:' + #userId",
        capacity = 5,
        leakRate = 1,
        message = "Redis漏桶限流：请求过于频繁，请稍后再试"
    )
    public String testLeakyBucket(@RequestParam(defaultValue = "1") String userId) {
        return "Redis漏桶限流测试通过，用户ID：" + userId;
    }

    /**
     * 测试Redis限流功能 - 滑动窗口日志算法
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-sliding-window-log")
    @SlidingWindowLogRateLimiter(
        key = "'test-sliding-window-log:' + #userId",
        limit = 5,
        windowSize = 60,
        message = "Redis滑动窗口日志限流：请求过于频繁，请稍后再试"
    )
    public String testSlidingWindowLog(@RequestParam(defaultValue = "1") String userId) {
        return "Redis滑动窗口日志限流测试通过，用户ID：" + userId;
    }

    /**
     * 测试Redis限流功能 - 滑动窗口计数器算法
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-sliding-window-counter")
    @SlidingWindowCounterRateLimiter(
        key = "'test-sliding-window-counter:' + #userId",
        limit = 5,
        windowSize = 60,
        subWindows = 10,
        message = "Redis滑动窗口计数器限流：请求过于频繁，请稍后再试"
    )
    public String testSlidingWindowCounter(@RequestParam(defaultValue = "1") String userId) {
        return "Redis滑动窗口计数器限流测试通过，用户ID：" + userId;
    }
}