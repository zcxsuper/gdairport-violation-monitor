# Logback 配置文件解读

## 📋 配置文件结构

### 1. 基础配置（第7-13行）

```xml
<configuration>
    <!-- 引入 Spring Boot 默认配置 -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- 定义日志文件路径 -->
    <property name="LOG_FILE" value="${LOG_FILE:-logs/gdairport-violation-monitor.log}"/>
    <property name="LOG_PATH" value="${LOG_PATH:-logs}"/>
</configuration>
```

**说明**：
- `include`：引入 Spring Boot 的默认日志配置（包含默认的 pattern、颜色等）
- `LOG_FILE`：当前日志文件路径，可通过环境变量 `LOG_FILE` 覆盖，默认 `logs/gdairport-violation-monitor.log`
- `LOG_PATH`：日志文件存储目录，可通过环境变量 `LOG_PATH` 覆盖，默认 `logs`

---

### 2. 控制台输出 Appender（第15-21行）

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        <charset>UTF-8</charset>
    </encoder>
</appender>
```

**作用**：将日志输出到控制台（标准输出）

**Pattern 格式说明**：
- `%d{yyyy-MM-dd HH:mm:ss.SSS}`：日期时间（精确到毫秒）
- `[%thread]`：线程名
- `%-5level`：日志级别（左对齐，5个字符宽度）：DEBUG, INFO, WARN, ERROR
- `%logger{50}`：Logger 名称（最多50个字符）
- `%msg`：日志消息
- `%n`：换行符

**示例输出**：
```
2024-01-15 14:30:25.123 [http-nio-8080-exec-1] INFO  com.example.controller.UserController - 用户登录成功
```

---

### 3. 文件输出 Appender（第23-43行）

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_FILE}</file>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        <charset>UTF-8</charset>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG_PATH}/gdairport-violation-monitor.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>10MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>1GB</totalSizeCap>
        <cleanHistoryOnStart>true</cleanHistoryOnStart>
    </rollingPolicy>
</appender>
```

**作用**：将日志写入文件，并支持自动滚动（按大小和时间）

**滚动策略说明**：

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `fileNamePattern` | 滚动后的日志文件命名规则 | `logs/gdairport-violation-monitor.2024-01-15.0.log` |
| `maxFileSize` | 单个日志文件最大大小 | 10MB（超过后创建新文件） |
| `maxHistory` | 保留最近N天的日志文件 | 30天（超过30天的自动删除） |
| `totalSizeCap` | 所有日志文件总大小限制 | 1GB（超过后删除最旧的） |
| `cleanHistoryOnStart` | 启动时清理过期日志 | true（启动时自动清理） |

**文件命名规则**：
- `%d{yyyy-MM-dd}`：按日期滚动（每天一个文件）
- `%i`：同一天内，如果文件超过 10MB，会创建新文件，序号从 0 开始递增

**示例文件**：
```
logs/
  ├── gdairport-violation-monitor.log                    # 当前正在写入的日志
  ├── gdairport-violation-monitor.2024-01-15.0.log      # 2024-01-15 的第一个文件
  ├── gdairport-violation-monitor.2024-01-15.1.log      # 2024-01-15 的第二个文件（如果第一个超过10MB）
  ├── gdairport-violation-monitor.2024-01-14.0.log      # 2024-01-14 的日志
  └── ...
```

---

### 4. 异步日志 Appender（第45-50行）

```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="FILE"/>
</appender>
```

**作用**：异步写入日志，提高性能（生产环境推荐）

**配置说明**：
- `queueSize`：异步队列大小（512条日志）
- `discardingThreshold`：队列满时的丢弃阈值（0表示不丢弃，等待）
- `appender-ref`：引用 FILE appender（实际写入文件）

**性能优势**：
- 日志写入不阻塞主线程
- 适合高并发场景
- 生产环境推荐使用

---

### 5. 环境配置（第52-74行）

#### 开发环境（dev profile）

