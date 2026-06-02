package com.codechronicles.core.domain;

/**
 * 标签实体，对应 tag 表；articleCount 是查询列表时额外统计出的文章数量。
 */
public class Tag {

    private Long id;
    private String name;
    private Integer articleCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
