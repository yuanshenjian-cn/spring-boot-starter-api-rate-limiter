package cn.springboot.starter.ratelimiter.core.storage.script;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 固定窗口计数器脚本工厂实现
 *
 * @author Yuan Shenjian
 */
public class FixedWindowCounterScriptFactory implements RateLimitScriptFactory {

    @Override
    public RedisScript<Long> createRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getFixedWindowCounterScript());
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
}