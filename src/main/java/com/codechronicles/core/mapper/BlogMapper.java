package com.codechronicles.core.mapper;

import com.codechronicles.core.domain.Article;
import com.codechronicles.core.domain.Profile;
import com.codechronicles.core.domain.ProfileLink;
import com.codechronicles.core.domain.Question;
import com.codechronicles.core.domain.Tag;
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
     * 统计已发布文章数量；传入 tagId 时只统计该标签下的文章。
     */
    long countArticles(@Param("tagId") Long tagId);

    int countTags();

    int countQuestions();

    /**
     * 分页查询已发布文章；传入 tagId 时按标签过滤。
     */
    List<Article> selectArticles(
            @Param("tagId") Long tagId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    Article selectArticleById(@Param("id") Long id);

    /**
     * 查询文章关联的标签名称，用于组装文章列表和详情响应。
     */
    List<String> selectArticleTags(@Param("articleId") Long articleId);

    /**
     * 新增文章，写入后通过自增主键回填 article.id。
     */
    int insertArticle(Article article);

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

    List<Question> selectQuestions();

    List<String> selectQuestionTags(@Param("questionId") Long questionId);
}
