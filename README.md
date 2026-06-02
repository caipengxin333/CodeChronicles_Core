# CodeChronicles Core

个人博客后端代码，基于 Java 21 + Spring Boot + MyBatis + Maven。

## 数据库

本地开发默认连接 MySQL 9.3：

- Host: `localhost`
- Port: `3306`
- Database: `codechronicles`
- Username: `root`
- Password env: `MYSQL_PASSWORD`
- Backend port env: `SERVER_PORT`，默认 `8080`

MySQL 密码放在项目根目录的 `.env.local` 中，当前值会通过 `scripts/run-local.sh` 加载为环境变量。`.env.local` 已加入 `.gitignore`，不会提交到仓库。
Spring Boot 也会自动导入项目根目录的 `.env.local`，所以在 IDEA 中直接运行主类时同样能读取这些本地配置。

可在 DBeaver 中执行 `docs/mysql-dbeaver-init.sql` 创建数据库、表和示例数据。

## 本地运行

```bash
./scripts/run-local.sh
```

服务默认运行在 `http://localhost:8080`，前端 `CodeChronicles_Ui` 已将 `/api` 代理到该地址。
如果 `8080` 被占用，可以临时执行 `SERVER_PORT=18080 ./scripts/run-local.sh`。

## 已提供接口

- `GET /api/profile`
- `GET /api/tags`
- `GET /api/articles?page=1&pageSize=10&tagId=1`
- `GET /api/articles/{id}`
- `POST /api/articles`
- `GET /api/questions`

响应统一为：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

开发期使用本地 MySQL。测试环境单独使用 H2，不依赖真实数据库。