```xml
<springProfile name="dev">
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
    <logger name="com.example" level="DEBUG"/>
</springProfile>
```

**配置**：
- 根日志级别：INFO
- 输出位置：控制台 + 文件
- `com.example` 包：DEBUG 级别（更详细的日志）

#### 生产环境（prod profile）

```xml
<springProfile name="prod">
    <root level="WARN">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
    </root>
    <logger name="com.example" level="INFO"/>
</springProfile>
```

**配置**：
- 根日志级别：WARN（只记录警告和错误）
- 输出位置：控制台 + 异步文件
- `com.example` 包：INFO 级别

#### 默认配置（无 profile）

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</root>
```

**配置**：
- 根日志级别：INFO
- 输出位置：控制台 + 文件

---

## 🚀 使用方法

### 1. 激活配置

配置文件已命名为 `logback-spring.xml`，Spring Boot 会自动加载。

### 2. 设置环境变量（可选）

```bash
# 修改日志文件路径
export LOG_FILE=/var/log/gdairport-violation-monitor/gdairport-violation-monitor.log
export LOG_PATH=/var/log/gdairport-violation-monitor

# 启动应用
java -jar app.jar
```

### 3. 切换环境

在 `application.yml` 中设置：

```yaml
spring:
  profiles:
    active: dev  # 或 prod
```

或在启动时指定：

```bash
java -jar app.jar --spring.profiles.active=prod
```

### 4. 查看日志

**控制台日志**：
```bash
# 直接查看控制台输出
```

**文件日志**：
```bash
# 查看当前日志
tail -f logs/gdairport-violation-monitor.log

# 查看历史日志
ls -lh logs/
cat logs/gdairport-violation-monitor.2024-01-15.0.log
```

---

## 📊 日志级别说明

| 级别 | 说明 | 使用场景 |
|------|------|----------|
| **TRACE** | 最详细 | 跟踪程序执行流程 |
| **DEBUG** | 调试信息 | 开发调试时使用 |
| **INFO** | 一般信息 | 正常的业务日志 |
| **WARN** | 警告信息 | 潜在问题，不影响运行 |
| **ERROR** | 错误信息 | 错误异常，需要关注 |

**日志级别继承关系**：
```
root (INFO)
  └── com.example (DEBUG in dev, INFO in prod)
      └── com.example.controller (继承父级)
          └── com.example.controller.UserController (继承父级)
```

---

## 🔧 自定义配置

### 修改日志文件路径

**方式1：环境变量**
```bash
export LOG_FILE=/custom/path/app.log
export LOG_PATH=/custom/path
```

**方式2：修改配置文件**
```xml
<property name="LOG_FILE" value="/custom/path/app.log"/>
<property name="LOG_PATH" value="/custom/path"/>
```

### 修改日志级别

**方式1：修改 logback-spring.xml**
```xml
<logger name="com.example.service" level="WARN"/>
```

**方式2：运行时指定**
```bash
java -jar app.jar --logging.level.com.example=DEBUG
```

### 添加新的 Logger

```xml
<!-- 单独配置某个类的日志级别 -->
<logger name="com.example.controller.UserController" level="DEBUG"/>

<!-- 配置第三方库的日志级别 -->
<logger name="org.springframework" level="WARN"/>
<logger name="com.baomidou.mybatisplus" level="DEBUG"/>
```

---

## ⚠️ 注意事项

1. **文件权限**：确保应用有权限在 `logs/` 目录下创建和写入文件
2. **磁盘空间**：定期检查日志文件大小，避免占满磁盘
3. **性能影响**：生产环境建议使用异步日志（ASYNC_FILE）
4. **敏感信息**：不要在日志中输出密码、token 等敏感信息

---

## 📝 配置检查清单

- [x] 控制台输出配置
- [x] 文件输出配置
- [x] 日志滚动策略（按大小和时间）
- [x] 日志保留策略（30天，1GB限制）
- [x] 环境区分（dev/prod）
- [x] 异步日志支持（生产环境）
- [x] 日志格式统一（UTF-8编码）

