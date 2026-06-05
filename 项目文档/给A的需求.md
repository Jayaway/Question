# 给 A 同学的需求清单（B 阻塞项）

> **作者**：B 同学
> **目的**：让 A 同学知道要先写哪些文件，B 才能继续往下做
> **配套参考**：`代码编写参考.md`（已推 master，所有代码都在里面）

---

A 同学你好 👋

数据库初始化、依赖配置、完整代码参考手册（`项目文档/代码编写参考.md`，2742 行）我都已经推到 master 了，你 `git pull` 就能拿到。下面我下一步要写的 B6~B11（Service + Controller）**强依赖**你那边的几个文件，**麻烦优先写这 11 个**，写完我立刻能动。

## 🔴 我被阻塞的 11 个文件

### 第 1 步：common/Result.java（最简单，5 分钟）

- 路径：`src/main/java/com/example/survey/common/Result.java`
- 内容：泛型类 `Result<T>`，3 个字段 `code` / `message` / `data`，2 个静态方法 `success(T data)` 和 `error(int code, String msg)`
- 参考手册位置：**§二.2.1（行 274-318）**

### 第 2 步：5 个 Entity（5 个文件，约 30 分钟）

| # | 文件路径 | 类名 | 表名 | 关键字段 |
|---|---------|------|------|----------|
| 1 | `entity/User.java` | `User` | `sys_user` | `id` / `username` / `password` / `email` / `createTime` |
| 2 | `entity/Survey.java` | `Survey` | `survey` | `id` / `title` / `description` / `status` / `creatorId` / `createTime` / `updateTime` |
| 3 | `entity/Question.java` | `Question` | `question` | `id` / `surveyId` / `type` / `title` / `options` / `sortOrder` / `required` |
| 4 | `entity/Response.java` | `Response` | `response` | `id` / `surveyId` / `respondentIp` / `submitTime` |
| 5 | `entity/Answer.java` | `Answer` | `answer` | `id` / `responseId` / `questionId` / `content` |

每个 Entity 都加：
- `@Data`（Lombok）
- `@TableName("表名")`（MyBatis-Plus）
- 主键字段 `@TableId(type = IdType.AUTO)`

参考手册位置：**§三.3.1-3.5（行 366-498）**

### 第 3 步：5 个 Mapper（5 个文件，10 分钟）

| # | 文件路径 | 接口定义 |
|---|---------|----------|
| 1 | `mapper/UserMapper.java` | `extends BaseMapper<User>` + 加一个 `User selectByUsername(String username)` 方法 |
| 2 | `mapper/SurveyMapper.java` | `extends BaseMapper<Survey>` |
| 3 | `mapper/QuestionMapper.java` | `extends BaseMapper<Question>` |
| 4 | `mapper/ResponseMapper.java` | `extends BaseMapper<Response>` |
| 5 | `mapper/AnswerMapper.java` | `extends BaseMapper<Answer>` |

参考手册位置：**§四（行 499-565）**

## 几个坑提前说，避免浪费时间

1. **包名用 `com.example.survey`**（不是 `com.example.oq`！OQ 只是显示名，pom 里的 groupId 也没改）
2. **Entity 字段用驼峰**（`createTime` 不是 `create_time`），`application.properties` 里我配了 `map-underscore-to-camel-case=true` 会自动映射
3. **不需要改 pom.xml**（我加好了 MyBatis-Plus / JJWT / Knife4j）
4. **不需要改 application.properties**（数据库 `survey_system` / 用户 `root` / 密码 `123456` 都配好了）
5. **Mapper 扫描二选一**：
   - 给每个 Mapper 加 `@Mapper` 注解（最直接）
   - 或在 `QqApplication.java` 上加 `@MapperScan("com.example.survey.mapper")`（少写注解）

## 🟡 之后还要写（不急，能跑起来后再说）

- `config/SecurityConfig.java`（放行 `/doc.html` 用的）
- `common/GlobalExceptionHandler.java`
- `service/impl/UserServiceImpl.java`
- `service/impl/SurveyServiceImpl.java`
- `controller/AuthController.java`
- `controller/SurveyController.java`

## 🟢 最后写

- `config/CorsConfig.java`（跨域）
- `config/MyBatisPlusConfig.java`（分页插件）
- `utils/JwtUtil.java` + `config/JwtAuthFilter.java`（JWT 工具）
- `config/Knife4jConfig.java`（API 文档）

## 时间预估

上面 11 个文件里，Result 5 分钟、5 个 Entity 30 分钟、5 个 Mapper 10 分钟。**总共不到 1 小时**。写完 `git push` 我就立刻开 B6~B11。

有不清楚的随时问，我之前把代码参考手册读得比较细。🚀
