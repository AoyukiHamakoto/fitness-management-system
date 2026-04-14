package com.fitness.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("user")
public class User {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 密码（加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
