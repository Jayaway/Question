# OQ Backend

Spring Boot 问卷系统后端，提供 `/api/**` 接口。

## 技术栈

- Spring Boot
- MyBatis-Plus
- MySQL
- Spring Security

## 启动后端

```bash
./mvnw spring-boot:run
```

默认地址：`http://localhost:8080`

## 数据库

默认连接配置在 `src/main/resources/application.properties`：

- 数据库：`survey_system`
- 用户名：`root`
- 密码：`123456`

启动时会读取 `schema.sql` 和 `data.sql` 初始化示例数据。
