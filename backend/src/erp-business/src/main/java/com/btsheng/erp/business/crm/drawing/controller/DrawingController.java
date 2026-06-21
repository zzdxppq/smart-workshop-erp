package com.btsheng.erp.business.crm.drawing.controller;

import com.btsheng.erp.business.crm.drawing.dto.DrawingCreateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingQueryRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingReleaseRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingUpdateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingVersionRequest;
import com.btsheng.erp.business.crm.drawing.dto.AttachmentDownloadPayload;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingAttachment;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion;
import com.btsheng.erp.business.crm.drawing.service.DrawingAttachmentService;
import com.btsheng.erp.business.crm.drawing.service.DrawingPdfExportService;
import com.btsheng.erp.business.crm.drawing.service.DrawingService;
import com.btsheng.erp.business.crm.drawing.service.DrawingUploadService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.7 · 图纸 Controller
 *
 * 8 端点：create / get / update / list / addVersion / release / archive / export
 * @Tag("E3-Drawing")
 * V1.3.7 UI 红线 5：审计 + > 20万 二次密码 + 黑名单优先
 */
@RestController
@RequestMapping("/drawings")
@Tag(name = "E3-Drawing", description = "Epic 3 图纸与物料（Story 1.7）")
public class DrawingController {

    private final DrawingService drawingService;
    private final DrawingPdfExportService pdfExportService;
    private final DrawingUploadService uploadService;
    private final DrawingAttachmentService attachmentService;

    @Autowired
    public DrawingController(DrawingService drawingService,
                              DrawingPdfExportService pdfExportService,
                              DrawingUploadService uploadService,
                              DrawingAttachmentService attachmentService) {
        this.drawingService = drawingService;
        this.pdfExportService = pdfExportService;
        this.uploadService = uploadService;
        this.attachmentService = attachmentService;
    }

    @Operation(summary = "1. 创建图纸（AC-3.1.1）")
    @PostMapping
    public Result<CrmDrawing> create(@Valid @RequestBody DrawingCreateRequest req,
                                      @RequestParam Long operatorUserId) {
        return drawingService.createDrawing(req, operatorUserId);
    }

    @Operation(summary = "2. 查询详情（AC-3.1.1）")
    @GetMapping("/{id}")
    public Result<CrmDrawing> get(@PathVariable Long id) {
        return drawingService.getDrawing(id);
    }

    @Operation(summary = "3. 修改图纸（AC-3.1.1 · DRAFT）")
    @PutMapping("/{id}")
    public Result<CrmDrawing> update(@PathVariable Long id,
                                      @Valid @RequestBody DrawingUpdateRequest req,
                                      @RequestParam Long operatorUserId) {
        return drawingService.updateDrawing(id, req, operatorUserId);
    }

    @Operation(summary = "4. 列表查询 6 维过滤（AC-3.1.4）")
    @GetMapping
    public Result<Map<String, Object>> list(DrawingQueryRequest query) {
        return drawingService.listDrawings(query);
    }

    @Operation(summary = "5. 新增版本（AC-3.1.2 · v1→v2→v3 严格递增）")
    @PostMapping("/{id}/versions")
    public Result<CrmDrawingVersion> addVersion(@PathVariable Long id,
                                                 @Valid @RequestBody DrawingVersionRequest req,
                                                 @RequestParam Long operatorUserId) {
        return drawingService.addVersion(id, req, operatorUserId);
    }

    @Operation(summary = "5b. 版本历史列表（FR-3-1-2）")
    @GetMapping("/{id}/versions")
    public Result<List<CrmDrawingVersion>> listVersions(@PathVariable Long id) {
        return drawingService.listVersions(id);
    }

    @Operation(summary = "CAD/CAM 附件列表（FR-3-2-2）")
    @GetMapping("/{id}/attachments")
    public Result<List<CrmDrawingAttachment>> listAttachments(@PathVariable Long id) {
        return attachmentService.listAttachments(id);
    }

    @Operation(summary = "CAD/CAM 附件上传（FR-3-2-2 · dxf/step/nc/dwg）")
    @PostMapping("/{id}/attachments")
    public Result<CrmDrawingAttachment> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1001") Long operatorUserId) {
        return attachmentService.uploadAttachment(id, file, operatorUserId);
    }

    @Operation(summary = "CAD/CAM 附件下载（MinIO 文件流）")
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
        Result<AttachmentDownloadPayload> result = attachmentService.downloadAttachment(attachmentId);
        if (result.getCode() != 0 || result.getData() == null) {
            return ResponseEntity.status(404).body(
                    (result.getMessage() != null ? result.getMessage() : "NOT_FOUND").getBytes());
        }
        AttachmentDownloadPayload payload = result.getData();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                payload.getContentType() != null ? payload.getContentType() : "application/octet-stream"));
        String fileName = payload.getFileName() != null ? payload.getFileName() : ("cad-" + attachmentId);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(payload.getData().length);
        return ResponseEntity.ok().headers(headers).body(payload.getData());
    }

    @Operation(summary = "6. 发布图纸（AC-3.1.3 · 4 阈值 + 二次密码）")
    @PostMapping("/{id}/release")
    public Result<CrmDrawing> release(@PathVariable Long id,
                                       @RequestBody(required = false) DrawingReleaseRequest req,
                                       @RequestParam Long operatorUserId) {
        if (req == null) req = new DrawingReleaseRequest();
        return drawingService.releaseDrawing(id, req, operatorUserId);
    }

    @Operation(summary = "7. 归档（AC-3.1.3 · RELEASED→ARCHIVED）")
    @PostMapping("/{id}/archive")
    public Result<CrmDrawing> archive(@PathVariable Long id, @RequestParam Long operatorUserId) {
        return drawingService.archiveDrawing(id, operatorUserId);
    }

    @Operation(summary = "上传图纸文件并快速建档（AC-3.1.1 · 报价「上传新图纸」）")
    @PostMapping("/upload")
    public Result<CrmDrawing> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(required = false) String title,
                                      @RequestParam(required = false) String materialGrade,
                                      @RequestParam(required = false) String specSize,
                                      @RequestParam(required = false) String customerDrawingNo,
                                      @RequestParam(required = false) java.math.BigDecimal unitWeight,
                                      @RequestParam(required = false) Long operatorUserId,
                                      @RequestParam(defaultValue = "true") boolean releaseAfter) {
        long uid = com.btsheng.erp.core.web.SalesDataScopeHelper.requireOperatorUserId(
                operatorUserId != null && operatorUserId > 0 ? operatorUserId : 1001L);
        return uploadService.uploadAndCreate(file, title, materialGrade, specSize,
                customerDrawingNo, unitWeight, uid, releaseAfter);
    }

    @Operation(summary = "8. 导出 PDF（AC-3.1.4 · 含签字扫描件解密嵌入 · 1h 缓存）")
    @GetMapping("/export/{id}")
    public ResponseEntity<byte[]> export(@PathVariable Long id,
                                          @RequestParam(defaultValue = "pdf") String format) {
        Result<byte[]> result = pdfExportService.exportPdf(id, format);
        if (result.getCode() != 0) {
            return ResponseEntity.status(500).body(("{\"code\":" + result.getCode()
                + ",\"message\":\"" + result.getMessage() + "\"}").getBytes());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
            "drawing-" + id + "-" + System.currentTimeMillis() + "." + format);
        return ResponseEntity.ok().headers(headers).body(result.getData());
    }
}
