package com.fitness.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fitness.management.dto.user.UserLoginDto;
import com.fitness.management.dto.user.UserRegisterDto;
import com.fitness.management.entity.User;
import com.fitness.management.vo.user.UserInfoVo;
import com.fitness.management.vo.user.UserLoginVo;

public interface UserService extends IService<User> {

    /**
     * 用户注册：校验用户名/手机号唯一，密码 BCrypt 加密后入库。
     */
    void register(UserRegisterDto dto);

    /**
     * 用户登录：校验账号状态与密码，签发 JWT；失败次数由 Redis 累计限流。
     */
    UserLoginVo login(UserLoginDto dto);

    /**
     * 根据主键查询可展示的用户信息（供登录态接口使用）。
     */
    UserInfoVo getLoginUserInfo(Long userId);
}
