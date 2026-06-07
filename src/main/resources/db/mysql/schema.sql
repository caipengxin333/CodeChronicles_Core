create table if not exists profile (
    id bigint primary key auto_increment comment '个人资料主键',
    nickname varchar(64) not null comment '作者昵称',
    account varchar(64) not null comment '作者账号标识',
    avatar varchar(512) comment '头像图片地址',
    bio varchar(512) comment '个人简介',
    role varchar(128) comment '职业或技术方向',
    location varchar(128) comment '所在地',
    followers int not null default 0 comment '关注者数量',
    unique key uk_profile_account (account)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='博客作者个人资料表';

create table if not exists profile_skill (
    id bigint primary key auto_increment comment '个人技能主键',
    profile_id bigint not null comment '所属个人资料ID',
    skill varchar(64) not null comment '技能名称',
    sort_order int not null default 0 comment '展示排序，值越小越靠前',
    constraint fk_profile_skill_profile foreign key (profile_id) references profile (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='个人技术栈技能表';

create table if not exists profile_link (
    id bigint primary key auto_increment comment '个人链接主键',
    profile_id bigint not null comment '所属个人资料ID',
    label varchar(64) not null comment '链接显示名称',
    url varchar(512) not null comment '外部链接地址',
    sort_order int not null default 0 comment '展示排序，值越小越靠前',
    constraint fk_profile_link_profile foreign key (profile_id) references profile (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='个人外部链接表';

create table if not exists `user` (
    id bigint primary key auto_increment comment '用户主键',
    phone varchar(11) not null comment '登录手机号',
    password varchar(100) not null comment 'BCrypt 加密后的密码',
    create_time datetime not null default current_timestamp comment '用户创建时间',
    profile_id bigint not null comment '关联个人资料ID',
    role varchar(32) not null default 'USER' comment '用户角色：ADMIN 管理员，USER 普通用户',
    unique key uk_user_phone (phone),
    constraint fk_user_profile foreign key (profile_id) references profile (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='后台登录用户表';

create table if not exists tag (
    id bigint primary key auto_increment comment '标签主键',
    name varchar(64) not null comment '标签名称',
    sort_order int not null default 0 comment '展示排序，值越小越靠前',
    unique key uk_tag_name (name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='文章和问答共用技术标签表';

create table if not exists article (
    id bigint primary key auto_increment comment '文章主键',
    title varchar(160) not null comment '文章标题',
    summary varchar(512) not null comment '文章摘要',
    cover varchar(512) comment '文章封面图片地址',
    category varchar(64) not null comment '文章分类名称',
    content text comment '文章正文内容',
    status varchar(32) not null default 'PENDING_REVIEW' comment '文章状态：DRAFT 草稿，PENDING_REVIEW 待审核，PUBLISHED 已发布，REJECTED 已拒绝',
    author_user_id bigint not null comment '作者用户ID',
    review_time datetime null comment '审核时间',
    reviewer_user_id bigint null comment '审核人用户ID',
    reject_reason varchar(512) null comment '审核拒绝原因',
    deleted tinyint not null default 0 comment '是否删除：0 未删除，1 已删除',
    deleted_at datetime null comment '删除时间',
    deleted_by bigint null comment '删除人用户ID',
    published_at date not null comment '发布时间，由后端生成',
    updated_at date not null comment '最后更新时间，由后端生成',
    views int not null default 0 comment '阅读量',
    likes int not null default 0 comment '点赞数',
    comments int not null default 0 comment '评论数',
    index idx_article_status_published (status, deleted, published_at),
    index idx_article_author_status (author_user_id, status, deleted),
    constraint fk_article_author foreign key (author_user_id) references `user` (id),
    constraint fk_article_reviewer foreign key (reviewer_user_id) references `user` (id),
    constraint fk_article_deleted_by foreign key (deleted_by) references `user` (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='博客文章表';

create table if not exists article_review_record (
    id bigint primary key auto_increment comment '审核记录ID',
    article_id bigint not null comment '文章ID',
    reviewer_user_id bigint not null comment '审核人用户ID',
    before_status varchar(32) not null comment '审核前状态',
    after_status varchar(32) not null comment '审核后状态',
    approved tinyint not null comment '是否通过：1 通过，0 拒绝',
    reject_reason varchar(512) null comment '拒绝原因',
    review_time datetime not null default current_timestamp comment '审核时间',
    constraint fk_review_article foreign key (article_id) references article (id),
    constraint fk_review_user foreign key (reviewer_user_id) references `user` (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='文章审核记录表';

create table if not exists article_like (
    article_id bigint not null comment '文章ID',
    user_id bigint not null comment '点赞用户ID',
    created_at datetime not null default current_timestamp comment '点赞时间',
    primary key (article_id, user_id),
    index idx_article_like_user (user_id, created_at),
    constraint fk_article_like_article foreign key (article_id) references article (id),
    constraint fk_article_like_user foreign key (user_id) references `user` (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='文章点赞记录表';

create table if not exists article_comment (
    id bigint primary key auto_increment comment '评论ID',
    article_id bigint not null comment '文章ID',
    user_id bigint not null comment '评论用户ID',
    root_comment_id bigint null comment '所属一级评论ID，一级评论为空',
    reply_to_comment_id bigint null comment '直接回复的评论ID',
    content varchar(1000) not null comment '评论内容',
    created_at datetime not null default current_timestamp comment '评论时间',
    index idx_article_comment_article (article_id, created_at),
    index idx_article_comment_root (root_comment_id, created_at),
    index idx_article_comment_user (user_id, created_at),
    constraint fk_article_comment_article foreign key (article_id) references article (id),
    constraint fk_article_comment_user foreign key (user_id) references `user` (id),
    constraint fk_article_comment_root foreign key (root_comment_id) references article_comment (id),
    constraint fk_article_comment_reply_to foreign key (reply_to_comment_id) references article_comment (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='文章评论表';

create table if not exists article_tag (
    article_id bigint not null comment '文章ID',
    tag_id bigint not null comment '标签ID',
    primary key (article_id, tag_id),
    constraint fk_article_tag_article foreign key (article_id) references article (id),
    constraint fk_article_tag_tag foreign key (tag_id) references tag (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='文章与标签关联表';

create table if not exists question (
    id bigint primary key auto_increment comment '问答主键',
    title varchar(160) not null comment '问题标题',
    description varchar(512) not null comment '问题描述或摘要',
    answer_count int not null default 0 comment '回答数量',
    updated_at date not null comment '最后更新时间',
    index idx_question_updated_at (updated_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='问答精选表';

create table if not exists question_tag (
    question_id bigint not null comment '问答ID',
    tag_id bigint not null comment '标签ID',
    primary key (question_id, tag_id),
    constraint fk_question_tag_question foreign key (question_id) references question (id),
    constraint fk_question_tag_tag foreign key (tag_id) references tag (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='问答与标签关联表';
