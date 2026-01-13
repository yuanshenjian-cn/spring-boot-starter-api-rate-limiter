package cn.springboot.starter.ratelimiter.core.storage.script;

import org.springframework.data.redis.core.script.RedisScript;

/**
 * 限流脚本工厂接口
 * 定义了创建限流算法Redis脚本的方法
 *
 * @author Yuan Shenjian
 */
public interface RateLimitScriptFactory {

    /**
     * 创建限流脚本
     *
     * @return Redis脚本对象
     */
    RedisScript<Long> createRateLimitScript();
}