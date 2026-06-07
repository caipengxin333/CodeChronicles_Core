package com.codechronicles.core;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    private final Map<String, String> redisValues = new ConcurrentHashMap<>();

    @BeforeEach
    void setUpRedis() {
        redisValues.clear();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doAnswer(invocation -> {
            redisValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redisValues.get(invocation.getArgument(0)));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(invocation -> redisValues.putIfAbsent(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ) == null);
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> redisValues.remove(invocation.getArgument(0)) != null);
    }

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
    void shouldCreateAndPageTwoLevelComments() throws Exception {
        String token = loginToken();
        MvcResult rootResult = mockMvc.perform(post("/api/articles/5/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "一级评论 A"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rootCommentId").doesNotExist())
                .andExpect(jsonPath("$.data.replies.length()").value(0))
                .andReturn();
        Number rootCommentId = com.jayway.jsonpath.JsonPath.read(
                rootResult.getResponse().getContentAsString(),
                "$.data.id"
        );

        clearCommentRateLimit();
        MvcResult replyResult = mockMvc.perform(post("/api/articles/5/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "回复 A 的评论 a",
                                  "parentCommentId": %d
                                }
                                """.formatted(rootCommentId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rootCommentId").value(rootCommentId.longValue()))
                .andExpect(jsonPath("$.data.replyToCommentId").value(rootCommentId.longValue()))
                .andReturn();
        Number replyCommentId = com.jayway.jsonpath.JsonPath.read(
                replyResult.getResponse().getContentAsString(),
                "$.data.id"
        );

        clearCommentRateLimit();
        mockMvc.perform(post("/api/articles/5/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "回复 a，但仍展示在 A 的二级回复列表",
                                  "parentCommentId": %d
                                }
                                """.formatted(replyCommentId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rootCommentId").value(rootCommentId.longValue()))
                .andExpect(jsonPath("$.data.replyToCommentId").value(replyCommentId.longValue()));

        mockMvc.perform(get("/api/articles/5/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].content").value("一级评论 A"))
                .andExpect(jsonPath("$.data.list[0].replies.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].replies[0].content").value("回复 A 的评论 a"))
                .andExpect(jsonPath("$.data.list[0].replies[1].replyToCommentId").value(replyCommentId.longValue()))
                .andExpect(jsonPath("$.data.list[0].replies[1].replyToUserName").value("CodeChronicles"));
    }

    @Test
    void shouldGenerateCaptcha() throws Exception {
        mockMvc.perform(get("/api/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.captchaKey").isNotEmpty())
                .andExpect(jsonPath("$.data.image").isNotEmpty());
    }

    @Test
    void shouldLoginWithCaptchaAndPassword() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.msg").value("登录成功"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void shouldRegisterWithoutToken() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("13900139000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.profileId").isNumber())
                .andExpect(jsonPath("$.data.phone").value("13900139000"))
                .andExpect(jsonPath("$.data.account").isNotEmpty());
    }

    @Test
    void shouldRejectRegisterWithDuplicatedPhone() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("13800138000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("手机号已注册"));
    }

    @Test
    void shouldRejectCreateArticleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticlePayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录"));
    }

    @Test
    void shouldRejectMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录"))
                .andExpect(jsonPath("$.msg").value("登录已过期，请重新登录"));
    }

    @Test
    void shouldReturnCurrentUserInfo() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + loginToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.nickname").value("CodeChronicles"))
                .andExpect(jsonPath("$.data.name").value("CodeChronicles"))
                .andExpect(jsonPath("$.data.avatar").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("全栈开发者"))
                .andExpect(jsonPath("$.data.bio").isNotEmpty())
                .andExpect(jsonPath("$.data.location").value("Shanghai"))
                .andExpect(jsonPath("$.data.followers").value(1280))
                .andExpect(jsonPath("$.data.articleCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.publishedArticleCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.tagCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.questionCount").isNumber())
                .andExpect(jsonPath("$.data.commentCount").doesNotExist())
                .andExpect(jsonPath("$.data.skills", hasItem("Spring Boot")))
                .andExpect(jsonPath("$.data.links[0].label").value("GitHub"));
    }

    @Test
    void shouldReturnZeroPersonalContentCountsForNewUser() throws Exception {
        String phone = "13700137000";
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(phone)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + loginToken(phone, "Aa123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleCount").value(0))
                .andExpect(jsonPath("$.data.publishedArticleCount").value(0))
                .andExpect(jsonPath("$.data.tagCount").value(0))
                .andExpect(jsonPath("$.data.questionCount").value(0))
                .andExpect(jsonPath("$.data.commentCount").doesNotExist());
    }

    @Test
    void shouldCountCurrentUsersArticleComments() throws Exception {
        String phone = "13600136000";
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(phone)))
                .andExpect(status().isOk());
        String token = loginToken(phone, "Aa123456");

        mockMvc.perform(post("/api/articles/1/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "用于验证个人评论数量"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(1))
                .andExpect(jsonPath("$.data.commentCount").doesNotExist());
    }

    @Test
    void shouldCreateArticleWithGeneratedFields() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticlePayload()))
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
                        .header("Authorization", "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章标题不能为空"));
    }

    @Test
    void shouldAllowPublicApiWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("CodeChronicles"));
    }

    @Test
    void shouldRejectProtectedApiWithInvalidToken() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticlePayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录"));
    }

    @Test
    void shouldInvalidateTokenAfterLogout() throws Exception {
        String token = loginToken();

        mockMvc.perform(post("/api/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("退出成功"));

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticlePayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录"));
    }

    private String loginToken() throws Exception {
        return loginToken("13800138000", "Aa123456");
    }

    private String loginToken(String phone, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(phone, password)))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.data.token"
        );
    }

    private String loginPayload() throws Exception {
        return loginPayload("13800138000", "Aa123456");
    }

    private String loginPayload(String phone, String password) throws Exception {
        MvcResult captchaResult = mockMvc.perform(get("/api/captcha"))
                .andExpect(status().isOk())
                .andReturn();
        String response = captchaResult.getResponse().getContentAsString();
        String captchaKey = com.jayway.jsonpath.JsonPath.read(response, "$.data.captchaKey");
        String redisCaptcha = redisValues.get("cc:test:captcha:" + captchaKey);

        return """
                {
                  "phone": "%s",
                  "password": "%s",
                  "captchaKey": "%s",
                  "captcha": "%s"
                }
                """.formatted(phone, password, captchaKey, redisCaptcha.toLowerCase());
    }

    private String validArticlePayload() {
        return """
                {
                  "title": "Spring Boot 文章新增接口设计",
                  "summary": "验证新增文章时由后端生成发布时间和统计字段。",
                  "cover": "https://example.com/cover.png",
                  "category": "后端实践",
                  "content": "正文由前端提交，发布时间、更新时间、阅读量、点赞数和评论数由后端生成。"
                }
                """;
    }

    private void clearCommentRateLimit() {
        redisValues.keySet().removeIf(key -> key.contains(":interaction:comment:"));
    }

    private String registerPayload(String phone) {
        return """
                {
                  "phone": "%s",
                  "password": "Aa123456",
                  "nickname": "蔡鹏鑫",
                  "avatar": "",
                  "bio": "",
                  "role": "Java 后端开发",
                  "location": "北京",
                  "skills": ["Java", "Spring Boot", "MySQL"],
                  "links": [
                    {
                      "label": "GitHub",
                      "url": "https://github.com/xxx"
                    }
                  ]
                }
                """.formatted(phone);
    }
}
