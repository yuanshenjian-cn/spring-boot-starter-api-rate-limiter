package cn.springboot.starter.ratelimiter.core.algorithm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 漏桶算法实现
 *
 * @author Yuan Shenjian
 */
public class LeakyBucketAlgorithm implements RateLimitAlgorithm {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long leakRate;

    /**
     * 构造函数
     *
     * @param capacity 桶容量
     * @param leakRate 泄露率
     */
    public LeakyBucketAlgorithm(long capacity, long leakRate) {
        this.capacity = capacity;
        this.leakRate = leakRate;
    }

    @Override
    public boolean isAllowed(String key, int permits) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, leakRate));

        return bucket.tryAdd(permits);
    }

    @Override
    public String getName() {
        return "LEAKY_BUCKET";
    }

    /**
     * 桶内部类
     */
    private static class Bucket {
        private final long capacity;
        private final long leakRate;
        private final AtomicLong currentSize;
        private volatile long lastUpdateTime;

        /**
         * 构造函数
         *
         * @param capacity 桶容量
         * @param leakRate 泄露率
         */
        public Bucket(long capacity, long leakRate) {
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.currentSize = new AtomicLong(0);
            this.lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * 尝试添加请求
         *
         * @param permits 许可数量
         * @return 是否允许请求
         */
        public boolean tryAdd(int permits) {
            leak();

            long currentSize = this.currentSize.get();

            if (currentSize + permits <= capacity) {
                this.currentSize.addAndGet(permits);
                return true;
            } else {
                return false;
            }
        }

        /**
         * 泄露处理
         */
        private void leak() {
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - lastUpdateTime;

            long leakedRequests = (timeElapsed * leakRate) / 1000;

            if (leakedRequests > 0) {
                long currentSize = this.currentSize.get();
                long newCurrentSize = Math.max(0, currentSize - leakedRequests);

                if (this.currentSize.compareAndSet(currentSize, newCurrentSize)) {
                    lastUpdateTime = currentTime;
                }
            }
        }
    }
}