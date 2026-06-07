package com.codechronicles.core.service;

import com.codechronicles.core.common.CurrentUserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 认证相关 Redis 存储服务，集中管理 key 前缀、验证码和登录 token。
 */
@Service
public class AuthRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Duration captchaExpiration;
    private final Duration tokenExpiration;

    public AuthRedisService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${blog.redis.key-prefix}") String keyPrefix,
            @Value("${blog.redis.captcha-expiration}") Duration captchaExpiration,
            @Value("${blog.jwt.expiration-ms}") long tokenExpirationMs
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
        this.captchaExpiration = captchaExpiration;
        this.tokenExpiration = Duration.ofMillis(tokenExpirationMs);
    }

    public String saveCaptcha(String captcha) {
        String captchaKey = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(captchaKey(captchaKey), captcha, captchaExpiration);
        return captchaKey;
    }

    public String getCaptcha(String captchaKey) {
        return redisTemplate.opsForValue().get(captchaKey(captchaKey));
    }

    public void removeCaptcha(String captchaKey) {
        redisTemplate.delete(captchaKey(captchaKey));
    }

    public void saveToken(CurrentUserContext currentUser) {
        try {
            redisTemplate.opsForValue().set(
                    tokenKey(currentUser.token()),
                    objectMapper.writeValueAsString(currentUser),
                    tokenExpiration
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("登录信息序列化失败", exception);
        }
    }

    public CurrentUserContext getTokenContext(String token) {
        String value = redisTemplate.opsForValue().get(tokenKey(token));
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, CurrentUserContext.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("登录信息反序列化失败", exception);
        }
    }

    /**
     * 主动退出登录时删除 Redis 中的 token 登录态，让尚未过期的 JWT 立即失效。
     */
    public void removeToken(String token) {
        redisTemplate.delete(tokenKey(token));
    }

    private String captchaKey(String captchaKey) {
        return keyPrefix + ":captcha:" + captchaKey;
    }

    private String tokenKey(String token) {
        return keyPrefix + ":login:token:" + token;
    }
}
