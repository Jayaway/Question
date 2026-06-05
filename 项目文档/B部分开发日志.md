# B 部分开发日志 —— 基础设施搭建记录

> **项目**：在线问卷调查与数据分析系统
> **作者**：B 同学（钱宇凡）
> **本文档定位**：记录基础设施搭建全过程，**不包含主体 Java 代码**（DTO / Service / Controller 由本人独立完成）
> **用途**：项目答辩时供老师翻阅 + 个人回顾

---

## 目录

1. [初始问题与诊断](#一初始问题与诊断)
2. [环境与版本选型](#二环境与版本选型)
3. [pom.xml 改造](#三pomxml-改造)
4. [数据库连接配置](#四数据库连接配置)
5. [SQL 初始化三件套](#五sql-初始化三件套)
6. [项目文档归档](#六项目文档归档)
7. [Git 提交策略](#七git-提交策略)
8. [答辩常见提问与回答](#八答辩常见提问与回答)
9. [未完成事项清单](#九未完成事项清单)

---

## 一、初始问题与诊断

### 1.1 现象

启动 Spring Boot 应用时抛异常：

```
Failed to configure a DataSource: 'url' attribute is not specified
and no embedded datasource could be configured.

Reason: Failed to determine a suitable driver class
```

### 1.2 根因

`src/main/resources/application.properties` 只有一行：

```properties
spring.application.name=QQ
```

**Spring Boot 数据源自动配置**（`DataSourceAutoConfiguration`）需要 `spring.datasource.url` / `username` / `password` 三个核心属性才能装配 `DataSource` Bean。由于属性缺失，且 classpath 上只有 `mysql-connector-j`（**不是嵌入式数据库 H2/HSQL/Derby**），自动配置失败，整个上下文启动失败。

### 1.3 解决思路

需要 3 件事齐备：
1. 显式配置 `spring.datasource.url/username/password`
2. 启动时数据库 `survey_system` 必须存在（或 URL 携带 `createDatabaseIfNotExist=true`）
3. 5 张业务表必须存在（或在 `schema.sql` 中带 `IF NOT EXISTS` 启动建表）

---

## 二、环境与版本选型

| 组件 | 版本 | 选型理由 |
|------|------|---------|
| JDK | 17.0.19（Oracle） | 课程要求 JavaEE LTS，稳定 |
| Spring Boot | 4.0.6 | 课程框架要求 |
| MyBatis-Plus | 3.5.7 | 文档指定，相比 JPA 更灵活、CRUD 一行代码 |
| JJWT | 0.12.6 | Spring Security 配套的 JWT 工具，最新稳定版 |
| Knife4j | 4.5.0 (jakarta) | Swagger 增强版，自动生成 API 文档 |
| MySQL Connector/J | （由 Spring Boot parent 管理） | MySQL 8.0 官方驱动 |
| Lombok | （由 Spring Boot parent 管理） | 消除 getter/setter 样板 |

> **老师可能会问**："为什么 MyBatis-Plus 用 3.5.7 而不是最新版？"
> **答**：3.5.7 是 Spring Boot 3/4 兼容性最稳的版本（Spring Boot 3 用 jakarta.* 命名空间，4 沿用）。更高版本可能在 4.0.6 上有 SPI 兼容风险。

---

## 三、pom.xml 改造

### 3.1 替换了什么

**移除**：`org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1`（普通 MyBatis）
**新增**：
- `com.baomidou:mybatis-plus-spring-boot3-starter:3.5.7`
- `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6`
- `com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:4.5.0`

### 3.2 为什么用 MyBatis-Plus 而不是普通 MyBatis

文档明确要求。区别：

| 维度 | MyBatis | MyBatis-Plus |
|------|---------|--------------|
| CRUD 写法 | 写 XML 映射文件 | 继承 `BaseMapper<T>` 自动获得 |
| 条件构造 | 手写 XML `<if>` | `LambdaQueryWrapper` 链式调用 |
| 分页 | 手写 `LIMIT` | 内置 `PaginationInnerInterceptor` |
| 字段映射 | XML `<resultMap>` | `map-underscore-to-camel-case=true` 自动 |

### 3.3 Spring Boot 4.0.6 兼容性

Spring Boot 4.0.6 用 **jakarta.\*** 命名空间（不是 javax.\*），所以：
- `jakarta.validation.constraints.NotBlank` ✅
- `knife4j-openapi3-jakarta-...` ✅
- 老教程里 `javax.servlet.*` ❌（已废弃）

---

## 四、数据库连接配置

### 4.1 最终 `application.properties`

```properties
spring.application.name=OQ
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/survey_system?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.sql.init.mode=always
spring.sql.init.continue-on-error=true

mybatis-plus.configuration.map-underscore-to-camel-case=true
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl

jwt.secret=oq-survey-system-secret-key-please-change-in-prod-32chars
jwt.expiration=86400000
```

### 4.2 关键设计点

**(1) 数据库名用 `survey_system` 而非 `oq`**

虽然项目从 `QQ` 重命名为 `OQ` 的 commit（`730f07b`）存在，但**数据库名仍按文档**用 `survey_system`。理由：业务名跟项目显示名解耦，重命名不影响数据。

**(2) URL 里加 `createDatabaseIfNotExist=true`**

MySQL JDBC 参数。**数据库 `survey_system` 不存在时自动创建**，省去老师手输 `CREATE DATABASE`。这是方案一（自动建库）的关键。

**(3) `spring.sql.init.mode=always` + `continue-on-error=true`**

```yaml
spring:
  sql:
    init:
      mode: always              # 每次启动都执行 schema.sql + data.sql
      continue-on-error: true   # 已存在的表/重复插入 → 静默跳过，不报错
```

这样**第二次启动**不会因为 `sys_user` 已存在而炸。已建表的 `CREATE TABLE IF NOT EXISTS` 也会自动跳过。

**(4) `mybatis-plus.configuration.log-impl=StdOutImpl`**

控制台打印每条 SQL，**开发时调试超有用**。生产环境可去掉。

**(5) JWT 配置**

`jwt.secret` 必须 ≥ 32 字符（HS256 要求）。生产环境应该用环境变量注入而不是写死在 properties 里。

---

## 五、SQL 初始化三件套

### 5.1 三个文件的分工

| 文件 | 位置 | 何时执行 | 给谁用 |
|------|------|---------|--------|
| `schema.sql` | `src/main/resources/` | Spring Boot 启动时自动执行 | 开发 + 老师启动即用 |
| `data.sql` | `src/main/resources/` | schema.sql 之后自动执行 | 演示用测试数据 |
| `sql/init.sql` | 项目根 `sql/` 目录 | 老师手动 `mysql -u root -p < init.sql` | 老师环境特殊时的保底方案 |

### 5.2 5 张表的设计

```
sys_user ──1:N──→ survey ──1:N──→ question
                      │
                      └──1:N──→ response ──1:N──→ answer
                                          │
                                          └──N:1──→ question
```

**外键策略**：
- `survey.creator_id → sys_user.id`（**RESTRICT**，不级联——删用户不删他的问卷，留作审计）
- `question.survey_id → survey.id`（**CASCADE**，删问卷自动删题目）
- `response.survey_id → survey.id`（**CASCADE**，删问卷级联删答卷）
- `answer.response_id → response.id`（**CASCADE**）
- `answer.question_id → question.id`（**RESTRICT**，不级联——防止误删题连带历史答卷丢失）

### 5.3 测试数据的选择

`data.sql` 里塞了：

- **1 个 admin 用户**（`admin / admin123`，BCrypt 哈希）—— 登录用
- **1 份示例问卷**（"校园食堂满意度调查"，status=1 已发布）—— 答辩演示用
- **3 道题**（单选/多选/文本各一）—— 覆盖所有题型，演示统计图表

为什么用 BCrypt 哈希 `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`？
- 文档 2.4 节明确提供的 `admin123` 对应哈希
- 不可逆单向加密，数据库泄露也不暴露明文密码
- Spring Security 的 `BCryptPasswordEncoder.matches()` 可直接校验

### 5.4 `sql/init.sql` 比 `schema.sql` 多了什么

`init.sql` = `schema.sql` + `CREATE DATABASE` + 测试数据。专门给老师当**保底方案**用——如果环境不支持 `createDatabaseIfNotExist=true` 自动建库，老师可以一行命令 `mysql -u root -p < sql/init.sql` 完整初始化。

---

## 六、项目文档归档

### 6.1 补全的参考文档

新增 `项目文档/代码编写参考.md`（2742 行），包含全部 30 个 Java 文件的**完整可复制代码**：
- 同学 A 部分（18 个文件，🔴 标记）
- 同学 B 部分（12 个文件，🔵 标记）
- AI 前端部分（6 个 Vue + 3 个 JS，🟢 标记）

为什么归档：实施清单里**反复引用这个文件**但之前在项目里根本不存在。我向 A 同学补全了这个文件，避免后面开发时各人理解不一致。

### 6.2 文档阅读建议

B 部分相关章节：
- §五 后端 DTO 数据传输对象（行 565-639）
- §八 后端 Service 接口与实现（行 956-1455）
- §九 后端 Controller（行 1460-1745）

---

## 七、Git 提交策略

### 7.1 第一次 push 的内容

```bash
git commit -m "chore: 配置数据库连接 + 补全依赖 + 添加 SQL 初始化文件和参考文档"
```

**7 个文件**：
- `pom.xml`（改造）
- `application.properties`（新增配置）
- `schema.sql`（新增）
- `data.sql`（新增）
- `sql/init.sql`（新增）
- `项目文档/代码编写参考.md`（新增）
- `.gitignore`（新增 `.omc/` 规则）

**未提交**：
- `LoginDto.java`（DTO 学习中的半成品，等写完再单独 commit）
- `.omc/`（Claude Code 内部状态文件，已加入 .gitignore）

### 7.2 为什么不一次推所有

半成品代码进 git 历史**会污染**项目回溯。等一个 DTO 写完、自测通过后单独 commit：

```bash
git add src/main/java/com/example/survey/dto/LoginDto.java
git commit -m "feat(dto): 添加 LoginDto（登录注册请求体）"
```

按 `type(scope): subject` 格式（参考项目里 `chore: rename project from QQ to OQ` 的风格）。

---

## 八、答辩常见提问与回答

### Q1：项目里数据库是怎么初始化的？

**答**：用了 3 个 SQL 文件 + 2 个 Spring Boot 配置。

启动时 Spring Boot 自动执行 `schema.sql` 建表 + `data.sql` 插测试数据。`url` 里加 `createDatabaseIfNotExist=true` 让 MySQL 自动建库。`continue-on-error=true` 让重复启动不会因为表已存在报错。

老师机器如果环境特殊（比如 MySQL 版本老不支持自动建库），可以手动 `mysql -u root -p < sql/init.sql`。

### Q2：为什么用 MyBatis-Plus？

**答**：3 个理由。
1. CRUD 不用写 XML，继承 `BaseMapper<T>` 自动获得 `insert/select/update/delete`
2. 条件构造器 `LambdaQueryWrapper` 链式调用，比 MyBatis 手写 `<if>` 干净
3. 分页插件 `PaginationInnerInterceptor` 一行代码搞定

### Q3：BCrypt 哈希存密码安全吗？

**答**：安全。3 个特点：
- **单向**：不可逆，数据库泄露也不会还原明文
- **加盐**：每次哈希结果不同（`$2a$10$...` 里前 22 字符是 salt）
- **慢**：故意设计得慢，暴力破解成本高

Spring Security 的 `BCryptPasswordEncoder.matches(rawPassword, hashedFromDB)` 内部重新加盐+哈希后比对。

### Q4：`schema.sql` 和 `data.sql` 的执行顺序？

**答**：
1. Spring Boot 启动 → 创建 `DataSource`（连接池初始化）
2. 执行 `schema.sql`（建表，因为 `spring.sql.init.mode=always`）
3. 执行 `data.sql`（插入测试数据，schema 之后）
4. 启动 Tomcat

**注意**：如果开启 JPA 的 `ddl-auto=update`，`schema.sql` 可能被 JPA 抢先执行。**我们项目用 MyBatis-Plus 不带 JPA**，所以 `schema.sql` 由 Spring Boot 的 `sql.init` 机制负责，不会冲突。

### Q5：JWT 为什么用 HS256？密钥怎么管理？

**答**：
- HS256 = HMAC + SHA-256，对称加密。**签发方和验证方用同一密钥**，适合单体应用。
- 生产环境应该用 RS256（非对称，公私钥分离），可以让多个微服务验证 token 但只有认证服务能签发。
- 密钥不能写死在代码里，应该用环境变量或配置中心注入。**我们项目为了答辩方便写死在 properties**，演示时老师会问这个改进点。

### Q6：MySQL 的 `utf8mb4` 和 `utf8` 区别？

**答**：
- MySQL 早期 `utf8` 实际是 **3 字节 UTF-8**，不支持 emoji 和部分生僻字
- `utf8mb4` 才是 **真正的 4 字节 UTF-8**（mb4 = most bytes 4）
- 2026 年的项目**必须用 utf8mb4**，否则用户填个 emoji 表情就会乱码

### Q7：SQL 里的 `ON DELETE CASCADE` 是什么？

**答**：级联删除。比如 `question.survey_id` 有 `ON DELETE CASCADE` 意味着：
- 删除某问卷时，**该问卷下的所有题目自动被删**
- 优点：不用业务代码里手动删子表
- 缺点：可能误删（删一个父记录牵连很多子记录）
- 项目里**只在"问卷→题目""问卷→答卷""答卷→答案"上用了 CASCADE**，因为这些生命周期一致；"答案→题目"用 RESTRICT 是为了**防止误删题导致历史数据丢失**。

### Q8：自动建表方案有什么风险？

**答**：
- 风险 1：**生产环境不应该用**。schema 变更应该有专门的 migration 工具（Flyway/Liquibase），有版本控制和回滚。
- 风险 2：**已建表后 ALTER COLUMN 不生效**。`CREATE TABLE IF NOT EXISTS` 只在表不存在时执行；改字段类型不会自动同步。
- 我们项目**只用于答辩演示**，生产前需要用 Flyway 替换。

---

## 九、未完成事项清单

### 9.1 我（B 同学）已完成的

- [x] **B1** 数据库建表 SQL（`schema.sql` + `sql/init.sql`）
- [x] **B2** 测试数据（`data.sql` 含 1 用户 + 1 问卷 + 3 题）
- [x] pom.xml 依赖补全（MyBatis-Plus / JJWT / Knife4j）
- [x] application.properties 数据库 + JWT + MyBatis-Plus 配置
- [x] 参考文档归档（`代码编写参考.md`）
- [x] 第一次 git push（7 个文件）

### 9.2 等待同学 A 提供后才能做的

- [ ] **B3** LoginDto（纯 DTO，现在可写）
- [ ] **B4** SurveyDto
- [ ] **B5** StatsDto（含 `OptionCount` 内部类）
- [ ] **B6** QuestionServiceImpl
- [ ] **B7** ResponseServiceImpl.submit（`@Transactional` 重点）
- [ ] **B8** ResponseServiceImpl.stats（统计 SQL 聚合）
- [ ] **B9** QuestionController
- [ ] **B10** ResponseController
- [ ] **B11** StatsController

### 9.3 部署与演示（B13-B14）

- [ ] `mvn clean package -DskipTests` 打 jar
- [ ] 启动后端 `java -jar target/OQ-0.0.1-SNAPSHOT.jar`
- [ ] 录制演示视频（3 分钟：登录→建问卷→加题→发布→填写→统计）
- [ ] 答辩 PPT（重点：ER 图、@Transactional 原理、统计 SQL 逻辑）

---

## 附录：开发时间线

| 时间 | 事件 |
|------|------|
| Day 1 | 项目初始化，pom + 空 application.properties，应用启动报 DataSource 错 |
| Day 1+ | 配置 datasource、添加 MyBatis-Plus/JJWT/Knife4j 依赖 |
| Day 1+ | 写 schema.sql（5 张表）+ data.sql（admin 用户） |
| Day 1+ | 验证：MySQL 连接成功、自动建表成功 |
| Day 2 | 补全 data.sql（加测试问卷 + 3 题）、写 sql/init.sql |
| Day 2+ | 同学 A 补全 `代码编写参考.md` 文档 |
| Day 2+ | 第一次 git push 到 master（7 文件） |
| 接下来 | 写 B3~B11（DTO/Service/Controller）|
