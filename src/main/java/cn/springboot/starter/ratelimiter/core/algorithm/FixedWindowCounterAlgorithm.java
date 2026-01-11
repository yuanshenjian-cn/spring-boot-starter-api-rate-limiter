package cn.springboot.starter.ratelimiter.core.algorithm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 固定窗口计数器算法实现
 *
 * @author Yuan Shenjian
 */
public class FixedWindowCounterAlgorithm implements RateLimitAlgorithm {

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final long limit;
    private final long windowSizeInMillis;

    /**
     * 构造函数
     *
     * @param limit 限制数量
     * @param windowSizeInMillis 窗口大小（毫秒）
     */
    public FixedWindowCounterAlgorithm(long limit, long windowSizeInMillis) {
        this.limit = limit;
        this.windowSizeInMillis = windowSizeInMillis;
    }

    @Override
    public boolean isAllowed(String key, int permits) {
        Counter counter = counters.computeIfAbsent(key, k -> new Counter(limit, windowSizeInMillis));

        return counter.increment(permits);
    }

    @Override
    public String getName() {
        return "FIXED_WINDOW_COUNTER";
    }

    /**
     * 计数器内部类
     */
    private static class Counter {
        private final long limit;
        private final long windowSizeInMillis;
        private final AtomicLong count;
        private volatile long windowStart;

        /**
         * 构造函数
         *
         * @param limit 限制数量
         * @param windowSizeInMillis 窗口大小（毫秒）
         */
        public Counter(long limit, long windowSizeInMillis) {
            this.limit = limit;
            this.windowSizeInMillis = windowSizeInMillis;
            this.count = new AtomicLong(0);
            this.windowStart = getCurrentWindowStart(System.currentTimeMillis());
        }

        /**
         * 增加计数
         *
         * @param permits 许可数量
         * @return 是否允许请求
         */
        public boolean increment(int permits) {
            long currentTime = System.currentTimeMillis();
            long currentWindowStart = getCurrentWindowStart(currentTime);

            if (currentWindowStart > windowStart) {
                count.set(0);
                windowStart = currentWindowStart;
            }

            long currentCount = count.get();
            if (currentCount + permits <= limit) {
                return count.compareAndSet(currentCount, currentCount + permits);
            } else {
                return false;
            }
        }

        /**
         * 获取当前窗口开始时间
         *
         * @param currentTime 当前时间
         * @return 窗口开始时间
         */
        private long getCurrentWindowStart(long currentTime) {
            return (currentTime / windowSizeInMillis) * windowSizeInMillis;
        }
    }
}