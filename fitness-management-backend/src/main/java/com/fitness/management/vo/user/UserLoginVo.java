package com.fitness.management.vo.user;

import lombok.Data;

/**
 * 登录成功返回：访问令牌 + 当前用户基本信息。
 */
@Data
public class UserLoginVo {

    /** JWT 访问令牌 */
    private String token;

    /** 令牌类型，便于前端统一组装 Authorization 头 */
    private String tokenType = "Bearer";

    /** 当前登录用户摘要信息 */
    private UserInfoVo user;
}
