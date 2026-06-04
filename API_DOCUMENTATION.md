# CodeChronicles Core 接口文档

本文档根据 `src/main/java/com/codechronicles/core/controller` 中的 Controller 整理，供前端开发联调参考。

## 基础信息

- 服务端口：`8080`
- 基础路径：`/api`
- 本地基础地址：`http://localhost:8080/api`
- 当前接口方法：`GET`、`POST`、`PUT`、`DELETE`
- 响应格式：统一使用 `ApiResponse<T>`

## 统一响应结构

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 业务状态码，成功为 `200` |
| message | string | 响应消息，成功为 `success` |
| data | any | 具体业务数据 |

### 失败响应

当前已处理的异常：

校验失败：

```json
{
  "code": 400,
  "message": "文章标题不能为空",
  "data": null
}
```

查询资源不存在：

```json
{
  "code": 404,
  "message": "Article not found: 999",
  "data": null
}
```

说明：

- 新增文章时必填字段缺失或字段格式不合法，HTTP 状态码为 `400`
- 查询不存在的文章详情时，HTTP 状态码为 `404`
- 响应体中的 `code` 与 HTTP 状态码一致

## 接口列表

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/captcha` | GET | 获取图形验证码 |
| `/api/login` | POST | 后台登录 |
| `/api/profile` | GET | 获取个人资料 |
| `/api/tags` | GET | 获取标签列表 |
| `/api/articles` | GET | 获取文章分页列表 |
| `/api/articles` | POST | 新增文章，默认提交审核 |
| `/api/articles/drafts` | POST | 保存草稿 |
| `/api/articles/{id}` | GET | 获取文章详情 |
| `/api/articles/{id}` | PUT | 修改文章 |
| `/api/articles/{id}` | DELETE | 删除文章，当前为软删除 |
| `/api/articles/{id}/submit` | POST | 草稿提交审核 |
| `/api/my/articles` | GET | 获取我的文章列表 |
| `/api/admin/articles` | GET | 管理员获取全部文章列表 |
| `/api/admin/articles/{id}/review` | POST | 管理员审核文章 |
| `/api/questions` | GET | 获取问答列表 |

## 1. 获取图形验证码

### 请求

```http
GET /api/captcha
```

### 请求参数

无。

### 功能说明

- 后端生成 4 位字母图形验证码。
- 验证码文本存入 Redis，key 格式为 `cc:captcha:{captchaKey}`。
- Redis 验证码过期时间为 2 分钟。
- 接口返回 `captchaKey` 和 base64 图片，前端登录时需要把 `captchaKey` 和用户输入的 `captcha` 一起提交。

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "msg": "success",
  "data": {
    "captchaKey": "31b61b45-51f7-4938-a32c-4ea4ed8e9b42",
    "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ..."
  }
}
```

### data 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| captchaKey | string | 验证码唯一标识，登录时原样传回 |
| image | string | base64 图片 data URL，可直接赋给 `img.src` |

## 2. 后台登录

### 请求

```http
POST /api/login
Content-Type: application/json
```

### 请求体

```json
{
  "phone": "13800138000",
  "password": "Aa123456",
  "captchaKey": "31b61b45-51f7-4938-a32c-4ea4ed8e9b42",
  "captcha": "abcd"
}
```

### 请求字段说明

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| phone | string | 是 | 手机号，必须为 11 位数字 |
| password | string | 是 | 用户输入的原始密码，必须包含大写字母、小写字母和数字 |
| captchaKey | string | 是 | `/api/captcha` 返回的验证码标识 |
| captcha | string | 是 | 用户输入的验证码文本，后端忽略大小写比较 |

### 功能说明

- 后端从 Redis 读取 `cc:captcha:{captchaKey}`。
- 验证码不存在时返回 `验证码已过期`。
- 验证码比较时会把前端输入和 Redis 中的验证码都转成大写，因此忽略大小写。
- 验证码校验后会删除 Redis 中的验证码，避免重复使用。
- 密码使用 `BCryptPasswordEncoder.matches(原始密码, 数据库 BCrypt 密文)` 校验。
- 登录成功后生成 JWT，并把用户上下文写入 Redis，key 格式为 `cc:login:token:{token}`，过期时间与 JWT 一致，当前为 2 小时。

### 响应示例

