package cn.springboot.starter.api_rate_limiter.core.storage.script;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 滑动窗口计数器脚本工厂实现
 *
 * @author Yuan Shenjian
 */
public class SlidingWindowCounterScriptFactory implements RateLimitScriptFactory {

    @Override
    public RedisScript<Long> createRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getSlidingWindowCounterScript());
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 获取滑动窗口计数器脚本内容
     *
     * @return 脚本内容
     */
    private static String getSlidingWindowCounterScript() {
        return """
            -- 滑动窗口计数器限流脚本
            -- KEYS[1] = 限流器的键
            -- ARGV[1] = 窗口内允许的最大请求数（限制）
            -- ARGV[2] = 窗口大小（秒）
            -- ARGV[3] = 子窗口数量
            -- ARGV[4] = 需要获取的许可数

            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window_size = tonumber(ARGV[2])
            local sub_windows = tonumber(ARGV[3])
            local permits = tonumber(ARGV[4])

            -- 计算子窗口大小
            local sub_window_size = window_size / sub_windows
            local current_time = tonumber(redis.call('TIME')[1])

            -- 计算当前子窗口索引
            local current_sub_window = math.floor(current_time / sub_window_size)

            -- 获取当前窗口内所有子窗口的计数
            local total_count = 0

            -- 遍历当前窗口内的所有子窗口
            for i = 0, sub_windows - 1 do
                local sub_window_index = current_sub_window - i
                local sub_key = key .. ':' .. sub_window_index
                local count = tonumber(redis.call('GET', sub_key)) or 0
                total_count = total_count + count
            end

            -- 检查是否超过限制
            if total_count + permits <= limit then
                -- 更新当前子窗口的计数
                local current_sub_key = key .. ':' .. current_sub_window
                redis.call('INCRBY', current_sub_key, permits)

                -- 设置子窗口的过期时间
                redis.call('EXPIRE', current_sub_key, window_size + 10)

                return 1  -- 请求允许
            else
                return 0  -- 请求拒绝
            end
            """;
    }
}