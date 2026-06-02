package com.codechronicles.core.service;

import com.codechronicles.core.domain.Article;
import com.codechronicles.core.domain.Profile;
import com.codechronicles.core.domain.ProfileLink;
import com.codechronicles.core.domain.Question;
import com.codechronicles.core.domain.Tag;
import com.codechronicles.core.dto.ArticleResponse;
import com.codechronicles.core.dto.CreateArticleRequest;
import com.codechronicles.core.dto.LinkResponse;
import com.codechronicles.core.dto.PageResponse;
import com.codechronicles.core.dto.ProfileResponse;
import com.codechronicles.core.dto.QuestionResponse;
import com.codechronicles.core.mapper.BlogMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {

    /**
     * 临时标签提取正则：匹配连续中文词或常见英文技术词。
     */
    private static final Pattern TAG_WORD_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}|[A-Za-z][A-Za-z0-9+#.-]{1,}");
    private static final int MAX_AUTO_TAG_COUNT = 3;

    private final BlogMapper blogMapper;

    public BlogService(BlogMapper blogMapper) {
        this.blogMapper = blogMapper;
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

    public ArticleResponse getArticleDetail(Long id) {
        Article article = blogMapper.selectArticleById(id);
        if (article == null) {
            throw new NoSuchElementException("Article not found: " + id);
        }
        return toArticleResponse(article);
    }

    @Transactional
    public ArticleResponse createArticle(CreateArticleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("文章内容不能为空");
        }

        Article article = new Article();
        // 前端只提交内容相关字段，发布时间和统计字段统一由后端生成，避免前端伪造。
        article.setTitle(requireText(request.title(), "文章标题", 160));
        article.setSummary(requireText(request.summary(), "文章摘要", 512));
        article.setCover(optionalUrl(request.cover(), "文章封面", 512));
        article.setCategory(requireText(request.category(), "文章分类", 64));
        article.setContent(requireText(request.content(), "文章正文", null));
        article.setStatus("PUBLISHED");

        LocalDate today = LocalDate.now();
        article.setPublishedAt(today);
        article.setUpdatedAt(today);
        article.setViews(0);
        article.setLikes(0);
        article.setComments(0);

        blogMapper.insertArticle(article);
        bindArticleTags(article.getId(), request);

        return getArticleDetail(article.getId());
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

        // 当前版本先自动生成临时标签，后续替换为 AI 提取后仍复用后面的入库逻辑。
        extractTemporaryTags(request.title(), request.summary()).stream()
                .map(this::findOrCreateTag)
                .forEach(tagIds::add);

        tagIds.forEach(tagId -> blogMapper.insertArticleTag(articleId, tagId));
    }

    private List<String> extractTemporaryTags(String title, String summary) {
        String text = ((title == null ? "" : title) + " " + (summary == null ? "" : summary)).trim();
        if (text.isBlank()) {
            return List.of();
        }

        // TODO: 后期在这里接入 AI 关键词提取，替换当前从标题和摘要中随机取词的临时实现。
        Set<String> candidates = new LinkedHashSet<>();
        Matcher matcher = TAG_WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            splitTagCandidate(matcher.group()).stream()
                    .map(this::normalizeTagName)
                    .filter(tag -> tag != null && !tag.isBlank())
                    .forEach(candidates::add);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<String> tags = new ArrayList<>(candidates);
        Collections.shuffle(tags);
        int tagCount = ThreadLocalRandom.current().nextInt(1, Math.min(MAX_AUTO_TAG_COUNT, tags.size()) + 1);
        return tags.subList(0, tagCount);
    }

    private List<String> splitTagCandidate(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        String text = value.trim();
        // 长中文串直接入库会像一句话，先切成短块，作为临时标签更适合展示。
        if (text.codePoints().allMatch(this::isChineseCodePoint) && text.length() > 4) {
            List<String> chunks = new ArrayList<>();
            for (int index = 0; index < text.length(); index += 4) {
                chunks.add(text.substring(index, Math.min(index + 4, text.length())));
            }
            return chunks;
        }
        return List.of(text);
    }

    private boolean isChineseCodePoint(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN;
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
