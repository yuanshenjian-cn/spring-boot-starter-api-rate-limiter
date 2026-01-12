package cn.springboot.starter.ratelimiter;

import cn.springboot.starter.ratelimiter.config.RateLimiterProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RateLimiterApplicationTest {

    @Test
    void contextLoads() {
        assertTrue(true); // 简单的上下文加载测试
    }
}