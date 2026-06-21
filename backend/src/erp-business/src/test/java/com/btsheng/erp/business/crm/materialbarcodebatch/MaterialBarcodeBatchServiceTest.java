package com.btsheng.erp.business.crm.materialbarcodebatch;

import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeGenerateResponse;
import com.btsheng.erp.business.crm.materialbarcodebatch.dto.MaterialBarcodeParseResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V1.3.8 · Story 3.2 · MaterialBarcodeBatchService 单元测例（纯正则 + DTO 校验）
 *
 * <p>AC-3.2.1 复合物料码格式：WL-{material_no}-BATCH-{YYYYMMDD}-{seq:4}
 * <p>AC-3.2.2 扫码解析：正则匹配 + 格式校验
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@DisplayName("Story 3.2 · MaterialBarcodeBatchService 单元测例（V1.3.8 Sprint 7）")
class MaterialBarcodeBatchServiceTest {

    private static Validator validator;
    private static final Pattern BARCODE_PATTERN =
            Pattern.compile("^WL-(.+?)-BATCH-(\\d{8})-(\\d{4})$");

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== AC-3.2.1 复合物料码格式 ====================
            @Test
    @DisplayName("AC-3.2.1.a 标准格式 WL-A001-BATCH-20260613-0001 通过正则")
    void barcodePattern_standardFormat() {
        Matcher m = BARCODE_PATTERN.matcher("WL-A001-BATCH-20260613-0001");
        assertTrue(m.matches());
        assertEquals("A001", m.group(1));
        assertEquals("20260613", m.group(2));
        assertEquals("0001", m.group(3));
    }

    @Test
    @DisplayName("AC-3.2.1.b 长 material_no 含连字符")
    void barcodePattern_longMaterialNo() {
        Matcher m = BARCODE_PATTERN.matcher("WL-FLANGE-100-BATCH-20260613-9999");
        assertTrue(m.matches());
        assertEquals("FLANGE-100", m.group(1));
    }

    @Test
    @DisplayName("AC-3.2.1.c 老格式 WL-A001 不通过新正则")
    void barcodePattern_oldFormat_rejected() {
        Matcher m = BARCODE_PATTERN.matcher("WL-A001");
        assertFalse(m.matches(), "老格式 WL-A001 不应匹配新正则");
    }

    @Test
    @DisplayName("AC-3.2.1.d 缺 BATCH 部分不通过")
    void barcodePattern_missingBatch_rejected() {
        Matcher m = BARCODE_PATTERN.matcher("WL-A001-BATCH-");
        assertFalse(m.matches());
    }

    @Test
    @DisplayName("AC-3.2.1.e 日期格式错（6 位）不通过")
    void barcodePattern_wrongDateFormat_rejected() {
        Matcher m = BARCODE_PATTERN.matcher("WL-A001-BATCH-260613-0001");
        assertFalse(m.matches(), "日期必须是 8 位 yyyyMMdd");
    }

    @Test
    @DisplayName("AC-3.2.1.f 流水号非 4 位不通过")
    void barcodePattern_wrongSeqLength_rejected() {
        Matcher m = BARCODE_PATTERN.matcher("WL-A001-BATCH-20260613-1");
        assertFalse(m.matches(), "流水号必须是 4 位");
    }

    // ==================== AC-3.2.1 DTO 校验 ====================
            @Test
    @DisplayName("AC-3.2.1.g GenerateRequest materialId 必填")
    void generateRequest_materialId_required() {
        MaterialBarcodeGenerateRequest req = new MaterialBarcodeGenerateRequest();
        req.setBatchId(8001L);
        req.setMaterialNo("WL-A001");
        Set<ConstraintViolation<MaterialBarcodeGenerateRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }

    @Test
    @DisplayName("AC-3.2.1.h GenerateRequest batchId 必填")
    void generateRequest_batchId_required() {
        MaterialBarcodeGenerateRequest req = new MaterialBarcodeGenerateRequest();
        req.setMaterialId(5001L);
        req.setMaterialNo("WL-A001");
        Set<ConstraintViolation<MaterialBarcodeGenerateRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }

    @Test
    @DisplayName("AC-3.2.1.i GenerateRequest materialNo 必填")
    void generateRequest_materialNo_required() {
        MaterialBarcodeGenerateRequest req = new MaterialBarcodeGenerateRequest();
        req.setMaterialId(5001L);
        req.setBatchId(8001L);
        Set<ConstraintViolation<MaterialBarcodeGenerateRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }

    @Test
    @DisplayName("AC-3.2.1.j 完整 GenerateRequest 校验通过")
    void generateRequest_valid() {
        MaterialBarcodeGenerateRequest req = new MaterialBarcodeGenerateRequest();
        req.setMaterialId(5001L);
        req.setBatchId(8001L);
        req.setMaterialNo("WL-A001");
        Set<ConstraintViolation<MaterialBarcodeGenerateRequest>> v = validator.validate(req);
        assertTrue(v.isEmpty());
    }

    // ==================== AC-3.2.2 Response DTO 字段 ====================
            @Test
    @DisplayName("AC-3.2.2.a GenerateResponse 字段映射")
    void generateResponse_fields() {
        MaterialBarcodeGenerateResponse resp = new MaterialBarcodeGenerateResponse();
        resp.setBarcodeNo("WL-A001-BATCH-20260613-0001");
        resp.setIsNew(true);
        resp.setOldBarcode("WL-A001");

        assertEquals("WL-A001-BATCH-20260613-0001", resp.getBarcodeNo());
        assertTrue(resp.getIsNew());
        assertEquals("WL-A001", resp.getOldBarcode());
    }

    @Test
    @DisplayName("AC-3.2.2.b ParseResponse 字段映射")
    void parseResponse_fields() {
        MaterialBarcodeParseResponse resp = new MaterialBarcodeParseResponse();
        resp.setMaterialId(5001L);
        resp.setMaterialNo("A001");
        resp.setBatchId(8001L);
        resp.setBatchNo("BATCH-20260613-0001");
        resp.setQualityStatus("PENDING");

        assertEquals(5001L, resp.getMaterialId());
        assertEquals("A001", resp.getMaterialNo());
        assertEquals("BATCH-20260613-0001", resp.getBatchNo());
        assertEquals("PENDING", resp.getQualityStatus());
    }
}