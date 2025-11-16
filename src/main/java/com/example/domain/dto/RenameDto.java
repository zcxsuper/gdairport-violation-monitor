package com.example.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameDto {

    @NotNull(message = "文件ID不能为空")
    private Long id;

    @NotBlank(message = "新文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255个字符")
    private String newName;
}

