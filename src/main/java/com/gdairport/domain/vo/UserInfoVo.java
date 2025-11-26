package com.gdairport.domain.vo;

import com.gdairport.enums.UserRoleEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVo {

    private Long id;

    private String name;

    private String email;

    private UserRoleEnum auth;

    private LocalDateTime created;

    private LocalDateTime updated;
}
