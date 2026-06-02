package com.codechronicles.core.dto;

import java.util.List;

/**
 * 个人资料响应，包含基础信息、统计数据、技能和外部链接。
 */
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
