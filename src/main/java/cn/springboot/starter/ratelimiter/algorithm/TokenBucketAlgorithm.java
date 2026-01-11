package cn.springboot.starter.ratelimiter.algorithm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶算法实现
 *
 * @author Yuan Shenjian
 */
public class TokenBucketAlgorithm implements RateLimitAlgorithm {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long refillTokens;
    private final long refillIntervalMillis;

    /**
     * 构造函数
     *
     * @param capacity 桶容量
     * @param refillTokens 填充令牌数
     * @param refillIntervalMillis 填充间隔（毫秒）
     */
    public TokenBucketAlgorithm(long capacity, long refillTokens, long refillIntervalMillis) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMillis = refillIntervalMillis;
    }

    @Override
    public boolean isAllowed(String key, int permits) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(refillTokens, refillIntervalMillis, capacity));
        return bucket.tryConsume(permits);
    }

    @Override
    public String getName() {
        return "TOKEN_BUCKET";
    }

    /**
     * 桶内部类
     */
    private static class Bucket {
        private final long capacity;
        private final long refillTokens;
        private final long refillIntervalMillis;
        private final AtomicLong tokens;
        private volatile long lastRefillTime;

        /**
         * 构造函数
         *
         * @param refillTokens 填充令牌数
         * @param refillIntervalMillis 填充间隔（毫秒）
         * @param capacity 桶容量
         */
        public Bucket(long refillTokens, long refillIntervalMillis, long capacity) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillIntervalMillis = refillIntervalMillis;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }

        /**
         * 尝试消费令牌
         *
         * @param permits 许可数量
         * @return 是否允许请求
         */
        public boolean tryConsume(int permits) {
            refill();
            if (tokens.get() >= permits) {
                tokens.addAndGet(-permits);
                return true;
            } else {
                return false;
            }
        }

        /**
         * 填充令牌
         */
        private void refill() {
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - lastRefillTime;

            if (timeElapsed >= refillIntervalMillis) {
                long refillCount = (timeElapsed / refillIntervalMillis) * refillTokens;

                long currentTokens = tokens.get();
                long newTokens = Math.min(capacity, currentTokens + refillCount);

                if (tokens.compareAndSet(currentTokens, newTokens)) {
                    lastRefillTime = currentTime - (timeElapsed % refillIntervalMillis);
                }
            }
        }
    }
}