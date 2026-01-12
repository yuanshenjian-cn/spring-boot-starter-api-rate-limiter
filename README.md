# API Rate Limiter Spring Boot Starter

一个功能强大且易于使用的 Spring Boot Starter，用于实现 API 限流功能，支持多种限流算法和存储选项。

## 特性

- **多种限流算法**：支持固定窗口计数器、令牌桶、漏桶等多种限流算法
- **Redis 存储**：使用 Redis 作为存储后端，保证分布式环境下的限流一致性
- **注解驱动**：通过简单的注解即可实现接口限流
- **AOP 支持**：基于 Spring AOP 实现，对业务代码无侵入
- **可配置**：支持通过 application.yml 进行灵活配置，包含配置元数据支持 IDE 自动补全
- **高性能**：基于 Redis Lua 脚本实现，保证原子性和高性能

## 快速开始

### 1. 添加依赖

在您的 `pom.xml` 中添加以下依赖：

```xml
<dependency>
    <groupId>io.github.yuanshenjian-cn</groupId>
    <artifactId>api-rate-limiter-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置 Redis（如果使用 Redis 存储）

在 `application.yml` 中配置 Redis 连接：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: # 如果有密码请填写
    timeout: 2000ms  # 可选配置
    lettuce:         # 可选配置
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### 3. 配置限流参数（可选）

在 `application.yml` 中配置默认限流参数（如无需特殊配置，可完全省略）：

```yaml
rate-limiter:
  enabled: true                           # 是否启用限流，默认 true
  default-limit: 10                       # 默认限制次数
  default-window-size: 60                 # 默认时间窗口（秒）
  default-message: "访问频率过高，请稍后再试"  # 默认限流消息
```

注意：所有配置都是可选的，如果不配置，将使用内置的默认值。你也可以仅在需要时通过注解指定特定的限流规则。

### 4. 在接口上使用注解

```java
@RestController
public class ApiController {

    @RateLimiter(
        key = "'api:user:' + #id",         // 限流键，支持 SpEL 表达式
        limit = 5,                         // 限制次数
        windowSize = 60,                   // 时间窗口（秒）
        algorithm = RateLimiter.Algorithm.FIXED_WINDOW   // 限流算法
    )
    @GetMapping("/api/user/{id}")
    public String getUser(@PathVariable String id) {
        return "User: " + id;
    }
}
```

### 5. IDE 配置提示

本项目已包含配置元数据，添加依赖后 IDE 应该会自动提供 `rate-limiter` 配置项的自动补全提示。

## 配置选项

### 全局配置

```yaml
rate-limiter:
  enabled: true                    # 是否启用限流，默认 true
  default:
    limit: 10                      # 默认限制次数
    window: 60                     # 默认时间窗口（秒）
    algorithm: FIXED_WINDOW        # 默认限流算法
```

### 注解参数

- `key`: 限流键，支持 SpEL 表达式
- `limit`: 限制次数
- `windowSize`: 时间窗口（秒）
- `capacity`: 桶容量（用于令牌桶算法）
- `refillRate`: 填充速率（用于令牌桶）或泄漏速率（用于漏桶）
- `permits`: 每个请求所需的许可数量
- `algorithm`: 限流算法（FIXED_WINDOW, TOKEN_BUCKET, LEAKY_BUCKET）
- `message`: 超过限流时返回的消息

## 限流算法

### 1. 固定窗口计数器 (Fixed Window Counter)

在固定的时间窗口内限制请求次数。时间窗口结束后，计数器重置。

### 2. 令牌桶 (Token Bucket)

以固定速率向桶中添加令牌，请求需要消耗令牌才能执行。

### 3. 漏桶 (Leaky Bucket)

请求进入桶中，以固定速率从桶中流出，实现平滑的请求处理。

## 注意事项

- 本项目只支持 Redis 存储模式，不再支持本地内存模式。
- 如果在 `@RateLimiter` 注解中使用限流功能，但应用程序未配置 Redis 连接，
  则该限流规则将不会生效，并会在日志中记录警告信息。为确保限流功能正常工作，请确保：
  1. 添加了 Spring Data Redis 依赖
  2. 正确配置了 Redis 连接参数

## 发布到 Maven Central

本项目已发布到 Maven Central。如果您想了解如何将类似的项目发布到 Maven Central，请参阅 [发布指南](docs/publish-guides.md)。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

## 许可证

本项目采用 MIT 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

## 作者

Yuan Shenjian - [yuanshenjian-cn](https://github.com/yuanshenjian-cn)

## 致谢

感谢 Spring Boot 团队提供的优秀框架，以及所有为开源社区做出贡献的开发者们。