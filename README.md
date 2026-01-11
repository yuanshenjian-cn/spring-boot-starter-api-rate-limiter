# API Rate Limiter Spring Boot Starter

一个功能强大且易于使用的 Spring Boot Starter，用于实现 API 限流功能，支持多种限流算法和存储选项。

## 特性

- **多种限流算法**：支持固定窗口计数器、滑动窗口计数器、令牌桶等多种限流算法
- **灵活的存储选项**：支持 Redis 作为存储后端，可扩展其他存储方式
- **注解驱动**：通过简单的注解即可实现接口限流
- **AOP 支持**：基于 Spring AOP 实现，对业务代码无侵入
- **可配置**：支持通过 application.yml 进行灵活配置
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

### 2. 配置 Redis

在 `application.yml` 中配置 Redis 连接：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: # 如果有密码请填写

rate-limiter:
  enabled: true  # 启用限流功能
  default:
    limit: 10    # 默认限制次数
    window: 60   # 时间窗口（秒）
```

### 3. 在接口上使用注解

```java
@RestController
public class ApiController {

    @RateLimiter(
        key = "api:limit:user:{{#id}}",  // 限流键，支持 SpEL 表达式
        limit = 5,                       // 限制次数
        window = 60,                     // 时间窗口（秒）
        algorithm = Algorithm.FIXED_WINDOW_COUNTER  // 限流算法
    )
    @GetMapping("/api/user/{id}")
    public String getUser(@PathVariable String id) {
        return "User: " + id;
    }
}
```

## 配置选项

### 全局配置

```yaml
rate-limiter:
  enabled: true                    # 是否启用限流，默认 true
  default:
    limit: 10                      # 默认限制次数
    window: 60                     # 默认时间窗口（秒）
    algorithm: FIXED_WINDOW_COUNTER # 默认限流算法
  redis:
    script-mode: true              # 是否使用 Lua 脚本模式
```

### 注解参数

- `key`: 限流键，支持 SpEL 表达式
- `limit`: 限制次数
- `window`: 时间窗口（秒）
- `algorithm`: 限流算法（FIXED_WINDOW_COUNTER, SLIDING_WINDOW_COUNTER, TOKEN_BUCKET）
- `fallbackMethod`: 限流触发时的回调方法

## 限流算法

### 1. 固定窗口计数器 (Fixed Window Counter)

在固定的时间窗口内限制请求次数。时间窗口结束后，计数器重置。

### 2. 滑动窗口计数器 (Sliding Window Counter)

更精确的限流算法，将时间窗口划分为多个小窗口，提供更平滑的限流效果。

### 3. 令牌桶 (Token Bucket)

以固定速率向桶中添加令牌，请求需要消耗令牌才能执行。

## 存储选项

当前版本使用 Redis 作为存储后端，利用 Redis 的高性能和 Lua 脚本保证操作的原子性。

## 扩展性

项目设计具有良好的扩展性，您可以：

- 实现自定义的 `RateLimitStorage` 接口来支持其他存储方式
- 实现自定义的 `RateLimitAlgorithm` 接口来支持其他限流算法
- 通过 AOP 扩展限流逻辑

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