```json
{
  "code": 200,
  "message": "登录成功",
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

### 400 响应示例

```json
{
  "code": 400,
  "message": "验证码错误",
  "msg": "验证码错误",
  "data": null
}
```

## 3. 获取个人资料

### 请求

```http
GET /api/profile
```

### 请求参数

无。

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "nickname": "CodeChronicles",
    "name": "CodeChronicles",
    "account": "code_chronicles",
    "avatar": "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=300&q=80",
    "bio": "记录 Spring Boot、Vue、工程实践与日常踩坑，把复杂问题写清楚。",
    "role": "全栈开发者",
    "location": "Shanghai",
    "followers": 1280,
    "articleCount": 5,
    "articles": 5,
    "tagCount": 8,
    "questionCount": 3,
    "skills": ["Java", "Spring Boot", "MyBatis", "Vue", "Element Plus", "MySQL"],
    "links": [
      {
        "label": "GitHub",
        "url": "https://github.com/example"
      },
      {
        "label": "Email",
        "url": "mailto:hello@example.com"
      }
    ]
  }
}
```

### data 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| nickname | string | 昵称 |
| name | string | 名称，当前与 `nickname` 相同 |
| account | string | 账号 |
| avatar | string | 头像地址 |
| bio | string | 个人简介 |
| role | string | 角色 |
| location | string | 所在地 |
| followers | number | 关注者数量 |
| articleCount | number | 文章数量 |
| articles | number | 文章数量，当前与 `articleCount` 相同 |
| tagCount | number | 标签数量 |
| questionCount | number | 问答数量 |
| skills | string[] | 技能列表 |
| links | LinkResponse[] | 外部链接列表 |

### LinkResponse

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| label | string | 链接名称 |
| url | string | 链接地址 |

## 4. 获取标签列表

### 请求

```http
GET /api/tags
```

### 请求参数

