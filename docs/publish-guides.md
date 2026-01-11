# 发布到 Maven Central 指南

本指南将帮助您将此 Spring Boot Starter 发布到 Maven Central 仓库。

## 1. 准备工作

### 1.1 注册 Sonatype Central Portal 账户
- 访问 [Sonatype Central Portal](https://central.sonatype.com/)
- 注册账户（如果还没有）
- 登录后进入 "My Profile" 页面

### 1.2 申请 Namespace 权限
- 在 Central Portal 中，进入 "Upload" -> "Namespaces" 页面
- 点击 "Request New Namespace" 按钮
- 填写以下信息：
  - Namespace: `io.github.yuanshenjian-cn`
  - Project URL: https://github.com/yuanshenjian-cn/api-rate-limiter-spring-boot-starter
  - SCM URL: https://github.com/yuanshenjian-cn/api-rate-limiter-spring-boot-starter.git
  - Description: API Rate Limiter Spring Boot Starter
- 提交申请并等待审核（通常很快）

### 1.3 安装 GPG 并生成密钥对
```bash
# 安装 GPG (macOS)
brew install gpg

# 生成新的密钥对
gpg --gen-key

# 列出密钥
gpg --list-keys

# 将公钥上传到密钥服务器（推荐使用 keys.openpgp.org）
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID

# 或者尝试其他服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver pgp.mit.edu --send-keys YOUR_KEY_ID
```

## 2. GPG 签名的重要性

GPG（GNU Privacy Guard）签名是发布到 Maven Central 的强制要求，它提供了：

- **完整性验证**：确保发布的构件在传输过程中没有被篡改
- **身份验证**：证明构件确实是由您发布的，而不是冒名顶替者
- **信任保证**：为使用者提供安全保证

## 3. GPG 密钥查找机制

Maven GPG 插件的工作原理：

- Maven GPG 插件调用系统的 `gpg` 命令行工具
- `gpg` 会在默认密钥环 (`~/.gnupg/`) 中查找可用的私钥
- 如果只有一对密钥，会自动使用该密钥
- 如果有多个密钥，可通过配置指定特定密钥
- 认证由 GPG agent 处理，负责密码输入和缓存

## 4. PGP 密钥服务器

Maven Central 会从以下 PGP 密钥服务器查找公钥：

- `keys.openpgp.org` (推荐，现代且可靠)
- `keyserver.ubuntu.com` (可能不稳定)
- `pgp.mit.edu` (备用选项)

当您生成新的 GPG 密钥后，必须将公钥上传到至少一个服务器，以便 Maven Central 能够验证您的签名。

## 5. 配置 Maven 设置

在 `~/.m2/settings.xml` 中添加以下配置：

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_SONATYPE_USERNAME</username>
      <password>YOUR_SONATYPE_PASSWORD</password>
    </server>
  </servers>
</settings>
```

### 5.1 获取 Sonatype 认证信息

1. 登录 [Sonatype Central Portal](https://central.sonatype.com/)
2. 进入 "My Profile" 页面
3. 在 "User Token" 部分点击 "Access User Token"
4. 点击 "Generate" 按钮生成新的用户令牌
5. 将生成的用户名和密码分别填入 settings.xml 中的 username 和 password 字段

**注意**：Sonatype 现在使用 User Token 而不是传统的用户名/密码组合。您需要生成一个用户令牌并使用该令牌进行认证。

### 5.2 创建 settings.xml 文件

如果 `~/.m2/settings.xml` 文件不存在，请创建它：

```bash
mkdir -p ~/.m2
touch ~/.m2/settings.xml
```

然后将上述配置添加到文件中。

## 6. 发布流程

### 6.1 验证构建
```bash
# 清理并重新构建项目
./mvnw clean verify

# 确保生成了所有必需的构件（JAR、Sources、Javadoc）
```

### 6.2 执行发布
```bash
# 设置 GPG_TTY 环境变量，然后发布到 Sonatype Central
export GPG_TTY=$(tty)
./mvnw clean deploy -P release
```

或者使用以下命令跳过测试：
```bash
export GPG_TTY=$(tty)
./mvnw clean deploy -P release -DskipTests
```

也可以使用一行命令：
```bash
GPG_TTY=$(tty) ./mvnw clean deploy -P release -DskipTests
```

## 7. 在 Sonatype Central Portal 中完成发布

1. 登录 [Sonatype Central Portal](https://central.sonatype.com/)
2. 进入 "Upload" -> "Components" 页面
3. 查找刚刚上传的组件
4. 点击 "Publish" 按钮完成发布

## 8. 版本管理

- 每次发布前，请确保更新版本号
- 对于正式发布，使用不含 `-SNAPSHOT` 后缀的版本号
- 对于快照版本，保留 `-SNAPSHOT` 后缀

## 9. 故障排除

### 9.1 GPG 签名错误
- 确保 GPG 已正确安装
- 确保私钥可用
- 确保 GPG agent 正在运行

### 9.2 GPG_TTY 错误
如果遇到 "gpg: signing failed: Inappropriate ioctl for device" 错误：
- 这通常是因为 GPG 无法在当前终端环境中获取密码
- 解决方法：运行 `export GPG_TTY=$(tty)` 然后再执行部署命令
- 或者使用 `GPG_TTY=$(tty) ./mvnw clean deploy` 一行命令

### 9.3 密钥服务器错误
- 如果 `keyserver.ubuntu.com` 失败，尝试 `keys.openpgp.org`
- 确保公钥已成功上传到至少一个 PGP 服务器
- 上传后等待几分钟让服务器同步

### 9.4 认证失败
- 确认用户名和密码正确
- 确认账户有发布权限

### 9.5 构件验证失败
- 确保包含 sources 和 javadoc
- 确保所有元数据完整
- 确保所有构件都已签名

## 10. 安全最佳实践

### 10.1 GPG 密钥安全
- 为您的 GPG 密钥设置一个 strong passphrase
- 定期更新密钥（如本项目中设置的3年有效期）
- 妥善保管私钥，不要分享给他人

### 10.2 密码管理
- 不要在配置文件中硬编码任何密码
- 使用环境变量或 Maven 加密功能管理敏感信息

## 11. 注意事项

- 一旦发布到 Maven Central，无法删除或修改已发布的版本
- 确保版本号唯一且有意义
- 测试发布流程时可先使用测试版本号
- 发布后大约需要 10-30 分钟才能在 Maven Central 搜索到

## 12. 验证发布

发布完成后，可以在以下位置验证：
- Maven Central: https://search.maven.org/
- 搜索 `io.github.yuanshenjian-cn:api-rate-limiter-spring-boot-starter`

## 13. 重要提示

由于安全原因，我们已从 pom.xml 中移除了硬编码的 GPG passphrase。请通过以下方式之一提供 GPG 密码：

1. 使用环境变量：
```bash
export MAVEN_GPG_PASSPHRASE="your-passphrase-here"
./mvnw clean deploy
```

2. 使用 GPG agent：
```bash
# 启动 GPG agent
gpg-agent --daemon
# 然后运行部署命令
./mvnw clean deploy
```

3. 使用 Maven 加密密码功能：
- 参考 Maven 官方文档配置加密密码
- 在 settings.xml 中使用加密后的密码

## 14. 实际发布经验

根据实际测试，以下情况是正常的：
- 如果您的 GPG agent 已经缓存了密码，可能不需要设置 MAVEN_GPG_PASSPHRASE 环境变量
- Maven GPG 插件会自动选择默认密钥，无需在配置中指定密钥 ID
- 如果遇到"Invalid signature"错误，通常是因为公钥未上传到 PGP 服务器或服务器未同步
- 如果遇到"gpg: signing failed: Inappropriate ioctl for device"错误，需要设置GPG_TTY环境变量