package com.codechronicles.core.controller;

import com.codechronicles.core.common.ApiResponse;
import com.codechronicles.core.domain.Tag;
import com.codechronicles.core.dto.ArticleResponse;
import com.codechronicles.core.dto.ArticleCommentResponse;
import com.codechronicles.core.dto.ArticleLikeResponse;
import com.codechronicles.core.dto.CreateArticleRequest;
import com.codechronicles.core.dto.CreateCommentRequest;
import com.codechronicles.core.dto.PageResponse;
import com.codechronicles.core.dto.ProfileResponse;
import com.codechronicles.core.dto.QuestionResponse;
import com.codechronicles.core.dto.ReviewArticleRequest;
import com.codechronicles.core.service.BlogService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    /**
     * 获取首页/侧边栏展示的个人资料、统计数据、技能和外部链接。
     */
    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> getProfile() {
        return ApiResponse.success(blogService.getProfile());
    }

    /**
     * 获取标签列表，包含每个标签下已发布文章数量。
     */
    @GetMapping("/tags")
    public ApiResponse<List<Tag>> getTags() {
        return ApiResponse.success(blogService.getTags());
    }

    /**
     * 分页查询已发布文章，可按 tagId 筛选。
     */
    @GetMapping("/articles")
    public ApiResponse<PageResponse<ArticleResponse>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long tagId
    ) {
        return ApiResponse.success(blogService.getArticles(page, pageSize, tagId));
    }

    /**
     * 查询单篇文章详情，不存在时由全局异常处理器返回 404。
     */
    @GetMapping("/articles/{id}")
    public ApiResponse<ArticleResponse> getArticleDetail(@PathVariable Long id) {
        return ApiResponse.success(blogService.getArticleDetail(id));
    }

    /**
     * 当前用户点赞文章，重复点赞不会重复增加计数。
     */
    @PostMapping("/articles/{id}/likes")
    public ApiResponse<ArticleLikeResponse> likeArticle(@PathVariable Long id) {
        return ApiResponse.success(blogService.likeArticle(id));
    }

    /**
     * 当前用户取消文章点赞。
     */
    @DeleteMapping("/articles/{id}/likes")
    public ApiResponse<ArticleLikeResponse> unlikeArticle(@PathVariable Long id) {
        return ApiResponse.success(blogService.unlikeArticle(id));
    }

    /**
     * 当前用户发表评论，评论内容和评论数在同一事务内写入。
     */
    @PostMapping("/articles/{id}/comments")
    public ApiResponse<ArticleCommentResponse> createComment(
            @PathVariable Long id,
            @RequestBody CreateCommentRequest request
    ) {
        return ApiResponse.success(blogService.createComment(id, request));
    }

    /**
     * 分页查询一级评论，每条一级评论携带其二级回复列表。
     */
    @GetMapping("/articles/{id}/comments")
    public ApiResponse<PageResponse<ArticleCommentResponse>> getArticleComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(blogService.getArticleComments(id, page, pageSize));
    }

    /**
     * 新增文章，发布时间、统计字段和临时自动标签由后端生成。
     */
    @PostMapping("/articles")
    public ApiResponse<ArticleResponse> createArticle(@RequestBody CreateArticleRequest request) {
        return ApiResponse.success(blogService.createArticle(request));
    }

    @PostMapping("/articles/drafts")
    public ApiResponse<ArticleResponse> createDraft(@RequestBody CreateArticleRequest request) {
        return ApiResponse.success(blogService.createDraft(request));
    }

    @PutMapping("/articles/{id}")
    public ApiResponse<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @RequestBody CreateArticleRequest request
    ) {
        return ApiResponse.success(blogService.updateArticle(id, request));
    }

    @DeleteMapping("/articles/{id}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long id) {
        blogService.deleteArticle(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/articles/{id}/submit")
    public ApiResponse<ArticleResponse> submitArticle(@PathVariable Long id) {
        return ApiResponse.success(blogService.submitArticle(id));
    }

    @GetMapping("/my/articles")
    public ApiResponse<PageResponse<ArticleResponse>> getMyArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(blogService.getMyArticles(page, pageSize, status));
    }

    @GetMapping("/admin/articles")
    public ApiResponse<PageResponse<ArticleResponse>> getAdminArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(blogService.getAdminArticles(page, pageSize, status));
    }

    @PostMapping("/admin/articles/{id}/review")
    public ApiResponse<ArticleResponse> reviewArticle(
            @PathVariable Long id,
            @RequestBody ReviewArticleRequest request
    ) {
        return ApiResponse.success(blogService.reviewArticle(id, request));
    }

    /**
     * 获取问答列表，用于前端问答模块展示。
     */
    @GetMapping("/questions")
    public ApiResponse<List<QuestionResponse>> getQuestions() {
        return ApiResponse.success(blogService.getQuestions());
    }
}
