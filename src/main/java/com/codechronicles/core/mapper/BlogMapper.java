package com.codechronicles.core.mapper;

import com.codechronicles.core.domain.Article;
import com.codechronicles.core.domain.ArticleComment;
import com.codechronicles.core.domain.Profile;
import com.codechronicles.core.domain.ProfileLink;
import com.codechronicles.core.domain.Question;
import com.codechronicles.core.domain.Tag;
import com.codechronicles.core.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 博客模块的数据访问接口，具体 SQL 定义在 resources/mapper/BlogMapper.xml。
 */
public interface BlogMapper {

    /**
     * 查询站点唯一的个人资料。
     */
    Profile selectProfile();

    /**
     * 根据当前登录用户绑定的 profileId 查询个人资料。
     */
    Profile selectProfileById(@Param("profileId") Long profileId);

    /**
     * 查询个人技能，按配置的排序字段返回。
     */
    List<String> selectProfileSkills(@Param("profileId") Long profileId);

    /**
     * 查询个人外部链接，按配置的排序字段返回。
     */
    List<ProfileLink> selectProfileLinks(@Param("profileId") Long profileId);

    /**
     * 查询标签列表，并统计每个标签下已发布文章数量。
     */
    List<Tag> selectTags();

    /**
     * 根据手机号查询后台登录用户。
     */
    User selectUserByPhone(@Param("phone") String phone);

    /**
     * 注册时新增个人资料，写入后回填 profile.id。
     */
    int insertProfile(Profile profile);

    /**
     * 注册时新增后台登录用户，写入后回填 user.id。
     */
    int insertUser(User user);

    /**
     * 注册时保存个人技能。
     */
    int insertProfileSkill(
            @Param("profileId") Long profileId,
            @Param("skill") String skill,
            @Param("sortOrder") int sortOrder
    );

    /**
     * 注册时保存个人外部链接。
     */
    int insertProfileLink(
            @Param("profileId") Long profileId,
            @Param("label") String label,
            @Param("url") String url,
            @Param("sortOrder") int sortOrder
    );

    List<String> selectUserRoles(@Param("userId") Long userId);

    /**
     * 统计已发布文章数量；传入 tagId 时只统计该标签下的文章。
     */
    long countArticles(@Param("tagId") Long tagId);

    /**
     * 按作者统计未删除文章；status 不为空时继续按文章状态筛选。
     */
    long countMyArticles(@Param("authorUserId") Long authorUserId, @Param("status") String status);

    long countAdminArticles(@Param("status") String status);

    int countTags();

    int countQuestions();

    /**
     * 统计当前用户在文章下发布的评论数量。
     */
    int countUserComments(@Param("userId") Long userId);

    /**
     * 统计当前用户已发布文章关联过的去重标签数量。
     */
    int countUserTags(@Param("userId") Long userId);

    /**
     * 分页查询已发布文章；传入 tagId 时按标签过滤。
     */
    List<Article> selectArticles(
            @Param("tagId") Long tagId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<Article> selectMyArticles(
            @Param("authorUserId") Long authorUserId,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<Article> selectAdminArticles(
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    Article selectArticleById(@Param("id") Long id);

    Article selectArticleForManage(@Param("id") Long id);

    int incrementArticleViews(@Param("id") Long id);

    int selectArticleLikes(@Param("id") Long id);

    int countArticleLike(@Param("articleId") Long articleId, @Param("userId") Long userId);

    int insertArticleLike(@Param("articleId") Long articleId, @Param("userId") Long userId);

    int deleteArticleLike(@Param("articleId") Long articleId, @Param("userId") Long userId);

    int incrementArticleLikes(@Param("id") Long id);

    int decrementArticleLikes(@Param("id") Long id);

    int insertArticleComment(ArticleComment comment);

    int incrementArticleComments(@Param("id") Long id);

    ArticleComment selectArticleCommentById(@Param("id") Long id);

    long countRootArticleComments(@Param("articleId") Long articleId);

    List<ArticleComment> selectRootArticleComments(
            @Param("articleId") Long articleId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ArticleComment> selectArticleCommentReplies(@Param("rootCommentIds") List<Long> rootCommentIds);

    /**
     * 查询文章关联的标签名称，用于组装文章列表和详情响应。
     */
    List<String> selectArticleTags(@Param("articleId") Long articleId);

    /**
     * 新增文章，写入后通过自增主键回填 article.id。
     */
    int insertArticle(Article article);

    int updateArticle(Article article);

    int deleteArticleTags(@Param("articleId") Long articleId);

    Long selectTagIdByName(@Param("name") String name);

    /**
     * 新增标签，sortOrder 用于前端展示排序。
     */
    int insertTag(@Param("name") String name, @Param("sortOrder") int sortOrder);

    Integer selectMaxTagSortOrder();

    /**
     * 建立文章与标签的多对多关联。
     */
    int insertArticleTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    int updateArticleStatusForSubmit(
            @Param("id") Long id,
            @Param("updatedAt") java.time.LocalDate updatedAt
    );

    int reviewArticle(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("reviewerUserId") Long reviewerUserId,
            @Param("reviewTime") java.time.LocalDateTime reviewTime,
            @Param("rejectReason") String rejectReason,
            @Param("updatedAt") java.time.LocalDate updatedAt
    );

    int insertArticleReviewRecord(
            @Param("articleId") Long articleId,
            @Param("reviewerUserId") Long reviewerUserId,
            @Param("beforeStatus") String beforeStatus,
            @Param("afterStatus") String afterStatus,
            @Param("approved") boolean approved,
            @Param("rejectReason") String rejectReason,
            @Param("reviewTime") java.time.LocalDateTime reviewTime
    );

    int softDeleteArticle(
            @Param("id") Long id,
            @Param("deletedAt") java.time.LocalDateTime deletedAt,
            @Param("deletedBy") Long deletedBy,
            @Param("updatedAt") java.time.LocalDate updatedAt
    );

    List<Question> selectQuestions();

    List<String> selectQuestionTags(@Param("questionId") Long questionId);
}
