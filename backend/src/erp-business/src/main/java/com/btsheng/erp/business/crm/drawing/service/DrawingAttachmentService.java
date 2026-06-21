package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.dto.AttachmentDownloadPayload;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingAttachment;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingAttachmentMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * FR-3-2-2 · 图纸 CAD/CAM 附件挂载（.dxf / .step / .nc / .dwg）
 */
@Service
public class DrawingAttachmentService {

    private static final Set<String> ALLOWED_EXT = Set.of("dxf", "step", "stp", "nc", "dwg", "pdf");

    private final CrmDrawingMapper drawingMapper;
    private final CrmDrawingAttachmentMapper attachmentMapper;
    private final DrawingMinioFileService fileStorage;

    @Autowired
    public DrawingAttachmentService(
            CrmDrawingMapper drawingMapper,
            CrmDrawingAttachmentMapper attachmentMapper,
            DrawingMinioFileService fileStorage) {
        this.drawingMapper = drawingMapper;
        this.attachmentMapper = attachmentMapper;
        this.fileStorage = fileStorage;
    }

    public Result<List<CrmDrawingAttachment>> listAttachments(Long drawingId) {
        if (drawingMapper.selectById(drawingId) == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        return Result.ok(attachmentMapper.selectByDrawingId(drawingId));
    }

    public Result<CrmDrawingAttachment> uploadAttachment(Long drawingId, MultipartFile file, Long operatorUserId) {
        CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail(40001, "FILE_REQUIRED");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return Result.fail(40001, "FILE_NAME_REQUIRED");
        }
        String ext = extension(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.fail(40001, "FILE_TYPE_NOT_ALLOWED");
        }
        if (file.getSize() > 50L * 1024 * 1024) {
            return Result.fail(40001, "FILE_TOO_LARGE");
        }
        try {
            String objectName = "cad/" + drawingId + "/" + UUID.randomUUID() + "." + ext;
            fileStorage.putObject(objectName, file.getInputStream(), file.getSize(), file.getContentType());
            CrmDrawingAttachment row = new CrmDrawingAttachment();
            row.setDrawingId(drawingId);
            row.setFileName(originalName);
            row.setFileType(ext.toUpperCase(Locale.ROOT));
            row.setFilePath(fileStorage.toMinioUri(objectName));
            row.setFileSize(file.getSize());
            row.setUploadedBy(operatorUserId);
            row.setCreatedAt(LocalDateTime.now());
            attachmentMapper.insert(row);
            return Result.ok(row);
        } catch (Exception e) {
            return Result.fail(50001, "UPLOAD_FAILED: " + e.getMessage());
        }
    }

    public Result<AttachmentDownloadPayload> downloadAttachment(Long attachmentId) {
        CrmDrawingAttachment row = attachmentMapper.selectById(attachmentId);
        if (row == null) {
            return Result.fail(40404, "ATTACHMENT_NOT_FOUND");
        }
        try {
            byte[] bytes = fileStorage.readBytes(row.getFilePath());
            String contentType = DrawingMinioFileService.contentTypeForFileName(row.getFileName());
            return Result.ok(new AttachmentDownloadPayload(bytes, row.getFileName(), contentType));
        } catch (Exception e) {
            return Result.fail(50001, "DOWNLOAD_FAILED: " + e.getMessage());
        }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
