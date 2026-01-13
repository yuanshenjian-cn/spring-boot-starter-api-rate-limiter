package cn.springboot.starter.api_rate_limiter.core.exception;

/**
 * 限流异常类
 *
 * @author Yuan Shenjian
 */
public class RateLimitException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public RateLimitException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause 异常原因
     */
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}