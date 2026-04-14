package com.fitness.management.vo.user;

import lombok.Data;

/**
 * 用户信息视图（不含密码等敏感字段）。
 */
@Data
public class UserInfoVo {

    private Long id;

    private String username;

    private String phone;

    private String nickname;

    /** 状态：1正常 0禁用 */
    private Integer status;
}
