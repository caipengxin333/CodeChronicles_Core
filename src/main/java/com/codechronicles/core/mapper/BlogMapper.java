package com.codechronicles.core.mapper;

import com.codechronicles.core.domain.Article;
import com.codechronicles.core.domain.Profile;
import com.codechronicles.core.domain.ProfileLink;
import com.codechronicles.core.domain.Question;
import com.codechronicles.core.domain.Tag;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BlogMapper {

    Profile selectProfile();

    List<String> selectProfileSkills(@Param("profileId") Long profileId);

    List<ProfileLink> selectProfileLinks(@Param("profileId") Long profileId);

    List<Tag> selectTags();

    long countArticles(@Param("tagId") Long tagId);

    int countTags();

    int countQuestions();

    List<Article> selectArticles(
            @Param("tagId") Long tagId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    Article selectArticleById(@Param("id") Long id);

    List<String> selectArticleTags(@Param("articleId") Long articleId);

    List<Question> selectQuestions();

    List<String> selectQuestionTags(@Param("questionId") Long questionId);
}
