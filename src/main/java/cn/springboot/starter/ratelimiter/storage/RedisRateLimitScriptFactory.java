package cn.springboot.starter.ratelimiter.storage;

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
     * 获取固定窗口计数器脚本内容
     *
     * @return 脚本内容
     */
    private static String getFixedWindowCounterScript() {
        return """
            -- Fixed Window Counter Rate Limiting Script
            -- KEYS[1] = key for the rate limiter
            -- ARGV[1] = max requests allowed in the window (limit)
            -- ARGV[2] = window size in seconds
            -- ARGV[3] = number of permits to acquire

            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window_size = tonumber(ARGV[2])
            local permits = tonumber(ARGV[3])

            -- Calculate the current window start time (in seconds)
            local current_time = redis.call('TIME')
            local current_timestamp = tonumber(current_time[1])
            local window_start = math.floor(current_timestamp / window_size) * window_size

            -- Get the current count and window start time stored in Redis
            local stored_data = redis.call('GET', key)

            if stored_data then
                -- Parse the stored data (format: "count:window_start")
                local parts = {}
                for part in string.gmatch(stored_data, '[^:]+') do
                    table.insert(parts, part)
                end

                local stored_count = tonumber(parts[1])
                local stored_window_start = tonumber(parts[2])

                -- Check if we're in a new window
                if window_start > stored_window_start then
                    -- Reset the counter for the new window
                    redis.call('SET', key, permits .. ':' .. window_start)
                    -- Check if the permits fit within the limit
                    if permits <= limit then
                        return 1  -- Request allowed
                    else
                        return 0  -- Request denied
                    end
                else
                    -- Same window, increment the counter
                    local new_count = stored_count + permits
                    if new_count <= limit then
                        redis.call('SET', key, new_count .. ':' .. stored_window_start)
                        return 1  -- Request allowed
                    else
                        return 0  -- Request denied
                    end
                end
            else
                -- No data stored yet, initialize with the first request
                redis.call('SET', key, permits .. ':' .. window_start)
                -- Check if the permits fit within the limit
                if permits <= limit then
                    return 1  -- Request allowed
                else
                    return 0  -- Request denied
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
            -- Token Bucket Rate Limiting Script
            -- KEYS[1] = key for the rate limiter
            -- ARGV[1] = bucket capacity (max tokens)
            -- ARGV[2] = refill rate (tokens per second)
            -- ARGV[3] = number of permits to acquire

            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])  -- tokens per second
            local permits = tonumber(ARGV[3])

            -- Get the current bucket state (tokens, last_refill_time) from Redis
            local bucket_state = redis.call('HMGET', key, 'tokens', 'last_refill_time')

            local current_tokens, last_refill_time

            if bucket_state[1] and bucket_state[2] then
                current_tokens = tonumber(bucket_state[1])
                last_refill_time = tonumber(bucket_state[2])
            else
                -- Initialize the bucket if it doesn't exist
                current_tokens = capacity
                last_refill_time = tonumber(redis.call('TIME')[1])
                redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill_time', last_refill_time)
            end

            -- Get current time
            local current_time = tonumber(redis.call('TIME')[1])

            -- Calculate how many tokens to add based on elapsed time
            local time_elapsed = current_time - last_refill_time
            local tokens_to_add = math.floor(time_elapsed * refill_rate)

            -- Update the number of tokens, but not exceeding the capacity
            local new_tokens = math.min(capacity, current_tokens + tokens_to_add)

            -- Check if we have enough tokens for the request
            if new_tokens >= permits then
                -- Deduct the tokens and update the last refill time
                redis.call('HMSET', key, 'tokens', new_tokens - permits, 'last_refill_time', current_time)
                return 1  -- Request allowed
            else
                -- Update the last refill time even if request is denied (to prevent abuse)
                redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill_time', current_time)
                return 0  -- Request denied
            end
            """;
    }
}