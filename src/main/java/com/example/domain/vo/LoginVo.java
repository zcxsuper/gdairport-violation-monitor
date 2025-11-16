package com.example.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVo {

    private Long userId;

    private String token;
}
