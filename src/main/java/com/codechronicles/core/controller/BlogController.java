package com.codechronicles.core.controller;

import com.codechronicles.core.common.ApiResponse;
import com.codechronicles.core.domain.Tag;
import com.codechronicles.core.dto.ArticleResponse;
import com.codechronicles.core.dto.PageResponse;
import com.codechronicles.core.dto.ProfileResponse;
import com.codechronicles.core.dto.QuestionResponse;
import com.codechronicles.core.service.BlogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> getProfile() {
        return ApiResponse.success(blogService.getProfile());
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> getTags() {
        return ApiResponse.success(blogService.getTags());
    }

    @GetMapping("/articles")
    public ApiResponse<PageResponse<ArticleResponse>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long tagId
    ) {
        return ApiResponse.success(blogService.getArticles(page, pageSize, tagId));
    }

    @GetMapping("/articles/{id}")
    public ApiResponse<ArticleResponse> getArticleDetail(@PathVariable Long id) {
        return ApiResponse.success(blogService.getArticleDetail(id));
    }

    @GetMapping("/questions")
    public ApiResponse<List<QuestionResponse>> getQuestions() {
        return ApiResponse.success(blogService.getQuestions());
    }
}
