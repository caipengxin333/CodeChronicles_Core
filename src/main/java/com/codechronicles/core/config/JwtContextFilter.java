package com.codechronicles.core.config;

import com.codechronicles.core.common.CurrentUserContext;
import com.codechronicles.core.service.AuthRedisService;
import com.codechronicles.core.service.JwtService;
import com.codechronicles.core.util.ThreadLocalUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从 Authorization: Bearer token 中解析用户信息，写入 ThreadLocal，供后续业务代码使用。
 */
@Component
public class JwtContextFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    public static final String TOKEN_INVALID_ATTRIBUTE = "cc.tokenInvalid";
    public static final String TOKEN_PRESENT_ATTRIBUTE = "cc.tokenPresent";

    private final JwtService jwtService;
    private final AuthRedisService authRedisService;

    public JwtContextFilter(JwtService jwtService, AuthRedisService authRedisService) {
        this.jwtService = jwtService;
        this.authRedisService = authRedisService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null) {
                request.setAttribute(TOKEN_PRESENT_ATTRIBUTE, true);
                loadCurrentUser(token, request);
            }
            filterChain.doFilter(request, response);
        } finally {
            ThreadLocalUtil.remove();
        }
    }

    private void loadCurrentUser(String token, HttpServletRequest request) {
        try {
            jwtService.parseToken(token);
            CurrentUserContext currentUser = authRedisService.getTokenContext(token);
            if (currentUser != null) {
                ThreadLocalUtil.set(currentUser);
            } else {
                request.setAttribute(TOKEN_INVALID_ATTRIBUTE, true);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Token 过期、签名错误或格式不正确时，不让公开接口因为旧 token 返回 500。
            request.setAttribute(TOKEN_INVALID_ATTRIBUTE, true);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

}
