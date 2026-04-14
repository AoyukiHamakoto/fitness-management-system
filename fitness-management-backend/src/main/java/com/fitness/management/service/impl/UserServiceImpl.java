package com.fitness.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.management.common.ResultCode;
import com.fitness.management.dto.user.UserLoginDto;
import com.fitness.management.dto.user.UserRegisterDto;
import com.fitness.management.entity.User;
import com.fitness.management.exception.BusinessException;
import com.fitness.management.mapper.UserMapper;
import com.fitness.management.service.UserService;
import com.fitness.management.utils.JwtUtils;
import com.fitness.management.vo.user.UserInfoVo;
import com.fitness.management.vo.user.UserLoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户领域服务：注册、登录、资料查询（对齐论文用户管理用例）。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final int LOGIN_FAIL_MAX = 5;
    private static final Duration LOGIN_FAIL_WINDOW = Duration.ofMinutes(15);

    private static final int USER_STATUS_NORMAL = 1;

    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDto dto) {
        String username = dto.getUsername().trim();
        String phone = dto.getPhone().trim();

        if (lambdaQuery().eq(User::getUsername, username).exists()) {
            throw new BusinessException("用户名已存在");
        }
        if (lambdaQuery().eq(User::getPhone, phone).exists()) {
            throw new BusinessException("手机号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : username);
        user.setStatus(USER_STATUS_NORMAL);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        try {
            if (!save(user)) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "注册失败，请稍后重试");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("用户名或手机号已存在");
        }
    }

    @Override
    public UserLoginVo login(UserLoginDto dto) {
        String username = dto.getUsername().trim();
        assertNotLoginLocked(username);

        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) {
            onLoginFail(username);
            throw new BusinessException("用户名或密码错误");
        }
        if (!Objects.equals(user.getStatus(), USER_STATUS_NORMAL)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            onLoginFail(username);
            throw new BusinessException("用户名或密码错误");
        }

        clearLoginFail(username);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        UserLoginVo vo = new UserLoginVo();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setUser(toUserInfoVo(user));
        return vo;
    }

    @Override
    public UserInfoVo getLoginUserInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserInfoVo(user);
    }

    private UserInfoVo toUserInfoVo(User user) {
        UserInfoVo vo = new UserInfoVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        return vo;
    }

    private void assertNotLoginLocked(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val != null) {
            long fails = Long.parseLong(val);
            if (fails >= LOGIN_FAIL_MAX) {
                throw new BusinessException(ResultCode.FORBIDDEN, "登录失败次数过多，请15分钟后再试");
            }
        }
    }

    private void onLoginFail(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, LOGIN_FAIL_WINDOW);
        }
    }

    private void clearLoginFail(String username) {
        stringRedisTemplate.delete(LOGIN_FAIL_KEY_PREFIX + username);
    }
}
