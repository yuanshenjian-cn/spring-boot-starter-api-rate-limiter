# 如何在其他项目中使用 api-rate-limiter-spring-boot-starter

## 1. 添加依赖

在你的项目 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
    <groupId>io.github.yuanshenjian-cn</groupId>
    <artifactId>api-rate-limiter-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 2. 配置 application.yml

### 基础配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    # password: your_password
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

rate-limiter:
  enabled: true                           # 是否启用限流，默认 true
  default-limit: 10                       # 默认限制次数
  default-window-size: 60                 # 默认时间窗口（秒）
  default-message: "访问频率过高，请稍后再试"  # 默认限流消息
```

注意：所有配置都是可选的，如果不配置，将使用内置的默认值。你也可以仅在需要时通过注解指定特定的限流规则。

## 3. 在 Controller 中使用

```java
@RestController
@RequestMapping("/api")
public class UserController {

    /**
     * 使用令牌桶算法进行限流
     * 限制每个用户每分钟最多访问5次
     */
    @RateLimiter(
        key = "'user:detail:' + #userId",  // 限流键，支持 SpEL 表达式
        limit = 5,                         // 限制次数
        windowSize = 60,                   // 时间窗口（秒）
        algorithm = RateLimiter.Algorithm.TOKEN_BUCKET   // 限流算法
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<String> getUserDetail(@PathVariable String userId) {
        return ResponseEntity.ok("User detail for: " + userId);
    }

    /**
     * 使用固定窗口算法进行限流
     * 限制每个IP地址每小时最多访问100次
     */
    @RateLimiter(
        key = "'ip:access:' + @ipExtractor.getClientIp()",  // 使用SpEL表达式调用bean
        limit = 100,
        windowSize = 3600,                 // 1小时
        algorithm = RateLimiter.Algorithm.FIXED_WINDOW
    )
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile() {
        return ResponseEntity.ok("File uploaded successfully");
    }

    /**
     * 使用漏桶算法进行限流
     * 控制接口调用速率
     */
    @RateLimiter(
        key = "'api:leaky:' + #operation",
        limit = 10,
        refillRate = 2,                    // 每秒泄漏2个请求
        algorithm = RateLimiter.Algorithm.LEAKY_BUCKET
    )
    @GetMapping("/process/{operation}")
    public ResponseEntity<String> processOperation(@PathVariable String operation) {
        return ResponseEntity.ok("Processing: " + operation);
    }
}
```

## 4. 自定义限流异常处理

```java
@ControllerAdvice
public class RateLimitExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitExceptionHandler.class);

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitException ex) {
        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "RATE_LIMIT_EXCEEDED");
        response.put("message", ex.getMessage());
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
}
```

## 5. 高级用法

### 使用 SpEL 表达式动态构建限流键

```java
// 基于用户ID和方法名进行限流
@RateLimiter(key = "'user:' + #userId + ':method:' + methodName")
public String someMethod(String userId, String methodName) {
    return "result";
}

// 基于请求头信息进行限流
@RateLimiter(key = "'api:' + #request.getHeader('X-Real-IP')")
public String apiMethod(HttpServletRequest request) {
    return "result";
}

// 基于认证用户信息进行限流
@RateLimiter(key = "'user:' + authentication.name")
public String securedMethod(Authentication authentication) {
    return "result";
}
```

### 重要注意事项

- 本项目只支持 Redis 存储模式，不再支持本地内存模式。
- 如果在 `@RateLimiter` 注解中使用限流功能，但应用程序未配置 Redis 连接，
  则该限流规则将不会生效，并会在日志中记录警告信息。为确保限流功能正常工作，请确保：
  1. 添加了 Spring Data Redis 依赖
  2. 正确配置了 Redis 连接参数

## 6. 注意事项

1. 当使用 Redis 存储时，确保 Redis 服务可用
2. 选择合适的限流算法：
   - 令牌桶：适合处理突发流量
   - 漏桶：适合控制恒定的输出速率
   - 固定窗口：简单高效，但可能存在临界问题
3. 合理设置限流参数，避免影响正常业务
4. 在生产环境中，必须配置 Redis 以确保限流功能正常工作