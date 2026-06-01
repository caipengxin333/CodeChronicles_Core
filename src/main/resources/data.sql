insert into profile (id, nickname, account, avatar, bio, role, location, followers) values
(1, 'CodeChronicles', 'code_chronicles', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=300&q=80', '记录 Spring Boot、Vue、工程实践与日常踩坑，把复杂问题写清楚。', '全栈开发者', 'Shanghai', 1280);

insert into profile_skill (profile_id, skill, sort_order) values
(1, 'Java', 1),
(1, 'Spring Boot', 2),
(1, 'MyBatis', 3),
(1, 'Vue', 4),
(1, 'Element Plus', 5),
(1, 'MySQL', 6);

insert into profile_link (profile_id, label, url, sort_order) values
(1, 'GitHub', 'https://github.com/example', 1),
(1, 'Email', 'mailto:hello@example.com', 2);

insert into tag (id, name, sort_order) values
(1, 'Vue', 1),
(2, 'Spring Boot', 2),
(3, 'MyBatis', 3),
(4, 'MySQL', 4),
(5, 'Redis', 5),
(6, '架构设计', 6),
(7, '部署运维', 7),
(8, 'Element Plus', 8);

insert into article (id, title, summary, cover, category, content, published_at, updated_at, views, likes, comments) values
(1, '从零搭建 Spring Boot + Vue 的个人博客系统', '拆解前后端分层、接口约定、登录鉴权和文章发布流程，适合作为项目起点。', 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=900&q=80', '项目实战', '这篇文章整理个人技术博客的第一版后端模型，包括 Profile、Tag、Article 和 Question 四类核心资源。', '2026-05-28', '2026-05-28', 2680, 126, 34),
(2, 'Element Plus 后台布局的可维护写法', '用组件边界、状态收敛和响应式布局，让管理端界面保持清爽。', 'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=900&q=80', '前端工程', '布局不是堆组件，先稳定信息架构，再处理交互和视觉细节。', '2026-05-21', '2026-05-23', 1842, 96, 17),
(3, 'Redis 缓存穿透和热点 Key 的处理策略', '结合实际接口流量，整理缓存空值、布隆过滤器、互斥锁和限流方案。', 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=900&q=80', '后端实践', '缓存问题通常不是单点问题，需要结合数据命中率、失效策略和降级路径一起看。', '2026-05-16', '2026-05-18', 3210, 188, 41),
(4, 'MyBatis XML Mapper 的边界感', '用清晰的 SQL、DTO 组装和 ResultMap 约定，把数据访问层保持在合适复杂度。', 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=900&q=80', '后端实践', 'MyBatis 的优势是 SQL 透明，业务组装逻辑仍应放在 Service 层。', '2026-05-08', '2026-05-10', 1460, 73, 11),
(5, '个人博客数据库表设计第一版', '围绕文章、标签、问答和个人信息，设计一套够用且易扩展的数据结构。', 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?auto=format&fit=crop&w=900&q=80', '数据库', '先让核心阅读流程跑通，再为评论、归档和搜索补表。', '2026-04-30', '2026-05-02', 1328, 64, 9);

insert into article_tag (article_id, tag_id) values
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 8),
(3, 2), (3, 5), (3, 6),
(4, 2), (4, 3), (4, 4),
(5, 4), (5, 6);

insert into question (id, title, description, answer_count, updated_at) values
(1, '博客接口怎么规划更舒服？', '先统一响应结构，再按 profile、tags、articles、questions 分模块扩展。', 3, '2026-06-01'),
(2, '文章列表需要分页吗？', '需要。前端预留 page、pageSize、tagId、keyword 后，后端可以逐步扩展查询条件。', 2, '2026-05-30'),
(3, 'MyBatis 适合个人博客项目吗？', '适合，尤其当你希望 SQL 清晰可控，并且后续可能针对列表查询做性能优化。', 4, '2026-05-24');

insert into question_tag (question_id, tag_id) values
(1, 2), (1, 3),
(2, 1), (2, 2),
(3, 3), (3, 4);
