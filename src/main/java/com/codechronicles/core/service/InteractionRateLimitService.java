package com.codechronicles.core.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 内容互动限流，使用 Redis 原子写入保证多实例部署下规则仍然生效。
 */
@Service
public class InteractionRateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Duration likeInterval;
    private final Duration commentInterval;

    public InteractionRateLimitService(
            StringRedisTemplate redisTemplate,
            @Value("${blog.redis.key-prefix}") String keyPrefix,
            @Value("${blog.interaction.like-interval:3s}") Duration likeInterval,
            @Value("${blog.interaction.comment-interval:10s}") Duration commentInterval
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.likeInterval = likeInterval;
        this.commentInterval = commentInterval;
    }

    public void checkLike(Long userId, Long articleId) {
        check("like", userId, articleId, likeInterval, "操作过于频繁，请稍后再试");
    }

    public void checkComment(Long userId, Long articleId) {
        check("comment", userId, articleId, commentInterval, "评论过于频繁，请稍后再试");
    }

    private void check(
            String action,
            Long userId,
            Long articleId,
            Duration interval,
            String message
    ) {
        String key = keyPrefix + ":interaction:" + action + ":" + userId + ":" + articleId;
        Boolean allowed = redisTemplate.opsForValue().setIfAbsent(key, "1", interval);
        if (!Boolean.TRUE.equals(allowed)) {
            throw new IllegalArgumentException(message);
        }
    }
}
