package com.example.domain.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDto {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(max = 60, message = "密码长度不能超过60个字符")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Size(max = 60, message = "确认密码长度不能超过60个字符")
    private String confirmPassword;
}
