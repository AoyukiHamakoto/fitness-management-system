package com.fitness.management.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录入参（论文用例：用户名 + 密码）。
 */
@Data
public class UserLoginDto {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不合法")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 50, message = "密码长度不合法")
    private String password;
}
