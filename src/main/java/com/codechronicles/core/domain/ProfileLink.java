package com.codechronicles.core.domain;

/**
 * 个人资料外部链接实体，对应 profile_link 表。
 */
public class ProfileLink {

    private Long id;
    private String label;
    private String url;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
