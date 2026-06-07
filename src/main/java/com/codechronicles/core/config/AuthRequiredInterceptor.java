package com.codechronicles.core.config;

import com.codechronicles.core.common.ApiResponse;
import com.codechronicles.core.util.ThreadLocalUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器：公开接口放行，后台写操作必须携带有效 token。
 */
@Component
public class AuthRequiredInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AuthRequiredInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        // Flux 流式响应完成时会触发 ASYNC 二次派发，首次请求已完成登录校验。
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }
        if (!requiresLogin(request)) {
            return true;
        }
        if (ThreadLocalUtil.get() != null) {
            return true;
        }

        writeUnauthorized(response);
        return false;
    }

    private boolean requiresLogin(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        if ("/api/me".equals(path) && "GET".equalsIgnoreCase(method)) {
            return true;
        }
        if ("/api/chat".equals(path) || path.startsWith("/api/chat/")) {
            return true;
        }
        if (path.startsWith("/api/my/") || path.startsWith("/api/admin/")) {
            return true;
        }
        if ("/api/articles".equals(path) && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        if ("/api/articles/drafts".equals(path) && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        return path.startsWith("/api/articles/") && (
                "POST".equalsIgnoreCase(method)
                        ||
                "PUT".equalsIgnoreCase(method)
                        || "PATCH".equalsIgnoreCase(method)
                        || "DELETE".equalsIgnoreCase(method)
        );
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                new ApiResponse<>(401, "登录已过期，请重新登录", null)
        ));
    }
}
