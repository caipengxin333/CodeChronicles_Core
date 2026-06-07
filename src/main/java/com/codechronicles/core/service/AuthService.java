package com.codechronicles.core.service;

import com.codechronicles.core.common.CurrentUserContext;
import com.codechronicles.core.domain.Profile;
import com.codechronicles.core.domain.User;
import com.codechronicles.core.dto.LoginRequest;
import com.codechronicles.core.dto.LoginResponse;
import com.codechronicles.core.dto.LinkResponse;
import com.codechronicles.core.dto.MeResponse;
import com.codechronicles.core.dto.RegisterLinkRequest;
import com.codechronicles.core.dto.RegisterRequest;
import com.codechronicles.core.dto.RegisterResponse;
import com.codechronicles.core.mapper.BlogMapper;
import com.codechronicles.core.util.ThreadLocalUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录认证服务，负责验证码校验、用户校验和 token 生成。
 */
@Service
public class AuthService {

    private final BlogMapper blogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthRedisService authRedisService;

    public AuthService(
            BlogMapper blogMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthRedisService authRedisService
    ) {
        this.blogMapper = blogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authRedisService = authRedisService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("登录参数不能为空");
        }

        String phone = requireText(request.phone(), "手机号");
        String password = requireText(request.password(), "密码");
        String captchaKey = requireText(request.captchaKey(), "验证码标识");
        String captcha = requireText(request.captcha(), "验证码");

        if (!phone.matches("\\d{11}")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("密码必须包含大写字母、小写字母和数字");
        }

        validateCaptcha(captchaKey, captcha);

        User user = blogMapper.selectUserByPhone(phone);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("手机号或密码不正确");
        }

        String token = jwtService.generateToken(user);
        CurrentUserContext currentUser = new CurrentUserContext(
                user.getId(),
                user.getPhone(),
                user.getProfileId(),
                user.getRole(),
                token
        );
        authRedisService.saveToken(currentUser);
        ThreadLocalUtil.set(currentUser);
        return new LoginResponse(token);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("注册参数不能为空");
        }

        String phone = requireText(request.phone(), "手机号");
        String password = requireText(request.password(), "密码");
        String nickname = requireText(request.nickname(), "昵称");
        if (!phone.matches("\\d{11}")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("密码必须包含大写字母、小写字母和数字");
        }
        if (nickname.length() > 64) {
            throw new IllegalArgumentException("昵称不能超过 64 个字符");
        }
        if (blogMapper.selectUserByPhone(phone) != null) {
            throw new IllegalArgumentException("手机号已注册");
        }

        Profile profile = new Profile();
        profile.setNickname(nickname);
        profile.setAccount(generateAccount(phone));
        profile.setAvatar(optionalUrl(request.avatar(), "头像", 512));
        profile.setBio(optionalText(request.bio(), "个人简介", 512));
        profile.setRole(optionalText(request.role(), "职业或技术方向", 128));
        profile.setLocation(optionalText(request.location(), "所在地", 128));
        profile.setFollowers(0);
        blogMapper.insertProfile(profile);

        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setProfileId(profile.getId());
        user.setRole("USER");
        blogMapper.insertUser(user);

        saveProfileSkills(profile.getId(), request);
        saveProfileLinks(profile.getId(), request);
        return new RegisterResponse(user.getId(), profile.getId(), phone, profile.getAccount());
    }

    /**
     * 组装当前登录用户资料和本人内容统计。
     * 文章、标签、评论均按当前用户计算，没有关联数据时数据库 COUNT 返回 0。
     */
    public MeResponse getCurrentUserInfo() {
        // 用户上下文由 JWT 过滤器写入 ThreadLocal，避免信任前端传入的用户 ID。
        CurrentUserContext currentUser = ThreadLocalUtil.get();
        if (currentUser == null) {
            throw new IllegalArgumentException("登录已过期，请重新登录");
        }

        // 用户表只保存登录信息，昵称、头像、简介等展示资料从绑定的 profile 查询。
        Profile profile = blogMapper.selectProfileById(currentUser.profileId());
        if (profile == null) {
            throw new NoSuchElementException("Profile not found: " + currentUser.profileId());
        }

        List<String> skills = blogMapper.selectProfileSkills(profile.getId());
        List<LinkResponse> links = blogMapper.selectProfileLinks(profile.getId()).stream()
                .map(link -> new LinkResponse(link.getLabel(), link.getUrl()))
                .toList();
        int publishedArticleCount = Math.toIntExact(
                blogMapper.countMyArticles(currentUser.userId(), "PUBLISHED")
        );

        // articleCount 保留给旧前端，publishedArticleCount 提供更明确的字段语义。
        return new MeResponse(
                currentUser.userId(),
                currentUser.phone(),
                profile.getNickname(),
                profile.getNickname(),
                profile.getAvatar(),
                profile.getRole(),
                profile.getBio(),
                profile.getLocation(),
                profile.getFollowers(),
                publishedArticleCount,
                publishedArticleCount,
                // 标签数是本人已发布文章关联标签的去重数量，不是全站标签总数。
                blogMapper.countUserTags(currentUser.userId()),
                // questionCount 为兼容前端保留原字段名，实际统计本人在 article_comment 中的评论数。
                blogMapper.countUserComments(currentUser.userId()),
                skills,
                links
        );
    }

    private void validateCaptcha(String captchaKey, String inputCaptcha) {
        String captcha = authRedisService.getCaptcha(captchaKey);
        authRedisService.removeCaptcha(captchaKey);

        if (captcha == null) {
            throw new IllegalArgumentException("验证码已过期");
        }
        if (!inputCaptcha.toUpperCase(Locale.ROOT).equals(captcha.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("验证码错误");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String optionalText(String value, String fieldName, Integer maxLength) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (maxLength != null && text.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return text;
    }

    private String optionalUrl(String value, String fieldName, Integer maxLength) {
        String text = optionalText(value, fieldName, maxLength);
        if (text == null) {
            return null;
        }

        try {
            URI uri = new URI(text);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException(fieldName + "必须是 http 或 https 地址");
            }
            return text;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(fieldName + "格式不正确");
        }
    }

    private boolean isValidPassword(String password) {
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUppercase && hasLowercase && hasDigit;
    }

    private String generateAccount(String phone) {
        String suffix = phone.substring(phone.length() - 4);
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "u" + suffix + "_" + random;
    }

    private void saveProfileSkills(Long profileId, RegisterRequest request) {
        if (request.skills() == null) {
            return;
        }

        for (int index = 0; index < request.skills().size(); index++) {
            String skill = optionalText(request.skills().get(index), "技能名称", 64);
            if (skill != null) {
                blogMapper.insertProfileSkill(profileId, skill, index);
            }
        }
    }

    private void saveProfileLinks(Long profileId, RegisterRequest request) {
        if (request.links() == null) {
            return;
        }

        for (int index = 0; index < request.links().size(); index++) {
            RegisterLinkRequest link = request.links().get(index);
            if (link == null) {
                continue;
            }

            String label = optionalText(link.label(), "链接名称", 64);
            String url = optionalUrl(link.url(), "链接地址", 512);
            if (label != null && url != null) {
                blogMapper.insertProfileLink(profileId, label, url, index);
            }
        }
    }
}
