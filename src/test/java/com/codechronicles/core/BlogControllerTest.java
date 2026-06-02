package com.codechronicles.core;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("CodeChronicles"))
                .andExpect(jsonPath("$.data.skills", hasItem("Spring Boot")));
    }

    @Test
    void shouldReturnPagedArticles() throws Exception {
        mockMvc.perform(get("/api/articles").param("page", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThan(0)))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].tags.length()", greaterThan(0)));
    }

    @Test
    void shouldReturnArticleDetail() throws Exception {
        mockMvc.perform(get("/api/articles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("从零搭建 Spring Boot + Vue 的个人博客系统"))
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    void shouldCreateArticleWithGeneratedFields() throws Exception {
        String payload = """
                {
                  "title": "Spring Boot 文章新增接口设计",
                  "summary": "验证新增文章时由后端生成发布时间和统计字段。",
                  "cover": "https://example.com/cover.png",
                  "category": "后端实践",
                  "content": "正文由前端提交，发布时间、更新时间、阅读量、点赞数和评论数由后端生成。"
                }
                """;

        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("Spring Boot 文章新增接口设计"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.views").value(0))
                .andExpect(jsonPath("$.data.likes").value(0))
                .andExpect(jsonPath("$.data.comments").value(0))
                .andExpect(jsonPath("$.data.tags.length()", greaterThan(0)));
    }

    @Test
    void shouldRejectCreateArticleWithoutTitle() throws Exception {
        String payload = """
                {
                  "summary": "缺少标题时不能保存。",
                  "category": "后端实践",
                  "content": "正文"
                }
                """;

        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章标题不能为空"));
    }
}
