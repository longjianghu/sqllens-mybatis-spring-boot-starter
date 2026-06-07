# 项目说明

<div align="center">

[中文](#中文) | [English](#english) | [日本語](#日本語) | [한국어](#한국어)

</div>

---

## 中文

> MyBatis / MyBatis-Plus SQL 日志、慢查询监控与异步推送 Spring Boot Starter

SqlLens 是一个轻量级的 Spring Boot Starter，自动拦截 MyBatis SQL 执行，提供以下功能：

- **SQL 日志输出** — 输出参数替换后的完整可读 SQL
- **慢查询监控** — 超过阈值的 SQL 自动 WARN 告警
- **EXPLAIN 分析** — 可选的 SELECT 语句执行计划自动分析
- **异步数据推送** — 将 SQL 执行数据异步推送到远程服务端，便于集中监控与分析

### 快速开始

#### 1. 引入依赖

```xml
<dependency>
    <groupId>net.sohocn.sqllens</groupId>
    <artifactId>sqllens-mybatis-spring-boot-starter</artifactId>
  <version>1.1.2</version>
</dependency>
```

#### 2. 配置（可选）

```yaml
# application.yml — 全部使用默认值即可，无需任何配置
sqllens:
  enabled: false             # 总开关，默认 false，需显式开启
  slow-threshold: 1000       # 慢查询阈值（ms），默认 1000
  print-full-sql: true       # 是否输出参数替换后的完整 SQL，默认 true
  explain-enabled: false     # 是否对 SELECT 执行 EXPLAIN，默认 false
  explain-analyze: false     # 是否使用 EXPLAIN ANALYZE（真正执行 SQL），默认 false
  max-sql-length: 0          # SQL 日志最大长度（字符），默认 0 不限制
  exclude-tables:            # 排除 EXPLAIN 的表前缀
    - information_schema.
    - mysql.
    - performance_schema.
```

#### 3. 完成

自动生效，无需手动注册任何 Bean。启动应用后即可在日志中看到 SQL 输出：

```
INFO  SqlLensLogger - [SqlLens][12ms] SELECT * FROM user WHERE id = 1
WARN  SqlLensLogger - [SqlLens][3520ms] SELECT * FROM orders o JOIN user u ON ...
```

### 兼容性

| 框架 | 最低版本 | 说明 |
|------|---------|------|
| MyBatis | 3.5.x | 必须 |
| MyBatis-Plus | 3.5.x | 可选，自动检测 |
| Spring Boot | 2.x / 3.x | 均支持（spring.factories + AutoConfiguration.imports） |
| Java | 8+ | — |

**纯 MyBatis 项目**：引入依赖即可，无需额外配置。

**MyBatis-Plus 项目**：自动通过 `InnerInterceptorAdapter` 桥接，无需修改现有 `MybatisPlusInterceptor` 配置。

### 异步数据推送

SqlLens 可以将每次 SQL 执行的数据异步推送到远程服务端，用于集中监控和分析。

#### 配置

在项目根目录的 `.idea/sqllens.json` 文件中配置推送目标：

```json
{
  "serverUrl": "https://httpbin.org/post",
  "token": "your-auth-token",
  "reportIntervalSeconds": 1
}
```

- `reportIntervalSeconds`：上报间隔（秒），默认 1
- 如果该文件不存在或配置无效，推送功能自动禁用，不影响日志等其他功能
- 自动从当前工作目录向上查找 `.idea` 目录

#### 工作原理

```
SQL 执行 → SqlLogInterceptor 拦截
             └─ SqlLensReporter（异步推送）
                  ├─ 数据入队（内存缓冲，容量 1000）
                  ├─ 守护线程定期批量 flush（默认每 1 秒，可通过 reportIntervalSeconds 配置）
                  └─ HTTP POST 发送到 serverUrl（Bearer Token 认证）
```

#### 推送数据格式

每次推送为 JSON 数组，包含以下字段：

```json
[
  {
    "sql": "原始 SQL",
    "formattedSql": "参数替换后的完整 SQL",
    "duration": 12,
    "explainResult": "EXPLAIN 结果（可选）",
    "mapperMethod": "com.example.UserMapper.selectById",
    "timestamp": 1716470400000
  }
]
```

| 字段 | 说明 |
|------|------|
| `sql` | 原始 SQL |
| `formattedSql` | 参数替换后的完整 SQL |
| `duration` | 执行耗时（ms） |
| `explainResult` | EXPLAIN 结果（可选） |
| `mapperMethod` | MyBatis Mapper 方法 ID |
| `timestamp` | 执行时间戳（ms） |

#### 注意事项

- 推送为异步操作，不会影响 SQL 执行性能
- 缓冲队列满时（容量 1000），自动移除最旧的数据，新数据正常入队
- 每次 flush 将队列中所有数据批量发送为一个 JSON 数组，减少 HTTP 请求次数
- 发送失败时数据自动放回队列重试，连续失败 3 次后丢弃数据避免堆积
- HTTP 连接超时 3 秒，读取超时 1 秒
- 应用关闭时会自动 flush 剩余数据并关闭线程池

### 工作原理

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

### 自定义

如果需要自定义行为，可以通过 Spring Bean 覆盖任意组件：

```java
@Bean
public SqlLensLogger sqlLensLogger() {
  return new SqlLensLogger(1000, true, 8192); // 自定义慢查询阈值
}
```

### License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

## English

> MyBatis / MyBatis-Plus SQL logging, slow query monitoring & async push Spring Boot Starter

SqlLens is a lightweight Spring Boot Starter that automatically intercepts MyBatis SQL execution, providing:

- **SQL Log Output** — Outputs complete readable SQL after parameter substitution
- **Slow Query Monitoring** — Automatically warns on SQL exceeding the threshold
- **EXPLAIN Analysis** — Optional automatic execution plan analysis for SELECT statements
- **Async Data Push** — Asynchronously pushes SQL execution data to a remote server for centralized monitoring

### Quick Start

#### 1. Add Dependency

```xml

<dependency>
    <groupId>net.sohocn.sqllens</groupId>
    <artifactId>sqllens-mybatis-spring-boot-starter</artifactId>
  <version>1.1.2</version>
</dependency>
```

#### 2. Configuration (Optional)

```yaml
# application.yml — all properties have defaults, no configuration required
sqllens:
  enabled: false             # Master switch, default false, must be explicitly enabled
  slow-threshold: 1000       # Slow query threshold (ms), default 1000
  print-full-sql: true       # Output complete SQL with parameter substitution, default true
  explain-enabled: false     # Run EXPLAIN on SELECT statements, default false
  explain-analyze: false     # Use EXPLAIN ANALYZE (actually executes SQL), default false
  max-sql-length: 0          # Max SQL log length (chars), 0 means no limit
  exclude-tables: # Table prefixes to exclude from EXPLAIN
    - information_schema.
    - mysql.
    - performance_schema.
```

#### 3. Done

It works automatically — no beans to register manually. After starting the application, you'll see SQL output in the
logs:

```
INFO  SqlLensLogger - [SqlLens][12ms] SELECT * FROM user WHERE id = 1
WARN  SqlLensLogger - [SqlLens][3520ms] SELECT * FROM orders o JOIN user u ON ...
```

### Compatibility

| Framework    | Min Version | Notes                                                         |
|--------------|-------------|---------------------------------------------------------------|
| MyBatis      | 3.5.x       | Required                                                      |
| MyBatis-Plus | 3.5.x       | Optional, auto-detected                                       |
| Spring Boot  | 2.x / 3.x   | Both supported (spring.factories + AutoConfiguration.imports) |
| Java         | 8+          | —                                                             |

**Pure MyBatis projects**: Just add the dependency — no extra configuration needed.

**MyBatis-Plus projects**: Automatically bridges via `InnerInterceptorAdapter` — no changes to existing
`MybatisPlusInterceptor` configuration required.

### Async Data Push

SqlLens can asynchronously push SQL execution data to a remote server for centralized monitoring and analysis.

#### Configuration

Configure the push target in the `.idea/sqllens.json` file at the project root:

```json
{
  "serverUrl": "https://httpbin.org/post",
  "token": "your-auth-token",
  "reportIntervalSeconds": 1
}
```

- `reportIntervalSeconds`: Report interval in seconds, default 1
- If the file does not exist or the configuration is invalid, push is automatically disabled without affecting other
  features
- Automatically searches upward from the current working directory for the `.idea` directory

#### How It Works

```
SQL Execution → SqlLogInterceptor intercepts
                  └─ SqlLensReporter (async push)
                       ├─ Data enqueued (in-memory buffer, capacity 1000)
                       ├─ Daemon thread periodically flushes (default every 1s, configurable via reportIntervalSeconds)
                       └─ HTTP POST to serverUrl (Bearer Token auth)
```

#### Push Data Format

Each push is a JSON array with the following fields:

```json
[
  {
    "sql": "raw SQL",
    "formattedSql": "complete SQL with parameter substitution",
    "duration": 12,
    "explainResult": "EXPLAIN result (optional)",
    "mapperMethod": "com.example.UserMapper.selectById",
    "timestamp": 1716470400000
  }
]
```

| Field           | Description                               |
|-----------------|-------------------------------------------|
| `sql`           | Raw SQL                                   |
| `formattedSql`  | Complete SQL after parameter substitution |
| `duration`      | Execution time (ms)                       |
| `explainResult` | EXPLAIN result (optional)                 |
| `mapperMethod`  | MyBatis Mapper method ID                  |
| `timestamp`     | Execution timestamp (ms)                  |

#### Notes

- Push is asynchronous and does not affect SQL execution performance
- When the buffer is full (capacity 1000), the oldest data is automatically removed; new data is enqueued normally
- Each flush sends all buffered data as a single JSON array to reduce HTTP requests
- On send failure, data is returned to the queue for retry; data is discarded after 3 consecutive failures to avoid
  buildup
- HTTP connection timeout: 3s, read timeout: 1s
- On application shutdown, remaining data is automatically flushed and the thread pool is shut down

### Architecture

```
SQL Execution → SqlLogInterceptor (MyBatis Interceptor)
                   ├─ Records execution time
                   ├─ SqlLensLogger → formats SQL + outputs logs
                   ├─ ExplainAnalyzer → conditionally runs EXPLAIN
                   └─ SqlLensReporter → async push to remote server
```

- `SqlLogInterceptor` implements MyBatis's `Interceptor` interface, working in both pure MyBatis and MyBatis-Plus
  environments
- `SqlLensReporter` asynchronously pushes SQL data to a remote server, configured via `.idea/sqllens.json`
- All exceptions are caught and do not affect normal SQL execution
- All dependencies are marked `optional` — users include the frameworks they need

### Customization

Override any component via Spring Bean:

```java

@Bean
public SqlLensLogger sqlLensLogger() {
  return new SqlLensLogger(1000, true, 8192); // Custom slow query threshold
}
```

### License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

## 日本語

> MyBatis / MyBatis-Plus SQL ログ、スロークエリ監視、非同期プッシュ Spring Boot Starter

SqlLens は軽量な Spring Boot Starter であり、MyBatis の SQL 実行を自動的にインターセプトし、以下の機能を提供します：

- **SQL ログ出力** — パラメータ置換後の完全な可読 SQL を出力
- **スロークエリ監視** — 閾値を超えた SQL を自動的に WARN 警告
- **EXPLAIN 分析** — SELECT 文の実行計画をオプションで自動分析
- **非同期データプッシュ** — SQL 実行データを非同期でリモートサーバーにプッシュし、集中監視・分析を実現

### クイックスタート

#### 1. 依存関係の追加

```xml

<dependency>
    <groupId>net.sohocn.sqllens</groupId>
    <artifactId>sqllens-mybatis-spring-boot-starter</artifactId>
  <version>1.1.2</version>
</dependency>
```

#### 2. 設定（オプション）

```yaml
# application.yml — すべてデフォルト値で動作、設定は不要
sqllens:
  enabled: false             # マスタースイッチ、デフォルト false、明示的な有効化が必要
  slow-threshold: 1000       # スロークエリ閾値（ms）、デフォルト 1000
  print-full-sql: true       # パラメータ置換後の完全な SQL を出力、デフォルト true
  explain-enabled: false     # SELECT に EXPLAIN を実行するか、デフォルト false
  explain-analyze: false     # EXPLAIN ANALYZE を使用するか（SQL を実際に実行）、デフォルト false
  max-sql-length: 0          # SQL ログの最大長（文字数）、0 は無制限
  exclude-tables: # EXPLAIN から除外するテーブルプレフィックス
    - information_schema.
    - mysql.
    - performance_schema.
```

#### 3. 完了

自動的に動作します — Bean を手動で登録する必要はありません。アプリケーションを起動すると、ログに SQL 出力が表示されます：

```
INFO  SqlLensLogger - [SqlLens][12ms] SELECT * FROM user WHERE id = 1
WARN  SqlLensLogger - [SqlLens][3520ms] SELECT * FROM orders o JOIN user u ON ...
```

### 互換性

| フレームワーク      | 最低バージョン   | 備考                                                |
|--------------|-----------|---------------------------------------------------|
| MyBatis      | 3.5.x     | 必須                                                |
| MyBatis-Plus | 3.5.x     | オプション、自動検出                                        |
| Spring Boot  | 2.x / 3.x | 両対応（spring.factories + AutoConfiguration.imports） |
| Java         | 8+        | —                                                 |

**Pure MyBatis プロジェクト**：依存関係を追加するだけで完了、追加設定は不要です。

**MyBatis-Plus プロジェクト**：`InnerInterceptorAdapter` を介して自動的にブリッジされ、既存の `MybatisPlusInterceptor`
設定を変更する必要はありません。

### 非同期データプッシュ

SqlLens は SQL 実行データを非同期でリモートサーバーにプッシュし、集中監視と分析を実現します。

#### 設定

プロジェクトルートの `.idea/sqllens.json` ファイルでプッシュ先を設定します：

```json
{
  "serverUrl": "https://httpbin.org/post",
  "token": "your-auth-token",
  "reportIntervalSeconds": 1
}
```

- `reportIntervalSeconds`：レポート間隔（秒）、デフォルト 1
- ファイルが存在しないか設定が無効な場合、プッシュ機能は自動的に無効になり、ログなどの他の機能に影響しません
- カレントワーキングディレクトリから `.idea` ディレクトリを自動的に上方検索します

#### 動作原理

```
SQL 実行 → SqlLogInterceptor がインターセプト
             └─ SqlLensReporter（非同期プッシュ）
                  ├─ データをキューに追加（メモリバッファ、容量 1000）
                  ├─ デーモンスレッドが定期的に一括 flush（デフォルト 1 秒ごと、reportIntervalSeconds で設定可能）
                  └─ HTTP POST で serverUrl に送信（Bearer Token 認証）
```

#### プッシュデータ形式

各プッシュは JSON 配列で、以下のフィールドを含みます：

```json
[
  {
    "sql": "元の SQL",
    "formattedSql": "パラメータ置換後の完全な SQL",
    "duration": 12,
    "explainResult": "EXPLAIN 結果（オプション）",
    "mapperMethod": "com.example.UserMapper.selectById",
    "timestamp": 1716470400000
  }
]
```

| フィールド           | 説明                     |
|-----------------|------------------------|
| `sql`           | 元の SQL                 |
| `formattedSql`  | パラメータ置換後の完全な SQL       |
| `duration`      | 実行時間（ms）               |
| `explainResult` | EXPLAIN 結果（オプション）      |
| `mapperMethod`  | MyBatis Mapper メソッド ID |
| `timestamp`     | 実行タイムスタンプ（ms）          |

#### 注意事項

- プッシュは非同期操作であり、SQL 実行パフォーマンスに影響しません
- バッファキューが満杯の場合（容量 1000）、最も古いデータが自動的に削除され、新しいデータは正常にキューに追加されます
- 各 flush でキュー内の全データを 1 つの JSON 配列として一括送信し、HTTP リクエスト数を削減します
- 送信失敗時はデータがキューに戻されてリトライされ、3 回連続失敗後にデータは破棄されます
- HTTP 接続タイムアウト 3 秒、読み取りタイムアウト 1 秒
- アプリケーション終了時に残ったデータは自動的に flush され、スレッドプールがシャットダウンされます

### アーキテクチャ

```
SQL 実行 → SqlLogInterceptor（MyBatis Interceptor）
              ├─ 実行時間を記録
              ├─ SqlLensLogger → SQL をフォーマット + ログ出力
              ├─ ExplainAnalyzer → 条件に応じて EXPLAIN を実行
              └─ SqlLensReporter → リモートサーバーに非同期プッシュ
```

- `SqlLogInterceptor` は MyBatis の `Interceptor` インターフェースを実装し、Pure MyBatis と MyBatis-Plus の両環境で動作します
- `SqlLensReporter` は SQL データを非同期でリモートサーバーにプッシュし、設定は `.idea/sqllens.json` から読み込みます
- すべての例外は catch され、通常の SQL 実行に影響しません
- すべての依存関係は `optional` としてマークされており、ユーザーが必要なフレームワークのみを導入します

### カスタマイズ

Spring Bean を介して任意のコンポーネントをオーバーライドできます：

```java

@Bean
public SqlLensLogger sqlLensLogger() {
  return new SqlLensLogger(1000, true, 8192); // カスタムスロークエリ閾値
}
```

### License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

## 한국어

> MyBatis / MyBatis-Plus SQL 로그, 느린 쿼리 모니터링 및 비동기 푸시 Spring Boot Starter

SqlLens는 경량 Spring Boot Starter로, MyBatis SQL 실행을 자동으로 가로채서 다음 기능을 제공합니다:

- **SQL 로그 출력** — 매개변수 치환 후 완전한 가독성 있는 SQL 출력
- **느린 쿼리 모니터링** — 임계값을 초과하는 SQL에 자동 WARN 경고
- **EXPLAIN 분석** — SELECT 문에 대한 선택적 실행 계획 자동 분석
- **비동기 데이터 푸시** — SQL 실행 데이터를 비동기적으로 원격 서버로 푸시하여 중앙 집중식 모니터링 및 분석

### 빠른 시작

#### 1. 의존성 추가

```xml

<dependency>
    <groupId>net.sohocn.sqllens</groupId>
    <artifactId>sqllens-mybatis-spring-boot-starter</artifactId>
  <version>1.1.2</version>
</dependency>
```

#### 2. 설정 (선택 사항)

```yaml
# application.yml — 모두 기본값 사용 가능, 별도 설정 불필요
sqllens:
  enabled: false             # 전체 스위치, 기본 false, 명시적 활성화 필요
  slow-threshold: 1000       # 느린 쿼리 임계값 (ms), 기본 1000
  print-full-sql: true       # 매개변수 치환 후 완전한 SQL 출력 여부, 기본 true
  explain-enabled: false     # SELECT에 EXPLAIN 실행 여부, 기본 false
  explain-analyze: false     # EXPLAIN ANALYZE 사용 여부 (SQL 실제 실행), 기본 false
  max-sql-length: 0          # SQL 로그 최대 길이 (문자), 0은 제한 없음
  exclude-tables: # EXPLAIN에서 제외할 테이블 접두사
    - information_schema.
    - mysql.
    - performance_schema.
```

#### 3. 완료

자동으로 작동합니다 — Bean을 수동으로 등록할 필요가 없습니다. 애플리케이션을 시작하면 로그에서 SQL 출력을 확인할 수 있습니다:

```
INFO  SqlLensLogger - [SqlLens][12ms] SELECT * FROM user WHERE id = 1
WARN  SqlLensLogger - [SqlLens][3520ms] SELECT * FROM orders o JOIN user u ON ...
```

### 호환성

| 프레임워크        | 최소 버전     | 설명                                                   |
|--------------|-----------|------------------------------------------------------|
| MyBatis      | 3.5.x     | 필수                                                   |
| MyBatis-Plus | 3.5.x     | 선택 사항, 자동 감지                                         |
| Spring Boot  | 2.x / 3.x | 모두 지원 (spring.factories + AutoConfiguration.imports) |
| Java         | 8+        | —                                                    |

**순수 MyBatis 프로젝트**: 의존성만 추가하면 되며, 추가 설정이 필요하지 않습니다.

**MyBatis-Plus 프로젝트**: `InnerInterceptorAdapter`를 통해 자동으로 브리징되며, 기존 `MybatisPlusInterceptor` 설정을 변경할 필요가 없습니다.

### 비동기 데이터 푸시

SqlLens는 각 SQL 실행 데이터를 비동기적으로 원격 서버로 푸시하여 중앙 집중식 모니터링 및 분석을 할 수 있습니다.

#### 설정

프로젝트 루트의 `.idea/sqllens.json` 파일에서 푸시 대상을 구성합니다:

```json
{
  "serverUrl": "https://httpbin.org/post",
  "token": "your-auth-token",
  "reportIntervalSeconds": 1
}
```

- `reportIntervalSeconds`: 보고 간격 (초), 기본 1
- 파일이 없거나 설정이 유효하지 않은 경우 푸시 기능이 자동으로 비활성화되며, 로그 등 다른 기능에 영향을 미치지 않습니다
- 현재 작업 디렉토리에서 상위로 `.idea` 디렉토리를 자동으로 검색합니다

#### 작동 원리

```
SQL 실행 → SqlLogInterceptor 가로챔
             └─ SqlLensReporter (비동기 푸시)
                  ├─ 데이터 큐에 추가 (메모리 버퍼, 용량 1000)
                  ├─ 데몬 스레드가 주기적으로 일괄 flush (기본 1초마다, reportIntervalSeconds로 설정 가능)
                  └─ HTTP POST로 serverUrl에 전송 (Bearer Token 인증)
```

#### 푸시 데이터 형식

각 푸시는 JSON 배열이며, 다음 필드를 포함합니다:

```json
[
  {
    "sql": "원본 SQL",
    "formattedSql": "매개변수 치환 후 완전한 SQL",
    "duration": 12,
    "explainResult": "EXPLAIN 결과 (선택 사항)",
    "mapperMethod": "com.example.UserMapper.selectById",
    "timestamp": 1716470400000
  }
]
```

| 필드              | 설명                    |
|-----------------|-----------------------|
| `sql`           | 원본 SQL                |
| `formattedSql`  | 매개변수 치환 후 완전한 SQL     |
| `duration`      | 실행 시간 (ms)            |
| `explainResult` | EXPLAIN 결과 (선택 사항)    |
| `mapperMethod`  | MyBatis Mapper 메서드 ID |
| `timestamp`     | 실행 타임스탬프 (ms)         |

#### 주의 사항

- 푸시는 비동기 작업으로, SQL 실행 성능에 영향을 미치지 않습니다
- 버퍼 큐가 가득 차면 (용량 1000), 가장 오래된 데이터가 자동으로 제거되고 새 데이터는 정상적으로 큐에 추가됩니다
- 각 flush는 큐의 모든 데이터를 단일 JSON 배열로 일괄 전송하여 HTTP 요청 횟수를 줄입니다
- 전송 실패 시 데이터가 큐로 반환되어 재시도되며, 3회 연속 실패 후 데이터가 폐기되어 누적을 방지합니다
- HTTP 연결 타임아웃 3초, 읽기 타임아웃 1초
- 애플리케이션 종료 시 남은 데이터가 자동으로 flush되고 스레드 풀이 종료됩니다

### 아키텍처

```
SQL 실행 → SqlLogInterceptor (MyBatis Interceptor)
              ├─ 실행 시간 기록
              ├─ SqlLensLogger → SQL 포맷팅 + 로그 출력
              ├─ ExplainAnalyzer → 조건부 EXPLAIN 실행
              └─ SqlLensReporter → 원격 서버로 비동기 푸시
```

- `SqlLogInterceptor`는 MyBatis의 `Interceptor` 인터페이스를 구현하여 순수 MyBatis 및 MyBatis-Plus 환경 모두에서 작동합니다
- `SqlLensReporter`는 SQL 데이터를 비동기적으로 원격 서버로 푸시하며, 설정은 `.idea/sqllens.json`에서 로드됩니다
- 모든 예외는 catch되어 정상적인 SQL 실행에 영향을 미치지 않습니다
- 모든 의존성은 `optional`로 표시되어 있으며, 사용자가 필요한 프레임워크만 도입합니다

### 사용자 정의

Spring Bean을 통해 모든 컴포넌트를 재정의할 수 있습니다:

```java

@Bean
public SqlLensLogger sqlLensLogger() {
  return new SqlLensLogger(1000, true, 8192); // 사용자 정의 느린 쿼리 임계값
}
```

### License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
