package com.btsheng.erp.business.finance.signedscan.controller;

import com.btsheng.erp.business.finance.signedscan.service.SignedScanArchiveService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "E9-Signed-Scan-Archive", description = "签字扫描件财务档案 · 5 年保留 · 下载审计")
@RestController
@RequestMapping("/finance/signed-scans")
public class SignedScanArchiveController {

    private final SignedScanArchiveService service;

    public SignedScanArchiveController(SignedScanArchiveService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "签字扫描件档案列表")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return service.listArchives(keyword, pageNum, pageSize);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "解密下载签字扫描件（写入 sys_download_log）")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            HttpServletRequest request) {
        try {
            byte[] data = service.downloadSignature(id, userId, clientIp(request));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signature_" + id + ".png\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
