# OQ 问卷系统

项目已拆分为后端和前端两个部分：

- 后端：Spring Boot，位于项目根目录，提供 `/api/**` 接口。
- 前端：Vite，位于 `frontend/`，通过代理访问后端接口。

## 启动后端

```bash
./mvnw spring-boot:run
```

默认地址：`http://localhost:8080`

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

开发环境中，Vite 会把 `/api` 请求代理到 `http://localhost:8080`。
