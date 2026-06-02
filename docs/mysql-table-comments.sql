use codechronicles;

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
