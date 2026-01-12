package cn.springboot.starter.ratelimiter.core;

import cn.springboot.starter.ratelimiter.config.RateLimiterProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * 限流切面抽象基类
 * 该类提供限流切面的通用功能，包括键生成等
 *
 * @author Yuan Shenjian
 */
@Slf4j
public abstract class AbstractRateLimiterAspect {

    protected final ExpressionParser parser = new SpelExpressionParser();
    protected final StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();

    protected final StringRedisTemplate redisTemplate;
    protected final RateLimiterProperties properties;

    /**
     * 构造函数
     *
     * @param redisTemplate 用于基于 Redis 的限流的 Redis 模板（可以为 null）
     * @param properties 限流器配置属性
     * @param metricsCollector 指标收集器（可以为 null）
     */
    public AbstractRateLimiterAspect(StringRedisTemplate redisTemplate,
                                    RateLimiterProperties properties,
                                    Object metricsCollector) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 基于方法和参数生成限流键
     *
     * @param method       被调用的方法
     * @param args         方法参数
     * @param keyTemplate 注解中的键模板
     * @return 生成的限流键
     */
    protected String generateKey(Method method, Object[] args, String keyTemplate) {
        String key;
        if (keyTemplate != null && !keyTemplate.isEmpty()) {
            EvaluationContext context = new MethodBasedEvaluationContext(
                    null, method, args, discoverer);
            key = parser.parseExpression(keyTemplate).getValue(context, String.class);
        } else {
            key = method.getDeclaringClass().getName() + ":" + method.getName();
        }

        // 验证键长度，防止恶意长键攻击
        if (key.length() > properties.getMaxKeyLength()) {
            log.warn("限流键长度超过最大限制: {} > {}, 截断键值", key.length(), properties.getMaxKeyLength());
            key = key.substring(0, properties.getMaxKeyLength());
        }

        return key;
    }

    /**
     * 获取方法签名
     *
     * @param point ProceedingJoinPoint
     * @return Method对象
     */
    protected Method getMethod(ProceedingJoinPoint point) {
        return ((MethodSignature) point.getSignature()).getMethod();
    }
}