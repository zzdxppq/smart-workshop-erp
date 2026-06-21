package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.authz.DrawingAuthz;
import com.btsheng.erp.business.crm.drawing.dto.DrawingPermissionDTO;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.9 Sprint 12 Story 12.1 · 图纸预览 / 下载 服务
 *
 * <p>2 个流式端点：
 * <ul>
 *   <li>previewPDF：鉴�?canView �?DrawingPdfExportService �?application/pdf �?/li>
 *   <li>downloadOriginal：鉴�?canDownload（仅 ENGINEER�?�?application/octet-stream �?/li>
 * </ul>
 *
 * <p>ACL 拒绝 100% 记录审计日志（@AuditLog action=DRAWING_ACL_DENY�? *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Service
public class DrawingPreviewService {

    private static final Logger log = LoggerFactory.getLogger(DrawingPreviewService.class);

    private final CrmDrawingMapper drawingMapper;
    private final DrawingPdfExportService pdfExportService;
    private final DrawingAuthz drawingAuthz;

    @Autowired
    public DrawingPreviewService(CrmDrawingMapper drawingMapper,
                                  DrawingPdfExportService pdfExportService,
                                  DrawingAuthz drawingAuthz) {
        this.drawingMapper = drawingMapper;
        this.pdfExportService = pdfExportService;
        this.drawingAuthz = drawingAuthz;
    }

    /**
     * 图纸预览（带 ACL · 自动审计�?     *
     * <p>错误码统一 40304（评审建议）�?     * <ul>
     *   <li>FINANCE �?"FINANCE 角色无图纸权�?</li>
     *   <li>SALES 不关联订�?�?"该图纸未关联您的订单"</li>
     *   <li>OPERATOR 工序不关�?�?"当前工序未关联该图纸"</li>
     *   <li>PURCHASER/WAREHOUSE/QC 类似</li>
     * </ul>
     */
    @Transactional
    @AuditLog(module = "drawing_acl", action = "drawing.preview")
    public Result<byte[]> previewPDF(Long drawingId, String resolution, Authentication auth) {
        if (drawingId == null) return Result.fail(40001, "DRAWING_ID_REQUIRED");

        // 1. 图纸存在性校�?
            CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) return Result.fail(40401, "DRAWING_NOT_FOUND");

        // 2. 归档状态默认不可预�?
            if ("ARCHIVED".equals(drawing.getStatus()) || "OBSOLETE".equals(drawing.getStatus())) {
            return Result.fail(41001, "DRAWING_ARCHIVED");
        }

        // 3. ACL 鉴权（@PreAuthorize 已拦�?FINANCE 全拒，此处兜底）
            if (!drawingAuthz.canView(auth, drawingId)) {
            recordAclDeny(auth, drawingId, "PREVIEW");
            return Result.fail(40304, aclDenyMessage(auth, drawingId, "preview"));
        }

