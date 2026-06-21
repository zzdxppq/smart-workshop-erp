package com.btsheng.erp.platform.file.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.file.dto.FileCompleteRequest;
import com.btsheng.erp.platform.file.dto.FileInitRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 内存 + 临时目录分片上传（Spec B.3） */
@Service
public class FileChunkUploadService {

    private static final Path TEMP_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "erp-chunks");

    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    public Result<Map<String, Object>> init(FileInitRequest req) {
        try {
            Files.createDirectories(TEMP_ROOT);
        } catch (IOException e) {
            return Result.fail(50001, "TEMP_DIR_ERROR");
        }
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        UploadSession session = new UploadSession();
        session.fileName = req != null ? req.getFileName() : "file.bin";
        session.fileSize = req != null && req.getFileSize() != null ? req.getFileSize() : 0L;
        session.md5 = req != null ? req.getMd5() : null;
        session.type = req != null ? req.getType() : "attachment";
        session.chunks = new TreeSet<>();
        sessions.put(uploadId, session);
        Map<String, Object> data = new HashMap<>();
        data.put("uploadId", uploadId);
        data.put("chunkSize", 5 * 1024 * 1024);
        return Result.ok(data);
    }

    public Result<Void> uploadChunk(String uploadId, int chunk, MultipartFile file) {
        UploadSession session = sessions.get(uploadId);
        if (session == null) return Result.fail(40401, "UPLOAD_NOT_FOUND");
        try {
            Path dir = TEMP_ROOT.resolve(uploadId);
            Files.createDirectories(dir);
            Path target = dir.resolve("part-" + chunk);
            file.transferTo(target.toFile());
            session.chunks.add(chunk);
            return Result.ok();
        } catch (IOException e) {
            return Result.fail(50001, "CHUNK_SAVE_FAILED");
        }
    }

    public Result<Map<String, Object>> complete(FileCompleteRequest req) {
        if (req == null || req.getUploadId() == null) {
            return Result.fail(40001, "UPLOAD_ID_REQUIRED");
        }
        UploadSession session = sessions.remove(req.getUploadId());
        if (session == null) return Result.fail(40401, "UPLOAD_NOT_FOUND");
        Map<String, Object> data = new HashMap<>();
        data.put("fileUrl", "/platform/files/" + req.getUploadId() + "/download");
        data.put("fileName", session.fileName);
        data.put("md5", session.md5);
        return Result.ok(data);
    }

    private static class UploadSession {
        String fileName;
        long fileSize;
        String md5;
        String type;
        Set<Integer> chunks;
    }
}
