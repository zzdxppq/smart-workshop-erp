package com.btsheng.erp.platform.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分片上传完成")
public class FileCompleteRequest {
    private String uploadId;
    private String md5;
}
