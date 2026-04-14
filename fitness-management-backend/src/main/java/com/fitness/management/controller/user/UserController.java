package com.fitness.management.controller.user;

import com.fitness.management.common.Result;
import com.fitness.management.common.ResultCode;
import com.fitness.management.dto.user.UserLoginDto;
import com.fitness.management.dto.user.UserRegisterDto;
import com.fitness.management.exception.BusinessException;
import com.fitness.management.service.UserService;
import com.fitness.management.utils.JwtUtils;
import com.fitness.management.vo.user.UserInfoVo;
import com.fitness.management.vo.user.UserLoginVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册、登录、当前用户信息（REST + 统一 Result）。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final JwtUtils jwtUtils;

    /**
     * 注册新用户。
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDto dto) {
        userService.register(dto);
        return Result.success();
    }

    /**
     * 登录，返回 JWT 与用户信息。
     */
    @PostMapping("/login")
    public Result<UserLoginVo> login(@Valid @RequestBody UserLoginDto dto) {
        return Result.success(userService.login(dto));
    }

    /**
     * 获取当前登录用户资料（请求头：Authorization: Bearer &lt;token&gt;）。
     */
    @GetMapping("/me")
    public Result<UserInfoVo> currentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = resolveUserId(authorization);
        return Result.success(userService.getLoginUserInfo(userId));
    }

    private Long resolveUserId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或令牌缺失");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "令牌无效");
        }
        return userId;
    }
}
