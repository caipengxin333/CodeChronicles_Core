# CodeChronicles Core

Java 21 + Spring Boot + MyBatis + Maven 的个人技术博客后端。

## 本地运行

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`，前端 `CodeChronicles_Ui` 已将 `/api` 代理到该地址。

## 已提供接口

- `GET /api/profile`
- `GET /api/tags`
- `GET /api/articles?page=1&pageSize=10&tagId=1`
- `GET /api/articles/{id}`
- `GET /api/questions`

响应统一为：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

开发期使用 H2 内存库，启动时会自动加载 `schema.sql` 和 `data.sql` 中的示例博客数据。
