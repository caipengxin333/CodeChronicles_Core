# CodeChronicles Core 接口文档

本文档根据 `src/main/java/com/codechronicles/core/controller` 中的 Controller 整理，供前端开发联调参考。

## 基础信息

- 服务端口：`8080`
- 基础路径：`/api`
- 本地基础地址：`http://localhost:8080/api`
- 当前接口方法：全部为 `GET`
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

```json
{
  "code": 404,
  "message": "Article not found: 999",
  "data": null
}
```

说明：

- 查询不存在的文章详情时，HTTP 状态码为 `404`
- 响应体中的 `code` 为 `404`

## 接口列表

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/profile` | GET | 获取个人资料 |
| `/api/tags` | GET | 获取标签列表 |
| `/api/articles` | GET | 获取文章分页列表 |
| `/api/articles/{id}` | GET | 获取文章详情 |
| `/api/questions` | GET | 获取问答列表 |

## 1. 获取个人资料

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

## 2. 获取标签列表

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

## 3. 获取文章分页列表

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
| tags | string[] | 标签名称列表 |
| tagNames | string[] | 标签名称列表，当前与 `tags` 相同 |
| publishedAt | string | 发布时间，格式 `yyyy-MM-dd` |
| updatedAt | string | 更新时间，格式 `yyyy-MM-dd` |
| date | string | 展示日期，当前与 `publishedAt` 相同 |
| views | number | 浏览量 |
| likes | number | 点赞数 |
| comments | number | 评论数 |

## 4. 获取文章详情

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

## 5. 获取问答列表

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
  data: T;
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
  tags: string[];
  tagNames: string[];
  publishedAt: string;
  updatedAt: string;
  date: string;
  views: number;
  likes: number;
  comments: number;
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

export const getTags = () =>
  request.get<ApiResponse<TagResponse[]>>("/tags");

export const getArticles = (params?: {
  page?: number;
  pageSize?: number;
  tagId?: number;
}) => request.get<ApiResponse<PageResponse<ArticleResponse>>>("/articles", { params });

export const getArticleDetail = (id: number) =>
  request.get<ApiResponse<ArticleResponse>>(`/articles/${id}`);

export const getQuestions = () =>
  request.get<ApiResponse<QuestionResponse[]>>("/questions");
```

## 跨域配置

当前后端允许以下前端地址访问 `/api/**`：

- `http://localhost:5173`
- `http://127.0.0.1:5173`

