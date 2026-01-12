package cn.springboot.starter.ratelimiter.core.storage;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis限流脚本工厂类
 *
 * @author Yuan Shenjian
 */
public class RedisRateLimitScriptFactory {

    /**
     * 创建固定窗口计数器脚本
     *
     * @return Redis脚本对象
     */
    public static RedisScript<Long> createFixedWindowCounterScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getFixedWindowCounterScript());
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 创建令牌桶脚本
     *
     * @return Redis脚本对象
     */
    public static RedisScript<Long> createTokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getTokenBucketScript());
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 创建漏桶脚本
     *
     * @return Redis脚本对象
     */
    public static RedisScript<Long> createLeakyBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getLeakyBucketScript());
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 获取固定窗口计数器脚本内容
     *
     * @return 脚本内容
     */
    private static String getFixedWindowCounterScript() {
        return """
            -- 固定窗口计数器限流脚本
            -- KEYS[1] = 限流器的键
            -- ARGV[1] = 窗口内允许的最大请求数（限制）
            -- ARGV[2] = 窗口大小（秒）
            -- ARGV[3] = 需要获取的许可数

            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window_size = tonumber(ARGV[2])
            local permits = tonumber(ARGV[3])

            -- 计算当前窗口开始时间（秒）
            local current_time = redis.call('TIME')
            local current_timestamp = tonumber(current_time[1])
            local window_start = math.floor(current_timestamp / window_size) * window_size

            -- 获取存储在Redis中的当前计数和窗口开始时间
            local stored_data = redis.call('GET', key)

            if stored_data then
                -- 解析存储的数据（格式："count:window_start"）
                local parts = {}
                for part in string.gmatch(stored_data, '[^:]+') do
                    table.insert(parts, part)
                end

                local stored_count = tonumber(parts[1])
                local stored_window_start = tonumber(parts[2])

                -- 检查是否是新窗口
                if window_start > stored_window_start then
                    -- 为新窗口重置计数器
                    redis.call('SET', key, permits .. ':' .. window_start)
                    -- 检查许可是否在限制范围内
                    if permits <= limit then
                        return 1  -- 请求允许
                    else
                        return 0  -- 请求拒绝
                    end
                else
                    -- 同一窗口，增加计数
                    local new_count = stored_count + permits
                    if new_count <= limit then
                        redis.call('SET', key, new_count .. ':' .. stored_window_start)
                        return 1  -- 请求允许
                    else
                        return 0  -- 请求拒绝
                    end
                end
            else
                -- 尚未存储数据，使用第一个请求初始化
                redis.call('SET', key, permits .. ':' .. window_start)
                -- 检查许可是否在限制范围内
                if permits <= limit then
                    return 1  -- 请求允许
                else
                    return 0  -- 请求拒绝
                end
            end
            """;
    }

    /**
     * 获取令牌桶脚本内容
     *
     * @return 脚本内容
     */
    private static String getTokenBucketScript() {
        return """
            -- 令牌桶限流脚本
            -- KEYS[1] = 限流器的键
            -- ARGV[1] = 桶容量（最大令牌数）
            -- ARGV[2] = 填充速率（每秒令牌数）
            -- ARGV[3] = 需要获取的许可数

            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])  -- 每秒令牌数
            local permits = tonumber(ARGV[3])

            -- 从Redis获取当前桶状态（令牌数，上次填充时间）
            local bucket_state = redis.call('HMGET', key, 'tokens', 'last_refill_time')

            local current_tokens, last_refill_time

            if bucket_state[1] and bucket_state[2] then
                current_tokens = tonumber(bucket_state[1])
                last_refill_time = tonumber(bucket_state[2])
            else
                -- 如果桶不存在则初始化
                current_tokens = capacity
                last_refill_time = tonumber(redis.call('TIME')[1])
                redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill_time', last_refill_time)
            end

            -- 获取当前时间
            local current_time = tonumber(redis.call('TIME')[1])

            -- 根据经过的时间计算要添加的令牌数
            local time_elapsed = current_time - last_refill_time
            local tokens_to_add = math.floor(time_elapsed * refill_rate)

            -- 更新令牌数，但不超过容量
            local new_tokens = math.min(capacity, current_tokens + tokens_to_add)

            -- 检查是否有足够的令牌用于请求
            if new_tokens >= permits then
                -- 扣除令牌并更新上次填充时间
                redis.call('HMSET', key, 'tokens', new_tokens - permits, 'last_refill_time', current_time)
                return 1  -- 请求允许
            else
                -- 即使请求被拒绝也要更新上次填充时间（防止滥用）
                redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill_time', current_time)
                return 0  -- 请求拒绝
            end
            """;
    }

    /**
     * 获取漏桶脚本内容
     *
     * @return 脚本内容
     */
    private static String getLeakyBucketScript() {
        return """
            -- 漏桶限流脚本
            -- KEYS[1] = 限流器的键
            -- ARGV[1] = 桶容量（最大请求数）
            -- ARGV[2] = 泄漏速率（每秒处理请求数）
            -- ARGV[3] = 需要获取的许可数

            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local leak_rate = tonumber(ARGV[2])  -- 每秒处理请求数
            local permits = tonumber(ARGV[3])

            -- 从Redis获取当前桶状态（当前请求数，上次处理时间）
            local bucket_state = redis.call('HMGET', key, 'current_size', 'last_process_time')

            local current_size, last_process_time

            if bucket_state[1] and bucket_state[2] then
                current_size = tonumber(bucket_state[1])
                last_process_time = tonumber(bucket_state[2])
            else
                -- 如果桶不存在则初始化
                current_size = 0
                last_process_time = tonumber(redis.call('TIME')[1])
                redis.call('HMSET', key, 'current_size', current_size, 'last_process_time', last_process_time)
            end

            -- 获取当前时间
            local current_time = tonumber(redis.call('TIME')[1])

            -- 计算经过的时间
            local time_elapsed = current_time - last_process_time

            -- 根据经过的时间计算要处理的请求数
            local leaked_requests = math.floor(time_elapsed * leak_rate)

            -- 更新桶中的请求数（减去已处理的请求）
            local new_current_size = math.max(0, current_size - leaked_requests)

            -- 检查添加新请求后是否会超过容量
            if new_current_size + permits <= capacity then
                -- 添加新请求到桶中
                redis.call('HMSET', key, 'current_size', new_current_size + permits, 'last_process_time', current_time)
                return 1  -- 请求允许
            else
                -- 桶已满，拒绝请求，但仍更新处理时间
                redis.call('HMSET', key, 'current_size', new_current_size, 'last_process_time', current_time)
                return 0  -- 请求拒绝
            end
            """;
    }
}