无。

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "Vue",
      "articleCount": 2
    },
    {
      "id": 2,
      "name": "Spring Boot",
      "articleCount": 4
    }
  ]
}
```

### data[] 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | number | 标签 ID |
| name | string | 标签名称 |
| articleCount | number | 该标签下的文章数量 |

## 5. 获取文章分页列表

### 请求

```http
GET /api/articles
```

### 请求参数

| 参数 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | number | 否 | 1 | 页码，小于 1 时后端按 1 处理 |
| pageSize | number | 否 | 10 | 每页数量，最小 1，最大 50 |
| tagId | number | 否 | 无 | 标签 ID，用于按标签筛选 |

### 请求示例

```http
GET /api/articles?page=1&pageSize=10
GET /api/articles?page=1&pageSize=10&tagId=2
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "list": [
      {
        "id": 1,
        "title": "从零搭建 Spring Boot + Vue 的个人博客系统",
        "summary": "拆解前后端分层、接口约定、登录鉴权和文章发布流程，适合作为项目起点。",
        "cover": "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=900&q=80",
        "category": "项目实战",
        "content": "这篇文章整理个人技术博客的第一版后端模型，包括 Profile、Tag、Article 和 Question 四类核心资源。",
        "tags": ["Vue", "Spring Boot", "MyBatis"],
        "tagNames": ["Vue", "Spring Boot", "MyBatis"],
        "publishedAt": "2026-05-28",
        "updatedAt": "2026-05-28",
        "date": "2026-05-28",
        "views": 2680,
        "likes": 126,
        "comments": 34
      }
    ]
  }
}
```

### data 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| total | number | 符合条件的文章总数 |
| list | ArticleResponse[] | 当前页文章列表 |

### ArticleResponse

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | number | 文章 ID |
| title | string | 文章标题 |
| summary | string | 文章摘要 |
| cover | string | 封面图片地址 |
| category | string | 分类 |
| content | string | 文章内容 |
| status | string | 文章状态：`DRAFT` 草稿，`PENDING_REVIEW` 待审核，`PUBLISHED` 已发布，`REJECTED` 已拒绝 |
| authorUserId | number | 作者用户 ID |
| reviewTime | string/null | 审核时间 |
| reviewerUserId | number/null | 审核人用户 ID |
| rejectReason | string/null | 审核拒绝原因 |
| tags | string[] | 标签名称列表 |
| tagNames | string[] | 标签名称列表，当前与 `tags` 相同 |
| publishedAt | string | 发布时间，格式 `yyyy-MM-dd` |
| updatedAt | string | 更新时间，格式 `yyyy-MM-dd` |
| date | string | 展示日期，当前与 `publishedAt` 相同 |
| views | number | 浏览量 |
| likes | number | 点赞数 |
| comments | number | 评论数 |

## 6. 新增文章，提交审核

### 请求

```http
POST /api/articles
Content-Type: application/json
Authorization: Bearer {token}
```

### 请求体

```json
{
  "title": "Spring Boot 新增文章接口实践",
  "summary": "记录文章发布接口的参数校验、默认字段生成和 MyBatis 入库流程。",
  "cover": "https://example.com/cover.jpg",
  "category": "后端开发",
  "content": "这里是文章正文内容。",
  "tagIds": [1, 2],
  "tagNames": ["Spring Boot", "MyBatis"]
}
```

### 请求字段说明

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| title | string | 是 | 文章标题，最大 160 字符 |
| summary | string | 是 | 文章摘要，最大 512 字符 |
| cover | string | 否 | 封面图片地址，填写时必须为 `http` 或 `https` URL，最大 512 字符 |
| category | string | 是 | 文章分类，最大 64 字符 |
| content | string | 是 | 文章正文 |
| tagIds | number[] | 否 | 已存在标签 ID 列表，用于绑定文章标签 |
| tagNames | string[] | 否 | 标签名称列表，不存在时后端自动创建并绑定 |

后端自动生成字段：

| 字段 | 默认值或生成规则 |
| --- | --- |
| authorUserId | 当前登录用户 ID |
| status | `PENDING_REVIEW` |
| publishedAt | 当前服务日期，格式 `yyyy-MM-dd` |
| updatedAt | 当前服务日期，格式 `yyyy-MM-dd` |
| views | `0` |
| likes | `0` |
| comments | `0` |

权限说明：

- 必须登录。
- 普通用户新增文章后默认进入待审核，公开列表暂不展示。
- 管理员新增文章当前也按统一流程进入待审核。

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 6,
    "title": "Spring Boot 新增文章接口实践",
    "summary": "记录文章发布接口的参数校验、默认字段生成和 MyBatis 入库流程。",
    "cover": "https://example.com/cover.jpg",
    "category": "后端开发",
    "content": "这里是文章正文内容。",
    "status": "PENDING_REVIEW",
    "authorUserId": 1,
    "reviewTime": null,
    "reviewerUserId": null,
    "rejectReason": null,
    "tags": ["Spring Boot", "MyBatis"],
    "tagNames": ["Spring Boot", "MyBatis"],
    "publishedAt": "2026-06-03",
    "updatedAt": "2026-06-03",
    "date": "2026-06-03",
    "views": 0,
    "likes": 0,
    "comments": 0
  }
}
```

### 400 响应示例

```json
{
  "code": 400,
  "message": "文章标题不能为空",
  "data": null
}
```

## 7. 保存草稿

### 请求

```http
POST /api/articles/drafts
Content-Type: application/json
Authorization: Bearer {token}
```

### 请求体

与 `POST /api/articles` 相同。

### 功能说明

