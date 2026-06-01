package com.codechronicles.core.service;

import com.codechronicles.core.domain.Article;
import com.codechronicles.core.domain.Profile;
import com.codechronicles.core.domain.ProfileLink;
import com.codechronicles.core.domain.Question;
import com.codechronicles.core.domain.Tag;
import com.codechronicles.core.dto.ArticleResponse;
import com.codechronicles.core.dto.LinkResponse;
import com.codechronicles.core.dto.PageResponse;
import com.codechronicles.core.dto.ProfileResponse;
import com.codechronicles.core.dto.QuestionResponse;
import com.codechronicles.core.mapper.BlogMapper;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class BlogService {

    private final BlogMapper blogMapper;

    public BlogService(BlogMapper blogMapper) {
        this.blogMapper = blogMapper;
    }

    public ProfileResponse getProfile() {
        Profile profile = blogMapper.selectProfile();
        List<String> skills = blogMapper.selectProfileSkills(profile.getId());
        List<LinkResponse> links = blogMapper.selectProfileLinks(profile.getId()).stream()
                .map(this::toLinkResponse)
                .toList();
        int articleCount = Math.toIntExact(blogMapper.countArticles(null));
        int tagCount = blogMapper.countTags();
        int questionCount = blogMapper.countQuestions();

        return new ProfileResponse(
                profile.getNickname(),
                profile.getNickname(),
                profile.getAccount(),
                profile.getAvatar(),
                profile.getBio(),
                profile.getRole(),
                profile.getLocation(),
                profile.getFollowers(),
                articleCount,
                articleCount,
                tagCount,
                questionCount,
                skills,
                links
        );
    }

    public List<Tag> getTags() {
        return blogMapper.selectTags();
    }

    public PageResponse<ArticleResponse> getArticles(int page, int pageSize, Long tagId) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (currentPage - 1) * currentPageSize;
        long total = blogMapper.countArticles(tagId);
        List<ArticleResponse> list = blogMapper.selectArticles(tagId, currentPageSize, offset).stream()
                .map(this::toArticleResponse)
                .toList();

        return new PageResponse<>(total, list);
    }

    public ArticleResponse getArticleDetail(Long id) {
        Article article = blogMapper.selectArticleById(id);
        if (article == null) {
            throw new NoSuchElementException("Article not found: " + id);
        }
        return toArticleResponse(article);
    }

    public List<QuestionResponse> getQuestions() {
        return blogMapper.selectQuestions().stream()
                .map(this::toQuestionResponse)
                .toList();
    }

    private LinkResponse toLinkResponse(ProfileLink link) {
        return new LinkResponse(link.getLabel(), link.getUrl());
    }

    private ArticleResponse toArticleResponse(Article article) {
        List<String> tags = blogMapper.selectArticleTags(article.getId());
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getCover(),
                article.getCategory(),
                article.getContent(),
                tags,
                tags,
                article.getPublishedAt(),
                article.getUpdatedAt(),
                article.getPublishedAt(),
                article.getViews(),
                article.getLikes(),
                article.getComments()
        );
    }

    private QuestionResponse toQuestionResponse(Question question) {
        List<String> tags = blogMapper.selectQuestionTags(question.getId());
        return new QuestionResponse(
                question.getId(),
                question.getTitle(),
                question.getDescription(),
                question.getDescription(),
                tags,
                question.getAnswerCount(),
                question.getUpdatedAt()
        );
    }
}
