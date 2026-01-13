package cn.springboot.starter.ratelimiter.core.storage.script;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 令牌桶脚本工厂实现（增强版）
 * 支持更灵活的时间单位配置，如每分钟、每小时、每天等
 *
 * @author Yuan Shenjian
 */
public class TokenBucketScriptFactory implements RateLimitScriptFactory {

    @Override
    public RedisScript<Long> createRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(getEnhancedTokenBucketScript());
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 获取增强版令牌桶脚本内容
     *
     * @return 脚本内容
     */
    private static String getEnhancedTokenBucketScript() {
        return """
            -- 增强版令牌桶限流脚本
            -- KEYS[1] = 限流器的键
            -- ARGV[1] = 桶容量（最大令牌数）
            -- ARGV[2] = 填充数量（在指定时间单位内填充的令牌数）
            -- ARGV[3] = 填充间隔（秒）
            -- ARGV[4] = 需要获取的许可数

            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_amount = tonumber(ARGV[2])  -- 在指定时间单位内填充的令牌数
            local refill_interval = tonumber(ARGV[3])  -- 填充间隔（秒）
            local permits = tonumber(ARGV[4])

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

            -- 计算距离上次填充经过了多少个完整的填充周期
            local time_since_last_refill = current_time - last_refill_time
            local elapsed_intervals = math.floor(time_since_last_refill / refill_interval)

            -- 计算应该添加的令牌数（按完整周期计算）
            local tokens_to_add = elapsed_intervals * refill_amount

            -- 更新令牌数，但不超过容量
            local new_tokens = math.min(capacity, current_tokens + tokens_to_add)

            -- 计算下次填充时间（基于上次填充时间）
            local next_refill_time = last_refill_time + (elapsed_intervals + 1) * refill_interval

            -- 检查是否有足够的令牌用于请求
            if new_tokens >= permits then
                -- 扣除令牌并更新下次填充时间
                redis.call('HMSET', key, 'tokens', new_tokens - permits, 'last_refill_time', current_time)
                return 1  -- 请求允许
            else
                -- 即使请求被拒绝也要更新时间（防止滥用）
                redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill_time', current_time)
                return 0  -- 请求拒绝
            end
            """;
    }
}