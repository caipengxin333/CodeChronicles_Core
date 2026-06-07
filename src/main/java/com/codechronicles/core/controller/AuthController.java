package com.codechronicles.core.controller;

import com.codechronicles.core.common.ApiResponse;
import com.codechronicles.core.dto.CaptchaResponse;
import com.codechronicles.core.dto.LoginRequest;
import com.codechronicles.core.dto.LoginResponse;
import com.codechronicles.core.dto.MeResponse;
import com.codechronicles.core.dto.RegisterRequest;
import com.codechronicles.core.dto.RegisterResponse;
import com.codechronicles.core.service.AuthRedisService;
import com.codechronicles.core.service.AuthService;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台登录认证接口，包含图形验证码和登录。
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final AuthRedisService authRedisService;

    public AuthController(AuthService authService, AuthRedisService authRedisService) {
        this.authService = authService;
        this.authRedisService = authRedisService;
    }

    /**
     * 生成 4 位字母验证码，验证码文本存入 Redis，图片以 base64 data URL 返回。
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() throws IOException {
        SpecCaptcha captcha = new SpecCaptcha(120, 40, 4);
        captcha.setCharType(Captcha.TYPE_ONLY_CHAR);
        String captchaKey = authRedisService.saveCaptcha(captcha.text());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        captcha.out(outputStream);
        String image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return ApiResponse.success(new CaptchaResponse(captchaKey, image));
    }

    /**
     * 校验验证码、手机号和密码，成功后返回 2 小时有效的 JWT。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    /**
     * 注册新用户。注册接口不需要登录态，后端会创建 profile 并加密保存用户密码。
     */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    /**
     * 获取当前登录用户信息及本人内容统计。
     * JWT 过滤器会先从 Redis 恢复用户上下文，拦截器负责拒绝未登录请求。
     */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.success(authService.getCurrentUserInfo());
    }

    /**
     * 退出登录时删除 Redis 登录态，使当前 JWT 在服务端立即失效。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null) {
            authRedisService.removeToken(token);
        }
        return ApiResponse.success("退出成功", null);
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
