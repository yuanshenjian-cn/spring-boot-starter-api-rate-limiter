package cn.springboot.starter.ratelimiter.core.storage.script;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 滑动窗口日志脚本工厂实现
 *
 * @author Yuan Shenjian
 */
public class SlidingWindowLogScriptFactory implements RateLimitScriptFactory {

    @Override
    public RedisScript<Long> createRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getSlidingWindowLogScript());
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 获取滑动窗口日志脚本内容
     *
     * @return 脚本内容
     */
    private static String getSlidingWindowLogScript() {
        return """
            -- 滑动窗口日志限流脚本
            -- KEYS[1] = 限流器的键
            -- ARGV[1] = 窗口内允许的最大请求数（限制）
            -- ARGV[2] = 窗口大小（秒）
            -- ARGV[3] = 需要获取的许可数

            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window_size = tonumber(ARGV[2])
            local permits = tonumber(ARGV[3])

            -- 获取当前时间戳
            local current_time = tonumber(redis.call('TIME')[1])
            local window_start = current_time - window_size

            -- 获取当前窗口内的请求时间戳列表
            local timestamps = redis.call('ZRANGEBYSCORE', key, window_start, current_time)

            -- 计算当前窗口内的请求数量
            local current_count = 0
            if timestamps then
                current_count = #timestamps
            end

            -- 检查是否超过限制
            if current_count + permits <= limit then
                -- 添加当前请求的时间戳到有序集合中
                redis.call('ZADD', key, current_time, current_time .. ':' .. math.random(1000000))

                -- 设置键的过期时间，防止无限增长
                redis.call('EXPIRE', key, window_size * 2)

                return 1  -- 请求允许
            else
                -- 不添加时间戳，但仍然设置过期时间
                redis.call('EXPIRE', key, window_size * 2)

                return 0  -- 请求拒绝
            end
            """;
    }
}