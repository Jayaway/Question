# OQ Frontend

独立前端工程，后端 API 默认运行在 `http://localhost:8080`。

## 开发

```bash
npm install
npm run dev
```

打开 `http://localhost:5173`。开发服务器会把 `/api` 代理到后端。

## 构建

```bash
npm run build
```

如果前端部署地址和后端不在同一个域名下，可以在构建时指定：

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run build
```
