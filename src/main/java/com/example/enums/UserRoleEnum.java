package com.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    UserRoleEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserRoleEnum fromCode(int code) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知角色 code: " + code);
    }
}
