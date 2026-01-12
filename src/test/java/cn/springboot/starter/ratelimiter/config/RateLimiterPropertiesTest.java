package cn.springboot.starter.ratelimiter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RateLimiterPropertiesTest {

    @Test
    void testDefaultValues() {
        RateLimiterProperties properties = new RateLimiterProperties();
        
        assertTrue(properties.isEnabled());
        assertEquals(10, properties.getDefaultLimit());
        assertEquals(60, properties.getDefaultWindowSize());
        assertEquals("请求过于频繁，请稍后再试", properties.getDefaultMessage());
        assertEquals(429, properties.getRateLimitHttpStatus());
        assertFalse(properties.isEnableMetrics());
        assertEquals(2000, properties.getRedisTimeout());
        assertFalse(properties.isRedisSslEnabled());
        assertEquals(255, properties.getMaxKeyLength());
    }

    @Test
    void testSetterAndGetters() {
        RateLimiterProperties properties = new RateLimiterProperties();
        
        properties.setEnabled(false);
        properties.setDefaultLimit(20);
        properties.setDefaultWindowSize(120);
        properties.setDefaultMessage("Custom message");
        properties.setRateLimitHttpStatus(503);
        properties.setEnableMetrics(true);
        properties.setRedisTimeout(5000);
        properties.setRedisSslEnabled(true);
        properties.setMaxKeyLength(500);
        
        assertFalse(properties.isEnabled());
        assertEquals(20, properties.getDefaultLimit());
        assertEquals(120, properties.getDefaultWindowSize());
        assertEquals("Custom message", properties.getDefaultMessage());
        assertEquals(503, properties.getRateLimitHttpStatus());
        assertTrue(properties.isEnableMetrics());
        assertEquals(5000, properties.getRedisTimeout());
        assertTrue(properties.isRedisSslEnabled());
        assertEquals(500, properties.getMaxKeyLength());
    }
}