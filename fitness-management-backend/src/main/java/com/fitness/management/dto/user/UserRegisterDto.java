package com.fitness.management.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册入参（论文用例：账号、手机号唯一性校验）。
 * <p>校验注解使用 Jakarta Bean Validation，与 Spring Boot 3.x 一致。</p>
 */
@Data
public class UserRegisterDto {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度为3-50个字符")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度为6-50个字符")
    private String password;

    /** 昵称，可选；未填时服务端可用用户名作为默认昵称 */
    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;
}
