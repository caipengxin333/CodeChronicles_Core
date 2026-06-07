package com.codechronicles.core.service;

import com.codechronicles.core.domain.Article;
import com.codechronicles.core.domain.ArticleComment;
import com.codechronicles.core.domain.Profile;
import com.codechronicles.core.domain.ProfileLink;
import com.codechronicles.core.domain.Question;
import com.codechronicles.core.domain.Tag;
import com.codechronicles.core.common.CurrentUserContext;
import com.codechronicles.core.config.AiChatClientConfig;
import com.codechronicles.core.dto.ArticleResponse;
import com.codechronicles.core.dto.ArticleCommentResponse;
import com.codechronicles.core.dto.ArticleLikeResponse;
import com.codechronicles.core.dto.CreateArticleRequest;
import com.codechronicles.core.dto.CreateCommentRequest;
import com.codechronicles.core.dto.LinkResponse;
import com.codechronicles.core.dto.PageResponse;
import com.codechronicles.core.dto.ProfileResponse;
import com.codechronicles.core.dto.QuestionResponse;
import com.codechronicles.core.dto.ReviewArticleRequest;
import com.codechronicles.core.mapper.BlogMapper;
import com.codechronicles.core.util.ThreadLocalUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final int MAX_AUTO_TAG_COUNT = 5;

    private final BlogMapper blogMapper;
    private final InteractionRateLimitService interactionRateLimitService;
    private final ChatClient tagExtractorChatClient;

    public BlogService(
            BlogMapper blogMapper,
            InteractionRateLimitService interactionRateLimitService,
            @Qualifier(AiChatClientConfig.TAG_EXTRACTOR_CHAT_CLIENT) ChatClient tagExtractorChatClient
    ) {
        this.blogMapper = blogMapper;
        this.interactionRateLimitService = interactionRateLimitService;
        this.tagExtractorChatClient = tagExtractorChatClient;
    }

    /**
     * 组装个人资料页数据，统计值实时从文章、标签和问答表计算。
     */
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

    /**
     * 查询文章分页。后端兜底修正分页参数，避免前端传入 0、负数或过大的 pageSize。
     */
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

    @Transactional
    public ArticleResponse getArticleDetail(Long id) {
        int updatedRows = blogMapper.incrementArticleViews(id);
        if (updatedRows == 0) {
            throw new NoSuchElementException("Article not found: " + id);
        }
        Article article = blogMapper.selectArticleById(id);
        return toArticleResponse(article);
    }

    @Transactional
    public ArticleLikeResponse likeArticle(Long articleId) {
        CurrentUserContext currentUser = requireCurrentUser();
        requirePublishedArticle(articleId);
        interactionRateLimitService.checkLike(currentUser.userId(), articleId);
        if (blogMapper.countArticleLike(articleId, currentUser.userId()) > 0) {
            throw new IllegalArgumentException("不能重复点赞");
        }
        blogMapper.insertArticleLike(articleId, currentUser.userId());
        blogMapper.incrementArticleLikes(articleId);
        return new ArticleLikeResponse(articleId, true, blogMapper.selectArticleLikes(articleId));
    }

    @Transactional
    public ArticleLikeResponse unlikeArticle(Long articleId) {
        CurrentUserContext currentUser = requireCurrentUser();
        requirePublishedArticle(articleId);
        interactionRateLimitService.checkLike(currentUser.userId(), articleId);
        if (blogMapper.deleteArticleLike(articleId, currentUser.userId()) == 0) {
            throw new IllegalArgumentException("当前文章尚未点赞");
        }
        blogMapper.decrementArticleLikes(articleId);
        return new ArticleLikeResponse(articleId, false, blogMapper.selectArticleLikes(articleId));
    }

    @Transactional
    public ArticleCommentResponse createComment(Long articleId, CreateCommentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        CurrentUserContext currentUser = requireCurrentUser();
        requirePublishedArticle(articleId);

        ArticleComment comment = new ArticleComment();
        comment.setArticleId(articleId);
        comment.setUserId(currentUser.userId());
        applyCommentReplyTarget(comment, request.parentCommentId());
        comment.setContent(requireText(request.content(), "评论内容", 1000));
        comment.setCreatedAt(LocalDateTime.now());
        interactionRateLimitService.checkComment(currentUser.userId(), articleId);
        blogMapper.insertArticleComment(comment);
        blogMapper.incrementArticleComments(articleId);
        return toCommentResponse(blogMapper.selectArticleCommentById(comment.getId()), List.of());
    }

    public PageResponse<ArticleCommentResponse> getArticleComments(Long articleId, int page, int pageSize) {
        requirePublishedArticle(articleId);
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (currentPage - 1) * currentPageSize;
        long total = blogMapper.countRootArticleComments(articleId);
        List<ArticleComment> rootComments = blogMapper.selectRootArticleComments(
                articleId,
                currentPageSize,
                offset
        );
        if (rootComments.isEmpty()) {
            return new PageResponse<>(total, List.of());
        }

        List<Long> rootCommentIds = rootComments.stream().map(ArticleComment::getId).toList();
        Map<Long, List<ArticleCommentResponse>> repliesByRootId = new HashMap<>();
        for (ArticleComment reply : blogMapper.selectArticleCommentReplies(rootCommentIds)) {
            repliesByRootId.computeIfAbsent(reply.getRootCommentId(), ignored -> new ArrayList<>())
                    .add(toCommentResponse(reply, List.of()));
        }

        List<ArticleCommentResponse> list = rootComments.stream()
                .map(comment -> toCommentResponse(
                        comment,
                        repliesByRootId.getOrDefault(comment.getId(), List.of())
                ))
                .toList();
        return new PageResponse<>(total, list);
    }

    @Transactional
    public ArticleResponse createArticle(CreateArticleRequest request) {
        return createArticleWithStatus(request, STATUS_PENDING_REVIEW);
    }

    @Transactional
    public ArticleResponse createDraft(CreateArticleRequest request) {
        return createArticleWithStatus(request, STATUS_DRAFT);
    }

    private ArticleResponse createArticleWithStatus(CreateArticleRequest request, String status) {
        if (request == null) {
            throw new IllegalArgumentException("文章内容不能为空");
        }
        CurrentUserContext currentUser = requireCurrentUser();

        Article article = new Article();
        // 前端只提交内容相关字段，发布时间和统计字段统一由后端生成，避免前端伪造。
        article.setTitle(requireText(request.title(), "文章标题", 160));
        article.setSummary(requireText(request.summary(), "文章摘要", 512));
        article.setCover(optionalUrl(request.cover(), "文章封面", 512));
        article.setCategory(requireText(request.category(), "文章分类", 64));
        article.setContent(requireText(request.content(), "文章正文", null));
        article.setStatus(status);
        article.setAuthorUserId(currentUser.userId());
        article.setDeleted(false);

        LocalDate today = LocalDate.now();
        article.setPublishedAt(today);
        article.setUpdatedAt(today);
        article.setViews(0);
        article.setLikes(0);
        article.setComments(0);

        blogMapper.insertArticle(article);
        bindArticleTags(article.getId(), request);

        return toArticleResponse(blogMapper.selectArticleForManage(article.getId()));
    }

    public PageResponse<ArticleResponse> getMyArticles(int page, int pageSize, String status) {
        CurrentUserContext currentUser = requireCurrentUser();
        String normalizedStatus = optionalStatus(status);
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (currentPage - 1) * currentPageSize;
        long total = blogMapper.countMyArticles(currentUser.userId(), normalizedStatus);
        List<ArticleResponse> list = blogMapper.selectMyArticles(currentUser.userId(), normalizedStatus, currentPageSize, offset)
                .stream()
                .map(this::toArticleResponse)
                .toList();
        return new PageResponse<>(total, list);
    }

    public PageResponse<ArticleResponse> getAdminArticles(int page, int pageSize, String status) {
        requireAdmin();
        String normalizedStatus = optionalStatus(status);
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (currentPage - 1) * currentPageSize;
        long total = blogMapper.countAdminArticles(normalizedStatus);
        List<ArticleResponse> list = blogMapper.selectAdminArticles(normalizedStatus, currentPageSize, offset)
                .stream()
                .map(this::toArticleResponse)
                .toList();
        return new PageResponse<>(total, list);
    }

    @Transactional
    public ArticleResponse updateArticle(Long id, CreateArticleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("文章内容不能为空");
        }
        CurrentUserContext currentUser = requireCurrentUser();
        Article article = getManageArticle(id);
        ensureArticleOwnerOrAdmin(article, currentUser);

        article.setTitle(requireText(request.title(), "文章标题", 160));
        article.setSummary(requireText(request.summary(), "文章摘要", 512));
        article.setCover(optionalUrl(request.cover(), "文章封面", 512));
        article.setCategory(requireText(request.category(), "文章分类", 64));
        article.setContent(requireText(request.content(), "文章正文", null));
        article.setUpdatedAt(LocalDate.now());
        article.setReviewTime(null);
        article.setReviewerUserId(null);
        article.setRejectReason(null);
        article.setStatus(STATUS_PENDING_REVIEW);
        blogMapper.updateArticle(article);
        blogMapper.deleteArticleTags(article.getId());
        bindArticleTags(article.getId(), request);
        return toArticleResponse(blogMapper.selectArticleForManage(article.getId()));
    }

    @Transactional
    public ArticleResponse submitArticle(Long id) {
        CurrentUserContext currentUser = requireCurrentUser();
        Article article = getManageArticle(id);
        ensureArticleOwnerOrAdmin(article, currentUser);
        blogMapper.updateArticleStatusForSubmit(id, LocalDate.now());
        return toArticleResponse(blogMapper.selectArticleForManage(id));
    }

    @Transactional
    public ArticleResponse reviewArticle(Long id, ReviewArticleRequest request) {
        CurrentUserContext currentUser = requireAdmin();
        if (request == null || request.approved() == null) {
            throw new IllegalArgumentException("审核结果不能为空");
        }

        Article article = getManageArticle(id);
        String beforeStatus = article.getStatus();
        boolean approved = request.approved();
        String afterStatus = approved ? STATUS_PUBLISHED : STATUS_REJECTED;
        String rejectReason = approved ? null : requireText(request.rejectReason(), "拒绝原因", 512);
        LocalDateTime reviewTime = LocalDateTime.now();
        blogMapper.reviewArticle(id, afterStatus, currentUser.userId(), reviewTime, rejectReason, LocalDate.now());
        blogMapper.insertArticleReviewRecord(
                id,
                currentUser.userId(),
                beforeStatus,
                afterStatus,
                approved,
                rejectReason,
                reviewTime
        );
        return toArticleResponse(blogMapper.selectArticleForManage(id));
    }

    @Transactional
    public void deleteArticle(Long id) {
        CurrentUserContext currentUser = requireCurrentUser();
        Article article = getManageArticle(id);
        ensureArticleOwnerOrAdmin(article, currentUser);
        blogMapper.softDeleteArticle(id, LocalDateTime.now(), currentUser.userId(), LocalDate.now());
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
                article.getStatus(),
                article.getAuthorUserId(),
                article.getReviewTime(),
                article.getReviewerUserId(),
                article.getRejectReason(),
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

    private Article getManageArticle(Long id) {
        Article article = blogMapper.selectArticleForManage(id);
        if (article == null) {
            throw new NoSuchElementException("Article not found: " + id);
        }
        return article;
    }

    private Article requirePublishedArticle(Long id) {
        Article article = blogMapper.selectArticleById(id);
        if (article == null) {
            throw new NoSuchElementException("Article not found: " + id);
        }
        return article;
    }

    private void applyCommentReplyTarget(ArticleComment comment, Long parentCommentId) {
        if (parentCommentId == null) {
            return;
        }
        if (parentCommentId <= 0) {
            throw new IllegalArgumentException("被回复评论ID不正确");
        }

        ArticleComment parent = blogMapper.selectArticleCommentById(parentCommentId);
        if (parent == null || !comment.getArticleId().equals(parent.getArticleId())) {
            throw new NoSuchElementException("Comment not found: " + parentCommentId);
        }
        comment.setReplyToCommentId(parent.getId());
        comment.setRootCommentId(parent.getRootCommentId() == null ? parent.getId() : parent.getRootCommentId());
    }

    private ArticleCommentResponse toCommentResponse(
            ArticleComment comment,
            List<ArticleCommentResponse> replies
    ) {
        return new ArticleCommentResponse(
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId(),
                comment.getUserName(),
                comment.getUserAvatar(),
                comment.getRootCommentId(),
                comment.getReplyToCommentId(),
                comment.getReplyToUserId(),
                comment.getReplyToUserName(),
                comment.getContent(),
                comment.getCreatedAt(),
                replies
        );
    }

    private CurrentUserContext requireCurrentUser() {
        CurrentUserContext currentUser = ThreadLocalUtil.get();
        if (currentUser == null) {
            throw new IllegalArgumentException("登录已过期，请重新登录");
        }
        return currentUser;
    }

    private CurrentUserContext requireAdmin() {
        CurrentUserContext currentUser = requireCurrentUser();
        if (!ROLE_ADMIN.equals(currentUser.role())) {
            throw new IllegalArgumentException("无权限操作");
        }
        return currentUser;
    }

    private void ensureArticleOwnerOrAdmin(Article article, CurrentUserContext currentUser) {
        if (ROLE_ADMIN.equals(currentUser.role())) {
            return;
        }
        if (!currentUser.userId().equals(article.getAuthorUserId())) {
            throw new IllegalArgumentException("无权限操作");
        }
    }

    private String optionalStatus(String status) {
        String normalized = optionalText(status, "文章状态", 32);
        if (normalized == null) {
            return null;
        }
        if (!Set.of(STATUS_DRAFT, STATUS_PENDING_REVIEW, STATUS_PUBLISHED, STATUS_REJECTED).contains(normalized)) {
            throw new IllegalArgumentException("文章状态不正确");
        }
        return normalized;
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

    private void bindArticleTags(Long articleId, CreateArticleRequest request) {
        Set<Long> tagIds = new LinkedHashSet<>();
        // 兼容前端手动选择已有标签的场景。
        if (request.tagIds() != null) {
            request.tagIds().stream()
                    .filter(id -> id != null && id > 0)
                    .forEach(tagIds::add);
        }

        // 兼容前端直接提交标签名称的场景，不存在的标签会自动创建。
        if (request.tagNames() != null) {
            request.tagNames().stream()
                    .map(name -> optionalText(name, "标签名称", 64))
                    .filter(name -> name != null && !name.isBlank())
                    .map(this::findOrCreateTag)
                    .forEach(tagIds::add);
        }

        // Spring AI 自动提取标签，后续仍复用统一的查找、创建和绑定逻辑。
        extractAiTags(request.title(), request.summary()).stream()
                .map(this::findOrCreateTag)
                .forEach(tagIds::add);

        tagIds.forEach(tagId -> blogMapper.insertArticleTag(articleId, tagId));
    }

    private List<String> extractAiTags(String title, String summary) {
        if ((title == null || title.isBlank()) && (summary == null || summary.isBlank())) {
            return List.of();
        }

        String aiContent = tagExtractorChatClient.prompt()
                .user("""
                        文章标题：%s
                        文章摘要：%s
                        """.formatted(title == null ? "" : title, summary == null ? "" : summary))
                .call()
                .content();
        return parseAiTags(aiContent);
    }

    private List<String> parseAiTags(String aiContent) {
        if (aiContent == null || aiContent.isBlank()) {
            return List.of();
        }

        Set<String> tags = new LinkedHashSet<>();
        for (String value : aiContent.replace("```", "").split("[,，、;；\\r\\n]+")) {
            String tag = normalizeTagName(value.replaceFirst("^\\s*(?:[-*]|\\d+[.)、])\\s*", ""));
            if (tag != null && !tag.isBlank()) {
                tags.add(tag);
            }
            if (tags.size() >= MAX_AUTO_TAG_COUNT) {
                break;
            }
        }
        return List.copyOf(tags);
    }

    private String normalizeTagName(String value) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        if (text.isBlank()) {
            return null;
        }
        if (text.length() > 64) {
            text = text.substring(0, 64);
        }

        // 英文标签首字母统一大写，减少 Spring/spring 这类重复标签。
        if (text.matches("[A-Za-z][A-Za-z0-9+#.-]*")) {
            return text.length() <= 2 ? text.toUpperCase() : text.substring(0, 1).toUpperCase() + text.substring(1);
        }
        return text;
    }

    private Long findOrCreateTag(String name) {
        Long tagId = blogMapper.selectTagIdByName(name);
        if (tagId != null) {
            return tagId;
        }

        Integer maxSortOrder = blogMapper.selectMaxTagSortOrder();
        blogMapper.insertTag(name, (maxSortOrder == null ? 0 : maxSortOrder) + 1);
        return blogMapper.selectTagIdByName(name);
    }

    private String requireText(String value, String fieldName, Integer maxLength) {
        String text = optionalText(value, fieldName, maxLength);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return text;
    }

    private String optionalText(String value, String fieldName, Integer maxLength) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        if (maxLength != null && text.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return text.isEmpty() ? null : text;
    }

    private String optionalUrl(String value, String fieldName, Integer maxLength) {
        String text = optionalText(value, fieldName, maxLength);
        if (text == null) {
            return null;
        }

        try {
            URI uri = new URI(text);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException(fieldName + "必须是 http 或 https 地址");
            }
            return text;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(fieldName + "格式不正确");
        }
    }
}
