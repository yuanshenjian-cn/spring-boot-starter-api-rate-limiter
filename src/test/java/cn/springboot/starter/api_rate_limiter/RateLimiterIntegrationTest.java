package cn.springboot.starter.api_rate_limiter;

import cn.springboot.starter.api_rate_limiter.core.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = RateLimiterIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(cn.springboot.starter.api_rate_limiter.config.RateLimiterAutoConfiguration.class)
public class RateLimiterIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testFixedWindowRateLimiter() throws InterruptedException {
        String url = "/test/fixed-window?userId=test1";

        // 发送5个请求，应该都成功（限制是5次）
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(200, response.getStatusCodeValue(), "第" + (i + 1) + "个请求应该成功");
            assertTrue(response.getBody().contains("固定窗口限流测试成功"), "响应应该包含成功信息");
        }

        // 第6个请求应该被限流
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(429, response.getStatusCodeValue(), "第6个请求应该被限流");
    }

    @Test
    public void testSlidingWindowCounterRateLimiter() throws InterruptedException {
        String url = "/test/sliding-window-counter?userId=test2";

        // 发送5个请求，应该都成功（限制是5次）
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(200, response.getStatusCodeValue(), "第" + (i + 1) + "个请求应该成功");
            assertTrue(response.getBody().contains("滑动窗口计数器限流测试成功"), "响应应该包含成功信息");
        }

        // 第6个请求应该被限流
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(429, response.getStatusCodeValue(), "第6个请求应该被限流");
    }

    @Test
    public void testTokenBucketRateLimiter() throws InterruptedException {
        String url = "/test/token-bucket?userId=test3";

        // 发送5个请求，应该都成功（桶容量是10）
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(200, response.getStatusCodeValue(), "第" + (i + 1) + "个请求应该成功");
            assertTrue(response.getBody().contains("令牌桶限流测试成功"), "响应应该包含成功信息");
        }

        // 再快速发送多个请求，部分可能会被限流
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < 8; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCodeValue() == 200) {
                successCount++;
            } else if (response.getStatusCodeValue() == 429) {
                failCount++;
            }
            Thread.sleep(10); // 短暂延迟，让令牌桶有时间填充
        }

        // 令牌桶算法允许突发请求，但总体速率受限
        System.out.println("令牌桶测试 - 成功: " + successCount + ", 失败: " + failCount);
    }

    @Test
    public void testLeakyBucketRateLimiter() throws InterruptedException {
        String url = "/test/leaky-bucket?userId=test4";

        // 发送5个请求，应该都成功（桶容量是10）
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(200, response.getStatusCodeValue(), "第" + (i + 1) + "个请求应该成功");
            assertTrue(response.getBody().contains("漏桶限流测试成功"), "响应应该包含成功信息");
        }

        // 再快速发送多个请求，部分可能会被限流
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < 8; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCodeValue() == 200) {
                successCount++;
            } else if (response.getStatusCodeValue() == 429) {
                failCount++;
            }
            Thread.sleep(10); // 短暂延迟
        }

        // 漏桶算法以固定速率处理请求
        System.out.println("漏桶测试 - 成功: " + successCount + ", 失败: " + failCount);
    }

    @Test
    public void testSlidingWindowLogRateLimiter() throws InterruptedException {
        String url = "/test/sliding-window-log?userId=test5";

        // 发送5个请求，应该都成功（限制是5次）
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(200, response.getStatusCodeValue(), "第" + (i + 1) + "个请求应该成功");
            assertTrue(response.getBody().contains("滑动窗口日志限流测试成功"), "响应应该包含成功信息");
        }

        // 第6个请求应该被限流
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(429, response.getStatusCodeValue(), "第6个请求应该被限流");
    }

    /**
     * 测试结束后清理 Redis 中的限流键
     */
    @AfterEach
    public void cleanupRedisKeys() {
        try {
            // 定义要清理的键模式
            String[] patterns = {
                "fixed:*",
                "sliding_counter:*",
                "token_bucket:*",
                "leaky_bucket:*",
                "sliding_log:*"
            };

            // 遍历每个模式并删除匹配的键
            for (String pattern : patterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    System.out.println("已清理 Redis 键: " + keys.size() + " 个，模式: " + pattern);
                }
            }
        } catch (Exception e) {
            System.err.println("清理 Redis 键时发生错误: " + e.getMessage());
        }
    }

    @SpringBootApplication
    @Import(cn.springboot.starter.api_rate_limiter.config.RateLimiterAutoConfiguration.class)
    @ComponentScan(basePackages = {"cn.springboot.starter.api_rate_limiter"})
    public static class TestApplication {
        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }

    @RestController
    public static class TestController {

        // 固定窗口限流测试
        @GetMapping("/test/fixed-window")
        @FixedWindowRateLimiter(key = "'fixed:' + #userId", limit = 5, windowSize = 60, message = "固定窗口限流：请求过于频繁")
        public String fixedWindowTest(@RequestParam String userId) {
            return "固定窗口限流测试成功 - 用户: " + userId + ", 时间: " + System.currentTimeMillis();
        }

        // 滑动窗口计数器限流测试
        @GetMapping("/test/sliding-window-counter")
        @SlidingWindowCounterRateLimiter(key = "'sliding_counter:' + #userId", limit = 5, windowSize = 60, message = "滑动窗口计数器限流：请求过于频繁")
        public String slidingWindowCounterTest(@RequestParam String userId) {
            return "滑动窗口计数器限流测试成功 - 用户: " + userId + ", 时间: " + System.currentTimeMillis();
        }

        // 令牌桶限流测试
        @GetMapping("/test/token-bucket")
        @TokenBucketRateLimiter(key = "'token_bucket:' + #userId", capacity = 10, refillRate = 2, permits = 1, message = "令牌桶限流：请求过于频繁")
        public String tokenBucketTest(@RequestParam String userId) {
            return "令牌桶限流测试成功 - 用户: " + userId + ", 时间: " + System.currentTimeMillis();
        }

        // 漏桶限流测试
        @GetMapping("/test/leaky-bucket")
        @LeakyBucketRateLimiter(key = "'leaky_bucket:' + #userId", capacity = 10, leakRate = 2, permits = 1, message = "漏桶限流：请求过于频繁")
        public String leakyBucketTest(@RequestParam String userId) {
            return "漏桶限流测试成功 - 用户: " + userId + ", 时间: " + System.currentTimeMillis();
        }

        // 滑动窗口日志限流测试
        @GetMapping("/test/sliding-window-log")
        @SlidingWindowLogRateLimiter(key = "'sliding_log:' + #userId", limit = 5, windowSize = 60, message = "滑动窗口日志限流：请求过于频繁")
        public String slidingWindowLogTest(@RequestParam String userId) {
            return "滑动窗口日志限流测试成功 - 用户: " + userId + ", 时间: " + System.currentTimeMillis();
        }
    }
}