        // 4. 调用 PDF 导出（含 1h 缓存 + 签字扫描件嵌入）
            Result<byte[]> result = pdfExportService.exportPdf(drawingId, "pdf");
        if (result.getCode() == 0) {
            // 5. 注入水印（用�?ID + 时间�?· 防止截屏外发�?
            byte[] pdfBytes = injectWatermark(result.getData(), auth, drawing);
            return Result.ok(pdfBytes);
        }
        return result;
    }

    /**
     * 下载原文件（�?ENGINEER · 强隔离）
     */
    @Transactional
    @AuditLog(module = "drawing_acl", action = "drawing.download")
    public Result<byte[]> downloadOriginal(Long drawingId, Authentication auth) {
        if (drawingId == null) return Result.fail(40001, "DRAWING_ID_REQUIRED");

        // 1. 图纸存在性校�?
            CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) return Result.fail(40401, "DRAWING_NOT_FOUND");

        // 2. ACL 鉴权（仅 ENGINEER�?
            if (!drawingAuthz.canDownload(auth, drawingId)) {
            recordAclDeny(auth, drawingId, "DOWNLOAD");
            return Result.fail(40304, "该图纸仅 ENGINEER 可下载");
        }

        // 3. 复用 PDF 导出（生产可改为原文件路径流�?
            Result<byte[]> result = pdfExportService.exportPdf(drawingId, "pdf");
        return result;
    }

    /**
     * permission 端点（任意角色可调）
     *
     * <p>返回当前用户对该图纸�?5 操作�?+ scope + linkedBizIds
     * <p>FINANCE 也返 200 + �?false + scope=NONE（不�?403�?     */
    public Result<DrawingPermissionDTO> getPermission(Long drawingId, Authentication auth) {
        if (drawingId == null) return Result.fail(40001, "DRAWING_ID_REQUIRED");

        // 1. 图纸存在性校�?
            CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) return Result.fail(40401, "DRAWING_NOT_FOUND");

        // 2. 计算权限位（不受 feature flag 影响�?
            DrawingAuthz.PermissionBits bits = drawingAuthz.computePermissionBits(auth, drawingId);

        // 3. 关联业务单据 ID 列表
            Map<String, List<Long>> linkedBizIds = drawingAuthz.linkedBizIds(drawingId);

        // 4. 包装 DTO
            DrawingPermissionDTO dto = new DrawingPermissionDTO();
        dto.setDrawingId(drawingId);
        dto.setRole(bits.role);
        dto.setScope(bits.scope);
        DrawingPermissionDTO.PermissionBitsDTO permDto = new DrawingPermissionDTO.PermissionBitsDTO();
        permDto.setView(bits.view);
        permDto.setPrint(bits.print);
        permDto.setDownload(bits.download);
        permDto.setUpload(bits.upload);
        permDto.setDelete(bits.delete);
        dto.setPermissions(permDto);
        dto.setLinkedBizIds(linkedBizIds);
        // 权限查询结果 5min 后过�?
            dto.setExpiresAt(LocalDateTime.now().plusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return Result.ok(dto);
    }

    // ------------------------------------------------------------
    // ACL 拒绝细节（消息定制）
    // ------------------------------------------------------------
            private String aclDenyMessage(Authentication auth, Long drawingId, String op) {
        if (auth == null) return "用户未认证";
        String role = DrawingAuthz.primaryRole(auth);
        switch (role) {
            case "FINANCE":
                return "FINANCE 角色无图纸权限";
            case "SALES":
                return "该图纸未关联您的订单";
            case "PURCHASER":
                return "该图纸未关联您的采购单";
            case "WAREHOUSE":
                return "该图纸未关联您的入库单";
            case "QC":
                return "该图纸未关联您的质检单";
            case "OPERATOR":
                return "当前工序未关联该图纸";
            default:
                return "角色 " + role + " �?" + op + " 权限";
        }
    }

    /** 记录 ACL 拒绝日志（独立切面兜�?· 不依�?@AuditLog 因为已经在事务外�?*/
    private void recordAclDeny(Authentication auth, Long drawingId, String op) {
        String role = auth == null ? "" : DrawingAuthz.primaryRole(auth);
        String username = auth == null ? "" : auth.getName();
        log.info("[DRAWING_ACL_DENY] drawingId={} op={} role={} user={} time={}",
                drawingId, op, role, username, LocalDateTime.now());
    }

    /**
     * 水印注入（简化版：文本叠加）
     * 生产可对�?PDFBox / OpenPDF 加图片水�?     */
    private byte[] injectWatermark(byte[] pdfBytes, Authentication auth, CrmDrawing drawing) {
        if (pdfBytes == null) return null;
        String watermark = String.format("\n[WATERMARK] user=%s role=%s drawingId=%d drawingNo=%s time=%s\n",
                auth == null ? "" : auth.getName(),
                auth == null ? "" : DrawingAuthz.primaryRole(auth),
                drawing.getId(),
                drawing.getDrawingNo(),
                LocalDateTime.now());
        byte[] watermarkBytes = watermark.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] combined = new byte[pdfBytes.length + watermarkBytes.length];
        System.arraycopy(pdfBytes, 0, combined, 0, pdfBytes.length);
        System.arraycopy(watermarkBytes, 0, combined, pdfBytes.length, watermarkBytes.length);
        return combined;
    }
}