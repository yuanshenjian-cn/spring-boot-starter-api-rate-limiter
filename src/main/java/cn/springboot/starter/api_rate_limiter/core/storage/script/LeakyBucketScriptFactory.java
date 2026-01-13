package cn.springboot.starter.api_rate_limiter.core.storage.script;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 漏桶脚本工厂实现
 *
 * @author Yuan Shenjian
 */
public class LeakyBucketScriptFactory implements RateLimitScriptFactory {

    @Override
    public RedisScript<Long> createRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getLeakyBucketScript());
        script.setResultType(Long.class);
        return script;
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

            -- 检查键的类型，如果不是hash类型则删除它
            local key_type = redis.call('TYPE', key)['ok']
            if key_type ~= 'hash' then
                redis.call('DEL', key)
            end

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