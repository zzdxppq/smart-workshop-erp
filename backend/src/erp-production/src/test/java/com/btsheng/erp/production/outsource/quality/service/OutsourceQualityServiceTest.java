package com.btsheng.erp.production.outsource.quality.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.quality.dto.AddQualityDefectRequest;
import com.btsheng.erp.production.outsource.quality.dto.QualityCreateRequest;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQuality;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityDefect;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityItem;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityDefectMapper;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityItemMapper;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
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
 * V1.3.7 · Story 1.27 · 委外工序质检 Service 单元测试（FR-6-7）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceQualityServiceTest {

    @Mock private CrmOutsourceQualityMapper qualityMapper;
    @Mock private CrmOutsourceQualityItemMapper itemMapper;
    @Mock private CrmOutsourceQualityDefectMapper defectMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceQualityService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceQualityService(qualityMapper, itemMapper, defectMapper, orderMapper, docNoGenerator);
        when(docNoGenerator.nextOutsourceQualityNo()).thenReturn("OQ20260612-0001", "OQ20260612-0002", "OQ20260612-0003");

        when(qualityMapper.insert(any(CrmOutsourceQuality.class))).thenAnswer(inv -> {
            CrmOutsourceQuality q = inv.getArgument(0);
            q.setId(1L);
            return 1;
        });
        when(itemMapper.insert(any(CrmOutsourceQualityItem.class))).thenAnswer(inv -> {
            CrmOutsourceQualityItem i = inv.getArgument(0);
            i.setId(1L);
            return 1;
        });
        when(defectMapper.insert(any(CrmOutsourceQualityDefect.class))).thenAnswer(inv -> {
            CrmOutsourceQualityDefect d = inv.getArgument(0);
            d.setId(1L);
            return 1;
        });
        when(qualityMapper.updateById(any(CrmOutsourceQuality.class))).thenReturn(1);
    }

    private CrmOutsourceOrder mockOrder() {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(1L);
        o.setOutsourceNo("WW20260612-0001");
        o.setSupplierId(101L);
        o.setSupplierName("上海热处理厂");
        o.setStatus("COMPLETED");
        return o;
    }

    private QualityCreateRequest.QualityItemDto mockItem(String name, int passed) {
        QualityCreateRequest.QualityItemDto i = new QualityCreateRequest.QualityItemDto();
        i.setItemType("FA");
        i.setItemName(name);
        i.setPassed(passed);
        return i;
    }

    private QualityCreateRequest buildValidReq() {
        QualityCreateRequest req = new QualityCreateRequest();
        req.setOutsourceId(1L);
        req.setProcessName("调质");
        req.setInspectType("FA");
        req.setInspectQty(1);
        req.setItems(Arrays.asList(mockItem("外观", 1), mockItem("硬度", 1)));
        return req;
    }

    // ====== createQuality 6 测例 ======
            @Test
    @DisplayName("createQuality happy path FA")
    void testCreate_FA() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder());
        Result<CrmOutsourceQuality> r = service.createQuality(buildValidReq(), 301L);
        assertEquals(0, r.getCode());
        assertEquals("OQ20260612-0001", r.getData().getQualityNo());
        assertEquals("FA", r.getData().getInspectType());
    }

    @Test
    @DisplayName("createQuality CMM 三次元允许")
    void testCreate_CMM() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder());
        QualityCreateRequest req = buildValidReq();
        req.setInspectType("CMM");
        req.setItems(Arrays.asList(mockItem("X 轴", 1), mockItem("Y 轴", 1)));
        Result<CrmOutsourceQuality> r = service.createQuality(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("createQuality 检验项目必填：空 → 40001")
    void testCreate_ItemsRequired() {
        QualityCreateRequest req = buildValidReq();
        req.setItems(new ArrayList<>());
        Result<CrmOutsourceQuality> r = service.createQuality(req, 301L);
        assertEquals(40001, r.getCode());
        assertEquals("QUALITY_ITEMS_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createQuality 检验项目名称必填")
    void testCreate_ItemNameRequired() {
        QualityCreateRequest req = buildValidReq();
        req.setItems(Arrays.asList(mockItem("", 1)));
        Result<CrmOutsourceQuality> r = service.createQuality(req, 301L);
        assertEquals(40001, r.getCode());
        assertEquals("QUALITY_ITEM_NAME_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createQuality 检验类型非法 → 40001")
    void testCreate_InspectTypeInvalid() {
        QualityCreateRequest req = buildValidReq();
        req.setInspectType("WRONG");
        Result<CrmOutsourceQuality> r = service.createQuality(req, 301L);
        assertEquals(40001, r.getCode());
        assertEquals("INSPECT_TYPE_INVALID", r.getMessage());
    }

    @Test
    @DisplayName("createQuality 委外单不存在 → 40404")
    void testCreate_OrderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);
        QualityCreateRequest req = buildValidReq();
        req.setOutsourceId(999L);
        Result<CrmOutsourceQuality> r = service.createQuality(req, 301L);
        assertEquals(40404, r.getCode());
    }

    // ====== addDefect 4 测例 ======
            @Test
    @DisplayName("addDefect 严重度 MINOR 接受")
    void testAddDefect_Minor() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(1L);
        q.setInspectQty(10);
        q.setFailedQty(0);
        when(qualityMapper.selectById(1L)).thenReturn(q);

        AddQualityDefectRequest req = new AddQualityDefectRequest();
        req.setQualityId(1L);
        req.setDefectType("毛刺");
        req.setSeverity("MINOR");
        req.setQty(1);
        Result<CrmOutsourceQualityDefect> r = service.addDefect(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addDefect 严重度 MAJOR 接受")
    void testAddDefect_Major() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(1L);
        q.setInspectQty(10);
        when(qualityMapper.selectById(1L)).thenReturn(q);

        AddQualityDefectRequest req = new AddQualityDefectRequest();
        req.setQualityId(1L);
        req.setDefectType("尺寸超差");
        req.setSeverity("MAJOR");
        req.setQty(1);
        Result<CrmOutsourceQualityDefect> r = service.addDefect(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addDefect 严重度 CRITICAL 接受")
    void testAddDefect_Critical() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(1L);
        q.setInspectQty(10);
        when(qualityMapper.selectById(1L)).thenReturn(q);

        AddQualityDefectRequest req = new AddQualityDefectRequest();
        req.setQualityId(1L);
        req.setDefectType("裂纹");
        req.setSeverity("CRITICAL");
        req.setQty(1);
        Result<CrmOutsourceQualityDefect> r = service.addDefect(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addDefect 严重度非法 → 40001")
    void testAddDefect_SeverityInvalid() {
        AddQualityDefectRequest req = new AddQualityDefectRequest();
        req.setQualityId(1L);
        req.setDefectType("x");
        req.setSeverity("WRONG");
        req.setQty(1);
        Result<CrmOutsourceQualityDefect> r = service.addDefect(req, 301L);
        assertEquals(40001, r.getCode());
    }

    // ====== pass / reject / list 4 测例 ======
            @Test
    @DisplayName("pass 无 CRITICAL → PASSED")
    void testPass_OK() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(1L);
        q.setInspectQty(1);
        q.setFailedQty(0);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(1L)).thenReturn(q);
        when(defectMapper.countCriticalByQualityId(1L)).thenReturn(0);

        Result<CrmOutsourceQuality> r = service.pass(1L, 301L);
        assertEquals(0, r.getCode());
        assertEquals("PASSED", r.getData().getResult());
    }

    @Test
    @DisplayName("pass CRITICAL 缺陷 → 40903")
    void testPass_HasCritical() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(1L);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(1L)).thenReturn(q);
        when(defectMapper.countCriticalByQualityId(1L)).thenReturn(1);

        Result<CrmOutsourceQuality> r = service.pass(1L, 301L);
        assertEquals(40903, r.getCode());
    }

    @Test
    @DisplayName("reject 触发返修")
    void testReject_TriggerRework() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(1L);
        q.setOutsourceId(1L);
        q.setOutsourceNo("WW1");
        q.setInspectQty(1);
        q.setResult("DRAFT");
        when(qualityMapper.selectById(1L)).thenReturn(q);

        Result<Map<String, Object>> r = service.reject(1L, "尺寸超差", 301L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("reworkTriggered"));
    }

    @Test
    @DisplayName("list 按 outsourceId 查询")
    void testList() {
        CrmOutsourceQuality q = new CrmOutsourceQuality();
        q.setId(1L);
        when(qualityMapper.selectByOutsourceId(1L)).thenReturn(List.of(q));

        Result<List<CrmOutsourceQuality>> r = service.list(1L, null, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
