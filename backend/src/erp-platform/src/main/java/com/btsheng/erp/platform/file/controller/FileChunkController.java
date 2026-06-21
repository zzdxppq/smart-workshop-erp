package com.btsheng.erp.platform.file.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.file.dto.FileCompleteRequest;
import com.btsheng.erp.platform.file.dto.FileInitRequest;
import com.btsheng.erp.platform.file.service.FileChunkUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Spec B.3 分片上传 · /files/init|chunk|complete */
@Tag(name = "E1-File-Chunk", description = "分片上传")
@RestController
@RequestMapping("/files")
public class FileChunkController {

    private final FileChunkUploadService uploadService;

    @Autowired
    public FileChunkController(FileChunkUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/init")
    @Operation(summary = "初始化分片上传")
    public Result<Map<String, Object>> init(@RequestBody FileInitRequest req) {
        return uploadService.init(req);
    }

    @PostMapping("/chunk")
    @Operation(summary = "上传分片")
    public Result<Void> chunk(@RequestParam String uploadId,
                              @RequestParam int chunk,
                              @RequestParam("chunk") MultipartFile file) {
        return uploadService.uploadChunk(uploadId, chunk, file);
    }

    @PostMapping("/complete")
    @Operation(summary = "合并完成")
    public Result<Map<String, Object>> complete(@RequestBody FileCompleteRequest req) {
        return uploadService.complete(req);
    }
}