- 必须登录。
- 后端自动写入 `authorUserId = 当前登录用户 ID`。
- 文章状态为 `DRAFT`。
- 草稿不会出现在公开文章列表。

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "msg": "success",
  "data": {
    "id": 7,
    "title": "草稿标题",
    "status": "DRAFT",
    "authorUserId": 1,
    "rejectReason": null
  }
}
```

## 8. 修改文章

### 请求

```http
PUT /api/articles/{id}
Content-Type: application/json
Authorization: Bearer {token}
```

### 权限说明

- `USER` 只能修改自己的文章。
- `ADMIN` 可以修改所有文章。
- 修改成功后状态统一变成 `PENDING_REVIEW`，并清空历史审核信息，避免已发布文章被修改后绕过审核。

### 请求体

与 `POST /api/articles` 相同。

## 9. 删除文章

### 请求

```http
DELETE /api/articles/{id}
Authorization: Bearer {token}
```

### 权限说明

- `USER` 只能删除自己的文章。
- `ADMIN` 可以删除所有文章。
- 当前实现是软删除：写入 `deleted = 1`、`deletedAt`、`deletedBy`，公开列表和我的文章列表默认不展示已删除文章。

### 响应示例

```json
{
  "code": 200,
  "message": "删除成功",
  "msg": "删除成功",
  "data": null
}
```

## 10. 草稿提交审核

### 请求

```http
POST /api/articles/{id}/submit
Authorization: Bearer {token}
```

### 功能说明

- `USER` 只能提交自己的文章。
- `ADMIN` 可以提交所有文章。
- 后端把文章状态更新为 `PENDING_REVIEW`，并清空 `reviewTime`、`reviewerUserId`、`rejectReason`。

## 11. 我的文章列表

### 请求

```http
GET /api/my/articles?page=1&pageSize=10&status=DRAFT
Authorization: Bearer {token}
```

### 请求参数

| 参数 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | number | 否 | 1 | 页码 |
| pageSize | number | 否 | 10 | 每页数量，最大 50 |
| status | string | 否 | 无 | 可选：`DRAFT`、`PENDING_REVIEW`、`PUBLISHED`、`REJECTED` |

### 功能说明

- 必须登录。
- 只返回当前登录用户自己的文章。
- 不传 `status` 时返回自己的全部状态文章，已软删除文章不返回。

## 12. 管理员文章列表

### 请求

```http
GET /api/admin/articles?page=1&pageSize=10&status=PENDING_REVIEW
Authorization: Bearer {token}
```

### 功能说明

- 必须登录且当前用户 `role = ADMIN`。
- 可查看所有未软删除文章。
- `status` 参数规则与 `/api/my/articles` 相同。

## 13. 管理员审核文章

### 请求

```http
POST /api/admin/articles/{id}/review
Content-Type: application/json
Authorization: Bearer {token}
```

### 请求体

审核通过：

```json
{
  "approved": true,
  "rejectReason": ""
}
```

审核拒绝：

```json
{
  "approved": false,
  "rejectReason": "内容不完整，请补充实践步骤"
}
```

### 功能说明

- 必须登录且当前用户 `role = ADMIN`。
- 审核通过后文章状态变成 `PUBLISHED`，公开文章列表可见。
- 审核拒绝后文章状态变成 `REJECTED`，并保存 `rejectReason`。
- 每次审核都会插入 `article_review_record` 审核记录。

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "msg": "success",
  "data": {
    "id": 6,
    "status": "PUBLISHED",
    "authorUserId": 1,
    "reviewerUserId": 2,
    "rejectReason": null
  }
}
```

## 14. 获取文章详情

公开文章详情只返回 `PUBLISHED` 且未软删除的文章。

### 请求

```http
GET /api/articles/{id}
```

### 路径参数

| 参数 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| id | number | 是 | 文章 ID |

### 请求示例

```http
GET /api/articles/1
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "从零搭建 Spring Boot + Vue 的个人博客系统",
    "summary": "拆解前后端分层、接口约定、登录鉴权和文章发布流程，适合作为项目起点。",
    "cover": "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=900&q=80",
    "category": "项目实战",
    "content": "这篇文章整理个人技术博客的第一版后端模型，包括 Profile、Tag、Article 和 Question 四类核心资源。",
    "tags": ["Vue", "Spring Boot", "MyBatis"],
    "tagNames": ["Vue", "Spring Boot", "MyBatis"],
    "publishedAt": "2026-05-28",
    "updatedAt": "2026-05-28",
    "date": "2026-05-28",
    "views": 2680,
    "likes": 126,
    "comments": 34
  }
}
```

### 404 响应示例

```json
{
  "code": 404,
  "message": "Article not found: 999",
  "data": null
}
```

## 15. 获取问答列表

### 请求

```http
GET /api/questions
```

### 请求参数

无。

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "博客接口怎么规划更舒服？",
      "description": "先统一响应结构，再按 profile、tags、articles、questions 分模块扩展。",
      "answer": "先统一响应结构，再按 profile、tags、articles、questions 分模块扩展。",
      "tags": ["Spring Boot", "MyBatis"],
      "answerCount": 3,
      "updatedAt": "2026-06-01"
    }
  ]
}
```

### data[] 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | number | 问题 ID |
| title | string | 问题标题 |
| description | string | 问题描述 |
| answer | string | 回答内容，当前与 `description` 相同 |
| tags | string[] | 标签名称列表 |
| answerCount | number | 回答数量 |
| updatedAt | string | 更新时间，格式 `yyyy-MM-dd` |

## 前端联调建议

### Axios 基础配置示例

```ts
import axios from "axios";

