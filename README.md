# SqlLens

> MyBatis / MyBatis-Plus SQL 日志、慢查询监控与异步推送 Spring Boot Starter

SqlLens 是一个轻量级的 Spring Boot Starter，自动拦截 MyBatis SQL 执行，提供以下功能：

- **SQL 日志输出** — 输出参数替换后的完整可读 SQL
- **慢查询监控** — 超过阈值的 SQL 自动 WARN 告警
- **EXPLAIN 分析** — 可选的 SELECT 语句执行计划自动分析
- **异步数据推送** — 将 SQL 执行数据异步推送到远程服务端，便于集中监控与分析

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>net.sohocn.sqllens</groupId>
    <artifactId>sqllens-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置（可选）

```yaml
# application.yml — 全部使用默认值即可，无需任何配置
sqllens:
  enabled: true              # 总开关，默认 true
  slow-threshold: 1000       # 慢查询阈值（ms），默认 1000
  print-full-sql: true       # 是否输出参数替换后的完整 SQL，默认 true
  explain-enabled: false     # 是否对 SELECT 执行 EXPLAIN，默认 false
  max-sql-length: 4096       # SQL 日志最大长度，超过截断，默认 4096
  exclude-tables:            # 排除 EXPLAIN 的表前缀
    - information_schema.
    - mysql.
    - performance_schema.
```

### 3. 完成

自动生效，无需手动注册任何 Bean。启动应用后即可在日志中看到 SQL 输出：

```
INFO  SqlLensLogger - [SqlLens][12ms] SELECT * FROM user WHERE id = 1
WARN  SqlLensLogger - [SqlLens][3520ms] SELECT * FROM orders o JOIN user u ON ...
```

## 兼容性

| 框架 | 最低版本 | 说明 |
|------|---------|------|
| MyBatis | 3.5.x | 必须 |
| MyBatis-Plus | 3.5.x | 可选，自动检测 |
| Spring Boot | 2.x / 3.x | 均支持（spring.factories + AutoConfiguration.imports） |
| Java | 8+ | — |

**纯 MyBatis 项目**：引入依赖即可，无需额外配置。

**MyBatis-Plus 项目**：自动通过 `InnerInterceptorAdapter` 桥接，无需修改现有 `MybatisPlusInterceptor` 配置。

## 异步数据推送

SqlLens 可以将每次 SQL 执行的数据异步推送到远程服务端，用于集中监控和分析。

### 配置

在项目根目录的 `.idea/sqllens.json` 文件中配置推送目标：

```json
{
  "serverUrl": "https://your-server.com/api/sql-report",
  "token": "your-auth-token"
}
```

- 如果该文件不存在或配置无效，推送功能自动禁用，不影响日志等其他功能
- 自动从当前工作目录向上查找 `.idea` 目录

### 工作原理

```
SQL 执行 → SqlLogInterceptor 拦截
             └─ SqlLensReporter（异步推送）
                  ├─ 数据入队（内存缓冲，容量 1000）
                  ├─ 守护线程每 5 秒批量 flush
                  └─ HTTP POST 发送到 serverUrl（Bearer Token 认证）
```

### 推送数据格式

每次推送的 JSON 包含以下字段：

| 字段 | 说明 |
|------|------|
| `sql` | 原始 SQL |
| `formattedSql` | 参数替换后的完整 SQL |
| `duration` | 执行耗时（ms） |
| `explainResult` | EXPLAIN 结果（可选） |
| `mapperMethod` | MyBatis Mapper 方法 ID |
| `timestamp` | 执行时间戳（ms） |

### 注意事项

- 推送为异步操作，不会影响 SQL 执行性能
- 缓冲队列满时（容量 1000），自动移除最旧的数据，新数据正常入队
- HTTP 连接超时 3 秒，读取超时 5 秒
- 应用关闭时会自动 flush 剩余数据并关闭线程池

## 工作原理

```
SQL 执行 → SqlLogInterceptor（MyBatis Interceptor 拦截）
              ├─ 记录执行耗时
              ├─ SqlLensLogger → 格式化 SQL + 输出日志
              ├─ ExplainAnalyzer → 条件执行 EXPLAIN
              └─ SqlLensReporter → 异步推送数据到远程服务端
```

- `SqlLogInterceptor` 同时实现 MyBatis 的 `Interceptor` 接口，确保在纯 MyBatis 和 MyBatis-Plus 环境中均生效
- `SqlLensReporter` 异步推送 SQL 数据到远程服务端，配置通过 `.idea/sqllens.json` 加载
- 所有异常被 catch，不会影响正常 SQL 执行
- 所有依赖标记为 `optional`，用户自行引入所需框架

## 自定义

如果需要自定义行为，可以通过 Spring Bean 覆盖任意组件：

```java
@Bean
public SqlLensLogger sqlLensLogger() {
    return new SqlLensLogger(5000, true, 8192); // 自定义慢查询阈值
}
```

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
