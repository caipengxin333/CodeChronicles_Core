package com.codechronicles.core.dto;

import java.util.List;

public record ProfileResponse(
        String nickname,
        String name,
        String account,
        String avatar,
        String bio,
        String role,
        String location,
        Integer followers,
        Integer articleCount,
        Integer articles,
        Integer tagCount,
        Integer questionCount,
        List<String> skills,
        List<LinkResponse> links
) {
}