export const request = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 10000
});
```

### TypeScript 类型参考

```ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  msg: string;
  data: T;
}

export interface CaptchaResponse {
  captchaKey: string;
  image: string;
}

export interface LoginRequest {
  phone: string;
  password: string;
  captchaKey: string;
  captcha: string;
}

export interface LoginResponse {
  token: string;
}

export interface LinkResponse {
  label: string;
  url: string;
}

export interface ProfileResponse {
  nickname: string;
  name: string;
  account: string;
  avatar: string;
  bio: string;
  role: string;
  location: string;
  followers: number;
  articleCount: number;
  articles: number;
  tagCount: number;
  questionCount: number;
  skills: string[];
  links: LinkResponse[];
}

export interface TagResponse {
  id: number;
  name: string;
  articleCount: number;
}

export interface PageResponse<T> {
  total: number;
  list: T[];
}

export interface ArticleResponse {
  id: number;
  title: string;
  summary: string;
  cover: string;
  category: string;
  content: string;
  status: "DRAFT" | "PENDING_REVIEW" | "PUBLISHED" | "REJECTED";
  authorUserId: number;
  reviewTime: string | null;
  reviewerUserId: number | null;
  rejectReason: string | null;
  tags: string[];
  tagNames: string[];
  publishedAt: string;
  updatedAt: string;
  date: string;
  views: number;
  likes: number;
  comments: number;
}

export interface CreateArticleRequest {
  title: string;
  summary: string;
  cover?: string | null;
  category: string;
  content: string;
  tagIds?: number[];
  tagNames?: string[];
}

export interface ReviewArticleRequest {
  approved: boolean;
  rejectReason?: string;
}

export interface QuestionResponse {
  id: number;
  title: string;
  description: string;
  answer: string;
  tags: string[];
  answerCount: number;
  updatedAt: string;
}
```

### API 方法参考

```ts
export const getProfile = () =>
  request.get<ApiResponse<ProfileResponse>>("/profile");

export const getCaptcha = () =>
  request.get<ApiResponse<CaptchaResponse>>("/captcha");

export const login = (data: LoginRequest) =>
  request.post<ApiResponse<LoginResponse>>("/login", data);

export const getTags = () =>
  request.get<ApiResponse<TagResponse[]>>("/tags");

export const getArticles = (params?: {
  page?: number;
  pageSize?: number;
  tagId?: number;
}) => request.get<ApiResponse<PageResponse<ArticleResponse>>>("/articles", { params });

export const getArticleDetail = (id: number) =>
  request.get<ApiResponse<ArticleResponse>>(`/articles/${id}`);

export const createArticle = (data: CreateArticleRequest) =>
  request.post<ApiResponse<ArticleResponse>>("/articles", data);

export const createArticleDraft = (data: CreateArticleRequest) =>
  request.post<ApiResponse<ArticleResponse>>("/articles/drafts", data);

export const updateArticle = (id: number, data: CreateArticleRequest) =>
  request.put<ApiResponse<ArticleResponse>>(`/articles/${id}`, data);

export const deleteArticle = (id: number) =>
  request.delete<ApiResponse<null>>(`/articles/${id}`);

export const submitArticle = (id: number) =>
  request.post<ApiResponse<ArticleResponse>>(`/articles/${id}/submit`);

export const getMyArticles = (params?: {
  page?: number;
  pageSize?: number;
  status?: ArticleResponse["status"];
}) => request.get<ApiResponse<PageResponse<ArticleResponse>>>("/my/articles", { params });

export const getAdminArticles = (params?: {
  page?: number;
  pageSize?: number;
  status?: ArticleResponse["status"];
}) => request.get<ApiResponse<PageResponse<ArticleResponse>>>("/admin/articles", { params });

export const reviewArticle = (id: number, data: ReviewArticleRequest) =>
  request.post<ApiResponse<ArticleResponse>>(`/admin/articles/${id}/review`, data);

export const getQuestions = () =>
  request.get<ApiResponse<QuestionResponse[]>>("/questions");
```

## 跨域配置

当前后端允许以下前端地址访问 `/api/**`：

- `http://localhost:5173`
- `http://127.0.0.1:5173`
