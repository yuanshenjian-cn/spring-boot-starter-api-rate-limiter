package cn.springboot.starter.ratelimiter.core.handler;

import cn.springboot.starter.ratelimiter.config.RateLimiterProperties;
import cn.springboot.starter.ratelimiter.core.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流异常全局处理器
 * 该类提供了一个默认的限流异常处理机制
 *
 * @author Yuan Shenjian
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rate-limiter.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitExceptionHandler {

    private final RateLimiterProperties properties;

    /**
     * 处理限流异常
     *
     * @param ex 限流异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "RATE_LIMIT_EXCEEDED");
        response.put("message", ex.getMessage());
        response.put("timestamp", Instant.now());
        response.put("status", properties.getRateLimitHttpStatus());

        HttpStatus status = HttpStatus.valueOf(properties.getRateLimitHttpStatus());
        return ResponseEntity.status(status).body(response);
    }
}