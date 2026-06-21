package com.btsheng.erp.platform.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分片上传初始化")
public class FileInitRequest {
    private String fileName;
    private Long fileSize;
    private String md5;
    private String type;
}
