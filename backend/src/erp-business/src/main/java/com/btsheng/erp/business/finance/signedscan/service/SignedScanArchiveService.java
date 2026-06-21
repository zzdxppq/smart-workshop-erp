package com.btsheng.erp.business.finance.signedscan.service;

import com.btsheng.erp.business.crm.drawing.service.DrawingEncryptionService;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcileSignature;
import com.btsheng.erp.business.crm.reconcile.mapper.CrmReconcileSignatureMapper;
import com.btsheng.erp.business.finance.signedscan.dto.SignedScanArchiveVo;
import com.btsheng.erp.business.finance.signedscan.mapper.SignedScanArchiveMapper;
import com.btsheng.erp.business.platform.audit.entity.SysDownloadLog;
import com.btsheng.erp.business.platform.audit.mapper.SysDownloadLogMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 签字扫描件财务档案 · AES-256 解密下载 + sys_download_log 审计
 */
@Service
public class SignedScanArchiveService {

    private final SignedScanArchiveMapper archiveMapper;
    private final CrmReconcileSignatureMapper signatureMapper;
    private final DrawingEncryptionService encryptionService;
    private final SysDownloadLogMapper downloadLogMapper;

    public SignedScanArchiveService(SignedScanArchiveMapper archiveMapper,
                                    CrmReconcileSignatureMapper signatureMapper,
                                    DrawingEncryptionService encryptionService,
                                    SysDownloadLogMapper downloadLogMapper) {
        this.archiveMapper = archiveMapper;
        this.signatureMapper = signatureMapper;
        this.encryptionService = encryptionService;
        this.downloadLogMapper = downloadLogMapper;
    }

    public Result<Map<String, Object>> listArchives(String keyword, int pageNum, int pageSize) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<SignedScanArchiveVo> items = archiveMapper.selectPage(trim(keyword), offset, pageSize);
        long total = archiveMapper.count(trim(keyword));
        Map<String, Object> page = new HashMap<>();
        page.put("records", items);
        page.put("total", total);
        page.put("pageNum", pageNum);
        page.put("pageSize", pageSize);
        return Result.ok(page);
    }

    @Transactional
    public byte[] downloadSignature(Long signatureId, Long userId, String clientIp) {
        CrmReconcileSignature sig = signatureMapper.selectById(signatureId);
        if (sig == null) {
            throw new IllegalArgumentException("SIGNATURE_NOT_FOUND");
        }
        if (sig.getEncryptedData() == null || sig.getEncryptedData().isBlank()) {
            throw new IllegalStateException("SIGNATURE_DATA_MISSING");
        }
        byte[] plain = encryptionService.decrypt(sig.getEncryptedData());

        SysDownloadLog log = new SysDownloadLog();
        log.setFileId(signatureId);
        log.setUserId(userId == null ? 0L : userId);
        log.setIp(clientIp);
        log.setTs(LocalDateTime.now());
        log.setAction("DOWNLOAD");
        downloadLogMapper.insert(log);

        return plain;
    }

    private static String trim(String keyword) {
        if (keyword == null) return null;
        String t = keyword.trim();
        return t.isEmpty() ? null : t;
    }
}
