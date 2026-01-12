package cn.springboot.starter.ratelimiter.demo;

import cn.springboot.starter.ratelimiter.core.FixedWindowRateLimiter;
import cn.springboot.starter.ratelimiter.core.LeakyBucketRateLimiter;
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
     * 测试Redis限流功能 - 令牌桶算法
     *
     * @param userId 用户ID，默认为"1"
     * @return 测试结果字符串
     */
    @GetMapping("/api/test-token-bucket")
    @TokenBucketRateLimiter(
        key = "'test-token-bucket:' + #userId",
        capacity = 5,
        refillRate = 1,
        message = "Redis令牌桶限流：请求过于频繁，请稍后再试"
    )
    public String testTokenBucket(@RequestParam(defaultValue = "1") String userId) {
        return "Redis令牌桶限流测试通过，用户ID：" + userId;
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
}