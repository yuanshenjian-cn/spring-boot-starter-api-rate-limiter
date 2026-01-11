package cn.springboot.starter.ratelimiter;

import cn.springboot.starter.ratelimiter.core.algorithm.FixedWindowCounterAlgorithm;
import cn.springboot.starter.ratelimiter.core.algorithm.LeakyBucketAlgorithm;
import cn.springboot.starter.ratelimiter.core.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void testFixedWindowCounterAlgorithm() {
        FixedWindowCounterAlgorithm algorithm =
            new FixedWindowCounterAlgorithm(3, 10000);

        String key = "test-fixed-window";

        assertTrue(algorithm.isAllowed(key, 1));
        assertTrue(algorithm.isAllowed(key, 1));
        assertTrue(algorithm.isAllowed(key, 1));

        assertFalse(algorithm.isAllowed(key, 1));
    }

    @Test
    void testLeakyBucketAlgorithm() {
        LeakyBucketAlgorithm algorithm =
            new LeakyBucketAlgorithm(5, 1);

        String key = "test-leaky-bucket";

        assertTrue(algorithm.isAllowed(key, 3));
        assertTrue(algorithm.isAllowed(key, 2));

        assertFalse(algorithm.isAllowed(key, 1));
    }

    @Test
    void testRateLimitException() {
        String message = "Rate limit exceeded";
        RateLimitException exception = new RateLimitException(message);

        assertEquals(message, exception.getMessage());
    }
}