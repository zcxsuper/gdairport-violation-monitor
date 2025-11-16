package com.example.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDto {

    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String name;

    @Size(max = 60, message = "密码长度不能超过60个字符")
    private String password;

    @Size(max = 60, message = "确认密码长度不能超过60个字符")
    private String confirmPassword;
}
