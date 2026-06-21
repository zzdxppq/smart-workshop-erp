package com.btsheng.erp.business.crm.qualityinspection.service;

import com.btsheng.erp.business.crm.qualityinspection.dto.AddInspectionItemRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionCreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionItem;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualitySample;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionItemMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualitySampleMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.28 · 品质·来料/过程/成品检 Service 单元测试（FR-7-1）
 * 18 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualityInspectionServiceTest {

    @Mock private CrmQualityInspectionMapper inspectionMapper;
    @Mock private CrmQualityInspectionItemMapper itemMapper;
    @Mock private CrmQualitySampleMapper sampleMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private QualityInspectionService service;

    @BeforeEach
    void setUp() {
        service = new QualityInspectionService(inspectionMapper, itemMapper, sampleMapper, docNoGenerator);
        when(docNoGenerator.nextQualityInspectionNo())
                .thenReturn("QI20260612-0001", "QI20260612-0002", "QI20260612-0003", "QI20260612-0004");

        when(inspectionMapper.insert(any(CrmQualityInspection.class))).thenAnswer(inv -> {
            CrmQualityInspection q = inv.getArgument(0);
            q.setId(1L);
            return 1;
        });
        when(itemMapper.insert(any(CrmQualityInspectionItem.class))).thenAnswer(inv -> {
            CrmQualityInspectionItem i = inv.getArgument(0);
            i.setId(1L);
            return 1;
        });
        when(sampleMapper.insert(any(CrmQualitySample.class))).thenAnswer(inv -> {
            CrmQualitySample s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });
        when(inspectionMapper.updateById(any(CrmQualityInspection.class))).thenReturn(1);
    }

    private InspectionCreateRequest.InspectionItemDto mockItem(String name, String sev, int passed) {
        InspectionCreateRequest.InspectionItemDto i = new InspectionCreateRequest.InspectionItemDto();
        i.setItemName(name);
        i.setSeverity(sev);
        i.setPassed(passed);
        return i;
    }

    private InspectionCreateRequest buildIqcReq() {
        InspectionCreateRequest req = new InspectionCreateRequest();
        req.setInspectType("IQC");
        req.setMaterialId(1001L);
        req.setMaterialCode("M-Q235");
        req.setLotSize(500);
        req.setSampleSize(50);
        req.setAqlLevel("1.0");
        req.setItems(Arrays.asList(
                mockItem("外观", "INFO", 1),
                mockItem("厚度", "INFO", 1)));
        return req;
    }

    // ====== createInspection 8 测例 ======
            @Test
    @DisplayName("createInspection happy path IQC 来料检")
    void testCreate_IQC() {
        Result<CrmQualityInspection> r = service.createInspection(buildIqcReq(), 401L);
        assertEquals(0, r.getCode());
        assertEquals("QI20260612-0001", r.getData().getInspectionNo());
        assertEquals("IQC", r.getData().getInspectType());
    }

    @Test
    @DisplayName("createInspection IPQC 过程检")
    void testCreate_IPQC() {
        InspectionCreateRequest req = buildIqcReq();
        req.setInspectType("IPQC");
        req.setWorkOrderId(1L);
        req.setWorkOrderNo("GD20260608-0001");
        req.setProcessName("粗车");
        req.setMaterialId(null);
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("createInspection OQC 成品检")
    void testCreate_OQC() {
        InspectionCreateRequest req = buildIqcReq();
        req.setInspectType("OQC");
        req.setWorkOrderId(1L);
        req.setWorkOrderNo("GD20260608-0001");
        req.setMaterialId(null);
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("createInspection 检验类型非法 → 40001")
    void testCreate_InspectTypeInvalid() {
        InspectionCreateRequest req = buildIqcReq();
        req.setInspectType("WRONG");
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("INSPECT_TYPE_INVALID", r.getMessage());
    }

    @Test
    @DisplayName("createInspection IQC 缺物料 → 40001")
    void testCreate_IQC_MaterialRequired() {
        InspectionCreateRequest req = buildIqcReq();
        req.setMaterialId(null);
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("IQC_MATERIAL_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createInspection IPQC 缺工序 → 40001")
    void testCreate_IPQC_ProcessRequired() {
        InspectionCreateRequest req = buildIqcReq();
        req.setInspectType("IPQC");
        req.setWorkOrderId(1L);
        req.setMaterialId(null);
        req.setProcessName(null);
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("IPQC_PROCESS_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createInspection 检验项目必填（P1 修补 2）")
    void testCreate_ItemsRequired() {
        InspectionCreateRequest req = buildIqcReq();
        req.setItems(new ArrayList<>());
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("INSPECTION_ITEMS_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createInspection 严重度非法（P1 修补 3）→ 40001")
    void testCreate_SeverityInvalid() {
        InspectionCreateRequest req = buildIqcReq();
        req.setItems(Arrays.asList(mockItem("外观", "WRONG_SEV", 1)));
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("INSPECTION_SEVERITY_INVALID", r.getMessage());
    }

    @Test
    @DisplayName("createInspection AQL 等级非法（P1 修补 1）→ 40001")
    void testCreate_AqlInvalid() {
        InspectionCreateRequest req = buildIqcReq();
        req.setAqlLevel("WRONG");
        Result<CrmQualityInspection> r = service.createInspection(req, 401L);
        assertEquals(40001, r.getCode());
        assertEquals("AQL_LEVEL_INVALID", r.getMessage());
    }

    // ====== addItem 3 测例 ======
            @Test
    @DisplayName("addItem 追加 INFO 项目")
    void testAddItem_Info() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(1L);
        q.setInspectQty(1);
        q.setFailedQty(0);
        q.setMaxSeverity("INFO");
        when(inspectionMapper.selectById(1L)).thenReturn(q);

        AddInspectionItemRequest req = new AddInspectionItemRequest();
        req.setInspectionId(1L);
        req.setItemName("硬度");
        req.setSeverity("INFO");
        req.setPassed(1);
        Result<CrmQualityInspectionItem> r = service.addItem(req, 401L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addItem CRITICAL → 升级 max_severity")
    void testAddItem_CriticalUpgrade() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(2L);
        q.setInspectQty(1);
        q.setFailedQty(0);
        q.setMaxSeverity("INFO");
        when(inspectionMapper.selectById(2L)).thenReturn(q);

        AddInspectionItemRequest req = new AddInspectionItemRequest();
        req.setInspectionId(2L);
        req.setItemName("裂纹");
        req.setSeverity("CRITICAL");
        req.setPassed(0);
        service.addItem(req, 401L);

        verify(inspectionMapper, atLeastOnce()).updateById(argThat((CrmQualityInspection qq) ->
                "CRITICAL".equals(qq.getMaxSeverity())));
    }

    @Test
    @DisplayName("addItem 检单不存在 → 40404")
    void testAddItem_NotFound() {
        when(inspectionMapper.selectById(99L)).thenReturn(null);
        AddInspectionItemRequest req = new AddInspectionItemRequest();
        req.setInspectionId(99L);
        req.setItemName("x");
        req.setSeverity("INFO");
        Result<CrmQualityInspectionItem> r = service.addItem(req, 401L);
        assertEquals(40404, r.getCode());
    }

    // ====== pass / reject 4 测例 ======
            @Test
    @DisplayName("pass OQC → 触发入库")
    void testPass_OQC_TriggerStockin() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(10L);
        q.setInspectType("OQC");
        q.setInspectQty(10);
        q.setFailedQty(0);
        q.setMaxSeverity("INFO");
        q.setResult("DRAFT");
        when(inspectionMapper.selectById(10L)).thenReturn(q);

        Result<Map<String, Object>> r = service.pass(10L, 401L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("triggerStockin"));
    }

    @Test
    @DisplayName("pass 有 CRITICAL → 40903")
    void testPass_HasCritical() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(11L);
        q.setMaxSeverity("CRITICAL");
        q.setResult("DRAFT");
        when(inspectionMapper.selectById(11L)).thenReturn(q);

        Result<Map<String, Object>> r = service.pass(11L, 401L);
        assertEquals(40903, r.getCode());
    }

    @Test
    @DisplayName("reject IQC → 触发返修")
    void testReject_IQC_TriggerRework() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(20L);
        q.setInspectType("IQC");
        q.setMaterialId(1001L);
        q.setInspectQty(10);
        q.setResult("DRAFT");
        when(inspectionMapper.selectById(20L)).thenReturn(q);

        Result<Map<String, Object>> r = service.reject(20L, "来料不合格", 401L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("triggerRework"));
        assertEquals(1001L, r.getData().get("materialId"));
    }

    @Test
    @DisplayName("reject 重复 → 40903")
    void testReject_AlreadyFailed() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(21L);
        q.setResult("FAILED");
        when(inspectionMapper.selectById(21L)).thenReturn(q);

        Result<Map<String, Object>> r = service.reject(21L, null, 401L);
        assertEquals(40903, r.getCode());
    }

    // ====== list 1 测例 ======
            @Test
    @DisplayName("list 按 inspectType 过滤")
    void testList() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(1L);
        when(inspectionMapper.selectByInspectType("IQC")).thenReturn(List.of(q));
        Result<List<CrmQualityInspection>> r = service.list("IQC", null, null, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    @DisplayName("list 按 materialId 过滤")
    void testList_ByMaterial() {
        CrmQualityInspection q = new CrmQualityInspection();
        q.setId(2L);
        when(inspectionMapper.selectByMaterialId(1001L)).thenReturn(List.of(q));
        Result<List<CrmQualityInspection>> r = service.list(null, 1001L, null, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
