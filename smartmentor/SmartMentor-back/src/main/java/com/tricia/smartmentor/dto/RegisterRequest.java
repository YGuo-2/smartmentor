package com.tricia.smartmentor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {
    private String role;

    @NotBlank @Size(min = 3, max = 50, message = "用户名长度应为3-50个字符")
    private String username;

    @NotBlank @Size(min = 6, max = 128, message = "密码长度不能少于6个字符")
    private String password;

    @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @Size(max = 50)
    private String nickname;

    private String grade;

    @Size(max = 100)
    private String school;
}
