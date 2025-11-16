package com.example.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveFileDto {

    @NotNull(message = "文件ID不能为空")
    private Long id;

    /** 新的父目录ID（null 表示移动到根目录） */
    private Long newParentId;
}

