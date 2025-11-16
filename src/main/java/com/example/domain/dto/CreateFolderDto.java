package com.example.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFolderDto {

    /** 文件夹名称，数据库字段类型：varchar(255) */
    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 255, message = "文件夹名称长度不能超过255个字符")
    private String name;

    /** 父目录ID（null 表示根目录） */
    private Long parentId;
}