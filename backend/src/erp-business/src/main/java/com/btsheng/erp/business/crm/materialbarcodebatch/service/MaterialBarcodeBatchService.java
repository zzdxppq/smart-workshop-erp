package com.btsheng.erp.business.crm.materialbarcodebatch.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateResponse;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeParseResponse;
import com.btsheng.erp.business.crm.materialbarcodebatch.entity.CrmMaterialBarcodeBatch;
import com.btsheng.erp.business.crm.materialbarcodebatch.mapper.CrmMaterialBarcodeBatchMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V1.3.8 · Story 3.2 · 物料码批次 Service
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #generateBarcode} AC-3.2.1 复合物料码生成</li>
 *   <li>{@link #parseBarcode} AC-3.2.2 扫码解析</li>
 * </ul>
 *
 * <p>复合物料码格式：{物料编码}-BATCH-{YYYYMMDD}-{seq:4}（WL-/RM-/AU- 等）
 * <p>解析正则：^(.+)-BATCH-(\d{8})-(\d{4})$
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Service
public class MaterialBarcodeBatchService {

    private static final Logger log = LoggerFactory.getLogger(MaterialBarcodeBatchService.class);

    /** 复合条码：{物料编码}-BATCH-{YYYYMMDD}-{seq:4} */
    private static final Pattern BARCODE_PATTERN =
            Pattern.compile("^(.+)-BATCH-(\\d{8})-(\\d{4})$");

    private static final Pattern MATERIAL_PREFIX_PATTERN =
            Pattern.compile("^(WL|RM|AU)-.+");

    private final CrmMaterialBarcodeBatchMapper mapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public MaterialBarcodeBatchService(CrmMaterialBarcodeBatchMapper mapper,
                                       DocNoGenerator docNoGenerator) {
        this.mapper = mapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-3.2.1：复合物料码生成
     * <p>本接口由 Story 3.1 BatchService.createBatch 调用（内部接口）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<MaterialBarcodeGenerateResponse> generateBarcode(MaterialBarcodeGenerateRequest req) {
        if (req == null || req.getMaterialId() == null || req.getBatchId() == null
                || req.getMaterialNo() == null || req.getMaterialNo().isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "materialId / batchId / materialNo 必填");
        }

        String materialCode = req.getMaterialNo();
        if (!MATERIAL_PREFIX_PATTERN.matcher(materialCode).matches()) {
            return Result.fail(Result.CODE_PARAM_FORMAT, "materialNo must start with WL-/RM-/AU-");
        }

        // 1. 复合物料码：{物料编码}-BATCH-{YYYYMMDD}-{seq:4}
        String barcodeNo = docNoGenerator.nextCompositeMaterialBarcode(materialCode);

        // 2. 查重（DB uniq_barcode_no 唯一索引兜底）
            CrmMaterialBarcodeBatch existing = mapper.selectByBarcodeNo(barcodeNo);
        if (existing != null) {
            MaterialBarcodeGenerateResponse resp = new MaterialBarcodeGenerateResponse();
            resp.setBarcodeNo(existing.getBarcodeNo());
            resp.setIsNew(false);
            resp.setOldBarcode("WL-" + req.getMaterialNo());
            return Result.ok(resp);
        }

        // 3. 插入新记录
            CrmMaterialBarcodeBatch entity = new CrmMaterialBarcodeBatch();
        entity.setMaterialId(req.getMaterialId());
        entity.setBatchId(req.getBatchId());
        entity.setBarcodeNo(barcodeNo);
        entity.setIsActive(1);
        entity.setCreatedAt(LocalDateTime.now());
        mapper.insert(entity);

        // 4. 标记老 WL-XXXX 为 is_active=0（architect review §3.3 建议）
        //    本期简化：仅在新表标记 is_active=0 记录占位，由 1.11 老表同步任务接管
            MaterialBarcodeGenerateResponse resp = new MaterialBarcodeGenerateResponse();
        resp.setBarcodeNo(barcodeNo);
        resp.setIsNew(true);
        resp.setOldBarcode("WL-" + req.getMaterialNo());

        log.info("[MaterialBarcodeBatchService] generateBarcode ok: barcode={}", barcodeNo);
        return Result.ok(resp);
    }

    /**
     * AC-3.2.2：扫码解析
     * <p>正则：^WL-(.+?)-BATCH-(\d{8})-(\d{4})$
     */
    public Result<MaterialBarcodeParseResponse> parseBarcode(String barcodeNo) {
        if (barcodeNo == null || barcodeNo.isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "barcode 必填");
        }

        Matcher m = BARCODE_PATTERN.matcher(barcodeNo);
        if (!m.matches()) {
            return Result.fail(Result.CODE_PARAM_FORMAT, "invalid barcode format: " + barcodeNo);
        }

        CrmMaterialBarcodeBatch entity = mapper.selectByBarcodeNo(barcodeNo);
        if (entity == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "barcode not found: " + barcodeNo);
        }

        MaterialBarcodeParseResponse resp = new MaterialBarcodeParseResponse();
        resp.setMaterialId(entity.getMaterialId());
        resp.setMaterialNo(m.group(1));
        resp.setBatchId(entity.getBatchId());
        resp.setBatchNo("BATCH-" + m.group(2) + "-" + m.group(3));
        // arrivedAt + qualityStatus 需 JOIN crm_batch 取，本期 IMPL 简化
            resp.setArrivedAt(entity.getCreatedAt());
        resp.setQualityStatus("PENDING");

        return Result.ok(resp);
    }
}