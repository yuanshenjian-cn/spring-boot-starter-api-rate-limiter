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

    // 使用令牌桶算法
    @TokenBucketRateLimiter(
        key = "'api:user:' + #id",         // 限流键，支持 SpEL 表达式
        capacity = 5,                      // 桶容量
        refillRate = 1,                    // 每秒填充1个令牌
        message = "访问频率过高，请稍后再试"
    )
    @GetMapping("/api/user/{id}")
    public String getUser(@PathVariable String id) {
        return "User: " + id;
    }

    // 使用固定窗口算法
    @FixedWindowRateLimiter(
        key = "'api:order:' + #orderId",   // 限流键
        limit = 10,                        // 限制次数
        windowSize = 60,                   // 时间窗口（秒）
        message = "访问频率过高，请稍后再试"
    )
    @PostMapping("/api/order/{orderId}")
    public String createOrder(@PathVariable String orderId) {
        return "Order created: " + orderId;
    }

    // 使用漏桶算法
    @LeakyBucketRateLimiter(
        key = "'api:upload:' + #userId",   // 限流键
        capacity = 5,                      // 桶容量
        leakRate = 2,                      // 每秒处理2个请求
        message = "访问频率过高，请稍后再试"
    )
    @PostMapping("/api/upload")
    public String uploadFile() {
        return "File uploaded successfully";
    }
}
```

### 5. IDE 配置提示

本项目已包含配置元数据，添加依赖后 IDE 应该会自动提供 `rate-limiter` 配置项的自动补全提示。

## 配置选项

### 全局配置

```yaml
rate-limiter:
  enabled: true                           # 是否启用限流，默认 true
  default-limit: 10                       # 默认限制次数
  default-window-size: 60                 # 默认时间窗口（秒）
  default-message: "访问频率过高，请稍后再试"  # 默认限流消息
  rate-limit-http-status: 429             # 限流异常的HTTP状态码
  redis-timeout: 2000                     # Redis连接超时时间（毫秒）
  max-key-length: 255                     # 最大限流键长度，防止恶意长键攻击
```

### 令牌桶注解参数

- `key`: 限流键，支持 SpEL 表达式
- `capacity`: 桶容量（最大令牌数）
- `refillRate`: 填充速率（每秒填充的令牌数）
- `permits`: 每个请求所需的许可数量
- `message`: 超过限流时返回的消息

### 固定窗口注解参数

- `key`: 限流键，支持 SpEL 表达式
- `limit`: 时间窗口内允许的最大请求数
- `windowSize`: 时间窗口大小（单位：秒）
- `permits`: 每个请求所需的许可数量
- `message`: 超过限流时返回的消息

### 漏桶注解参数

- `key`: 限流键，支持 SpEL 表达式
- `capacity`: 桶容量（最大请求数）
- `leakRate`: 泄漏速率（每秒处理请求数）
- `permits`: 每个请求所需的许可数量
- `message`: 超过限流时返回的消息

## 限流算法详解

### 1. 固定窗口计数器 (Fixed Window Counter)

固定窗口计数器是最简单的限流算法，它将时间划分为固定大小的时间窗口，在每个窗口内限制请求的数量。

**工作原理**：
- 将时间轴划分为固定大小的窗口（例如：每分钟一个窗口）
- 每个窗口内维护一个计数器
- 每次请求到来时，计数器加1
- 如果计数器超过阈值，则拒绝请求
- 窗口结束时，计数器重置为0

**举例说明**：
假设设置每分钟最多10个请求：
- 12:00:00-12:00:59 这个窗口内，前10个请求可以通过，第11个及以后的请求被拒绝
- 12:01:00-12:01:59 这个窗口内，计数器重置，又是前10个请求可以通过

**优点**：实现简单，易于理解
**缺点**：在窗口边界处可能出现突发流量（例如在12:00:59来了10个请求，在12:01:01又来了10个请求，相当于2秒内处理了20个请求）

### 2. 令牌桶 (Token Bucket)

令牌桶算法以固定的速率向桶中添加令牌，请求需要消耗令牌才能执行。

**工作原理**：
- 系统以固定速率向桶中添加令牌（例如：每秒添加1个令牌）
- 桶有一个最大容量，满了之后新令牌会被丢弃
- 每次请求需要从桶中取出一个或多个令牌
- 如果桶中令牌不足，则请求被拒绝
- 如果桶中有足够的令牌，则请求被允许，并从桶中扣除相应数量的令牌

**举例说明**：
假设桶容量为5，每秒添加1个令牌：
- 初始时桶中有5个令牌
- 第1秒：来了1个请求，消耗1个令牌，桶中剩余4个
- 第2秒：来了3个请求，消耗3个令牌，桶中剩余1个
- 第3秒：来了2个请求，但桶中只有1个令牌，第1个请求通过（消耗1个令牌），第2个请求被拒绝
- 第4秒：添加1个令牌（桶中变为1个），来了1个请求，通过

**优点**：可以应对一定程度的突发流量（桶中有积压的令牌可以被突发请求使用）
**缺点**：需要维护桶的状态

### 3. 漏桶 (Leaky Bucket)

漏桶算法将请求放入桶中，然后以固定速率从桶中"漏出"请求进行处理。

**工作原理**：
- 请求到达时，先放入桶中
- 桶以固定速率处理请求（"漏水"）
- 如果桶满了，则新来的请求被拒绝
- 请求按顺序被处理，保证了平滑的输出速率

**举例说明**：
假设桶容量为5，每秒处理1个请求：
- 第1秒：来了3个请求，全部放入桶中（桶中3个请求）
- 第2秒：来了2个请求，全部放入桶中（桶中5个请求），同时处理1个请求
- 第3秒：来了2个请求，但桶已满，只能放入1个（桶中5个请求），另1个被拒绝，同时处理1个请求
- 如此循环，保证每秒最多处理1个请求

**优点**：能够平滑处理请求，输出速率稳定
**缺点**：无法处理突发流量，可能导致请求排队延迟

## 注意事项

- 本项目只支持 Redis 存储模式，不支持本地内存模式。
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