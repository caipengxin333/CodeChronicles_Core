set names utf8mb4;

create database if not exists codechronicles
    default character set utf8mb4
    default collate utf8mb4_0900_ai_ci;

use codechronicles;

create table if not exists profile (
    id bigint primary key auto_increment,
    nickname varchar(64) not null,
    account varchar(64) not null,
    avatar varchar(512),
    bio varchar(512),
    role varchar(128),
    location varchar(128),
    followers int not null default 0
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists profile_skill (
    id bigint primary key auto_increment,
    profile_id bigint not null,
    skill varchar(64) not null,
    sort_order int not null default 0,
    constraint fk_profile_skill_profile foreign key (profile_id) references profile (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists profile_link (
    id bigint primary key auto_increment,
    profile_id bigint not null,
    label varchar(64) not null,
    url varchar(512) not null,
    sort_order int not null default 0,
    constraint fk_profile_link_profile foreign key (profile_id) references profile (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists tag (
    id bigint primary key auto_increment,
    name varchar(64) not null,
    sort_order int not null default 0,
    unique key uk_tag_name (name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists article (
    id bigint primary key auto_increment,
    title varchar(160) not null,
    summary varchar(512) not null,
    cover varchar(512),
    category varchar(64) not null,
    content text,
    status varchar(32) not null default 'PUBLISHED',
    published_at date not null,
    updated_at date not null,
    views int not null default 0,
    likes int not null default 0,
    comments int not null default 0,
    index idx_article_status_published (status, published_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists article_tag (
    article_id bigint not null,
    tag_id bigint not null,
    primary key (article_id, tag_id),
    constraint fk_article_tag_article foreign key (article_id) references article (id),
    constraint fk_article_tag_tag foreign key (tag_id) references tag (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists question (
    id bigint primary key auto_increment,
    title varchar(160) not null,
    description varchar(512) not null,
    answer_count int not null default 0,
    updated_at date not null,
    index idx_question_updated_at (updated_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists question_tag (
    question_id bigint not null,
    tag_id bigint not null,
    primary key (question_id, tag_id),
    constraint fk_question_tag_question foreign key (question_id) references question (id),
    constraint fk_question_tag_tag foreign key (tag_id) references tag (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

insert into profile (id, nickname, account, avatar, bio, role, location, followers) values
(1, 'CodeChronicles', 'code_chronicles', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=300&q=80', '记录 Spring Boot、Vue、工程实践与日常踩坑，把复杂问题写清楚。', '全栈开发者', 'Shanghai', 1280)
on duplicate key update nickname = values(nickname), account = values(account), avatar = values(avatar), bio = values(bio), role = values(role), location = values(location), followers = values(followers);

insert into profile_skill (id, profile_id, skill, sort_order) values
(1, 1, 'Java', 1), (2, 1, 'Spring Boot', 2), (3, 1, 'MyBatis', 3), (4, 1, 'Vue', 4), (5, 1, 'Element Plus', 5), (6, 1, 'MySQL', 6)
on duplicate key update skill = values(skill), sort_order = values(sort_order);

insert into profile_link (id, profile_id, label, url, sort_order) values
(1, 1, 'GitHub', 'https://github.com/example', 1), (2, 1, 'Email', 'mailto:hello@example.com', 2)
on duplicate key update label = values(label), url = values(url), sort_order = values(sort_order);

insert into tag (id, name, sort_order) values
(1, 'Vue', 1), (2, 'Spring Boot', 2), (3, 'MyBatis', 3), (4, 'MySQL', 4), (5, 'Redis', 5), (6, '架构设计', 6), (7, '部署运维', 7), (8, 'Element Plus', 8)
on duplicate key update name = values(name), sort_order = values(sort_order);

insert into article (id, title, summary, cover, category, content, published_at, updated_at, views, likes, comments) values
(1, '从零搭建 Spring Boot + Vue 的个人博客系统', '拆解前后端分层、接口约定、登录鉴权和文章发布流程，适合作为项目起点。', 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=900&q=80', '项目实战', '这篇文章整理个人技术博客的第一版后端模型，包括 Profile、Tag、Article 和 Question 四类核心资源。', '2026-05-28', '2026-05-28', 2680, 126, 34),
(2, 'Element Plus 后台布局的可维护写法', '用组件边界、状态收敛和响应式布局，让管理端界面保持清爽。', 'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=900&q=80', '前端工程', '布局不是堆组件，先稳定信息架构，再处理交互和视觉细节。', '2026-05-21', '2026-05-23', 1842, 96, 17),
(3, 'Redis 缓存穿透和热点 Key 的处理策略', '结合实际接口流量，整理缓存空值、布隆过滤器、互斥锁和限流方案。', 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=900&q=80', '后端实践', '缓存问题通常不是单点问题，需要结合数据命中率、失效策略和降级路径一起看。', '2026-05-16', '2026-05-18', 3210, 188, 41),
(4, 'MyBatis XML Mapper 的边界感', '用清晰的 SQL、DTO 组装和 ResultMap 约定，把数据访问层保持在合适复杂度。', 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=900&q=80', '后端实践', 'MyBatis 的优势是 SQL 透明，业务组装逻辑仍应放在 Service 层。', '2026-05-08', '2026-05-10', 1460, 73, 11),
(5, '个人博客数据库表设计第一版', '围绕文章、标签、问答和个人信息，设计一套够用且易扩展的数据结构。', 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?auto=format&fit=crop&w=900&q=80', '数据库', '先让核心阅读流程跑通，再为评论、归档和搜索补表。', '2026-04-30', '2026-05-02', 1328, 64, 9)
on duplicate key update title = values(title), summary = values(summary), cover = values(cover), category = values(category), content = values(content), published_at = values(published_at), updated_at = values(updated_at), views = values(views), likes = values(likes), comments = values(comments);

insert ignore into article_tag (article_id, tag_id) values
(1, 1), (1, 2), (1, 3), (2, 1), (2, 8), (3, 2), (3, 5), (3, 6), (4, 2), (4, 3), (4, 4), (5, 4), (5, 6);

insert into question (id, title, description, answer_count, updated_at) values
(1, '博客接口怎么规划更舒服？', '先统一响应结构，再按 profile、tags、articles、questions 分模块扩展。', 3, '2026-06-01'),
(2, '文章列表需要分页吗？', '需要。前端预留 page、pageSize、tagId、keyword 后，后端可以逐步扩展查询条件。', 2, '2026-05-30'),
(3, 'MyBatis 适合个人博客项目吗？', '适合，尤其当你希望 SQL 清晰可控，并且后续可能针对列表查询做性能优化。', 4, '2026-05-24')
on duplicate key update title = values(title), description = values(description), answer_count = values(answer_count), updated_at = values(updated_at);

insert ignore into question_tag (question_id, tag_id) values
(1, 2), (1, 3), (2, 1), (2, 2), (3, 3), (3, 4);

alter table profile comment = '博客作者个人资料表';
alter table profile
    modify id bigint not null auto_increment comment '个人资料主键',
    modify nickname varchar(64) not null comment '作者昵称',
    modify account varchar(64) not null comment '作者账号标识',
    modify avatar varchar(512) null comment '头像图片地址',
    modify bio varchar(512) null comment '个人简介',
    modify role varchar(128) null comment '职业或技术方向',
    modify location varchar(128) null comment '所在地',
    modify followers int not null default 0 comment '关注者数量';

alter table profile_skill comment = '个人技术栈技能表';
alter table profile_skill
    modify id bigint not null auto_increment comment '个人技能主键',
    modify profile_id bigint not null comment '所属个人资料ID',
    modify skill varchar(64) not null comment '技能名称',
    modify sort_order int not null default 0 comment '展示排序，值越小越靠前';

alter table profile_link comment = '个人外部链接表';
alter table profile_link
    modify id bigint not null auto_increment comment '个人链接主键',
    modify profile_id bigint not null comment '所属个人资料ID',
    modify label varchar(64) not null comment '链接显示名称',
    modify url varchar(512) not null comment '外部链接地址',
    modify sort_order int not null default 0 comment '展示排序，值越小越靠前';

alter table tag comment = '文章和问答共用技术标签表';
alter table tag
    modify id bigint not null auto_increment comment '标签主键',
    modify name varchar(64) not null comment '标签名称',
    modify sort_order int not null default 0 comment '展示排序，值越小越靠前';

alter table article comment = '博客文章表';
alter table article
    modify id bigint not null auto_increment comment '文章主键',
    modify title varchar(160) not null comment '文章标题',
    modify summary varchar(512) not null comment '文章摘要',
    modify cover varchar(512) null comment '文章封面图片地址',
    modify category varchar(64) not null comment '文章分类名称',
    modify content text null comment '文章正文内容',
    modify status varchar(32) not null default 'PUBLISHED' comment '文章状态：PUBLISHED 已发布，DRAFT 草稿',
    modify published_at date not null comment '发布时间，由后端生成',
    modify updated_at date not null comment '最后更新时间，由后端生成',
    modify views int not null default 0 comment '阅读量',
    modify likes int not null default 0 comment '点赞数',
    modify comments int not null default 0 comment '评论数';

alter table article_tag comment = '文章与标签关联表';
alter table article_tag
    modify article_id bigint not null comment '文章ID',
    modify tag_id bigint not null comment '标签ID';

alter table question comment = '问答精选表';
alter table question
    modify id bigint not null auto_increment comment '问答主键',
    modify title varchar(160) not null comment '问题标题',
    modify description varchar(512) not null comment '问题描述或摘要',
    modify answer_count int not null default 0 comment '回答数量',
    modify updated_at date not null comment '最后更新时间';

alter table question_tag comment = '问答与标签关联表';
alter table question_tag
    modify question_id bigint not null comment '问答ID',
    modify tag_id bigint not null comment '标签ID';
