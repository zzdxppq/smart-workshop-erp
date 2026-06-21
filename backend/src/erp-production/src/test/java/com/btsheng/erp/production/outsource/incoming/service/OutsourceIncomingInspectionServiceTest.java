package com.btsheng.erp.production.outsource.incoming.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.incoming.dto.AddDefectRequest;
import com.btsheng.erp.production.outsource.incoming.dto.IncomingInspectionRequest;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingDefect;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingInspection;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingItem;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingDefectMapper;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingInspectionMapper;
import com.btsheng.erp.production.outsource.incoming.mapper.CrmOutsourceIncomingItemMapper;
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
 * V1.3.7 · Story 1.25 · 委外来料质检 Service 单元测试（FR-6-5）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutsourceIncomingInspectionServiceTest {

    @Mock private CrmOutsourceIncomingInspectionMapper inspectionMapper;
    @Mock private CrmOutsourceIncomingItemMapper itemMapper;
    @Mock private CrmOutsourceIncomingDefectMapper defectMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private OutsourceIncomingInspectionService service;

    @BeforeEach
    void setUp() {
        service = new OutsourceIncomingInspectionService(inspectionMapper, itemMapper, defectMapper, orderMapper, docNoGenerator);
        when(docNoGenerator.nextOutsourceInspectionNo()).thenReturn("OI20260612-0001", "OI20260612-0002", "OI20260612-0003");

        when(inspectionMapper.insert(any(CrmOutsourceIncomingInspection.class))).thenAnswer(inv -> {
            CrmOutsourceIncomingInspection i = inv.getArgument(0);
            i.setId(1L);
            return 1;
        });
        when(itemMapper.insert(any(CrmOutsourceIncomingItem.class))).thenAnswer(inv -> {
            CrmOutsourceIncomingItem i = inv.getArgument(0);
            i.setId(1L);
            return 1;
        });
        when(defectMapper.insert(any(CrmOutsourceIncomingDefect.class))).thenAnswer(inv -> {
            CrmOutsourceIncomingDefect d = inv.getArgument(0);
            d.setId(1L);
            return 1;
        });
        when(inspectionMapper.updateById(any(CrmOutsourceIncomingInspection.class))).thenReturn(1);
    }

    private CrmOutsourceOrder mockOrder() {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(1L);
        o.setOutsourceNo("WW20260612-0001");
        o.setSupplierId(101L);
        o.setSupplierName("上海热处理厂");
        o.setMaterialCode("ZZ-0002");
        o.setStatus("COMPLETED");
        return o;
    }

    private IncomingInspectionRequest.IncomingItemDto mockItem(String name) {
        IncomingInspectionRequest.IncomingItemDto i = new IncomingInspectionRequest.IncomingItemDto();
        i.setItemName(name);
        i.setStandard("标准值");
        i.setMeasuredValue("实测值");
        i.setPassed(1);
        return i;
    }

    private IncomingInspectionRequest buildValidReq() {
        IncomingInspectionRequest req = new IncomingInspectionRequest();
        req.setOutsourceId(1L);
        req.setInspectQty(10);
        req.setNotifyEmail("qa@163.com");
        req.setItems(Arrays.asList(mockItem("外观"), mockItem("硬度")));
        return req;
    }

    // ====== createInspection 6 测例 ======
            @Test
    @DisplayName("createInspection happy path")
    void testCreate_Happy() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder());

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(buildValidReq(), 301L);
        assertEquals(0, r.getCode());
        assertEquals("OI20260612-0001", r.getData().getInspectionNo());
    }

    @Test
    @DisplayName("createInspection 检验项目必填：空 → 40001")
    void testCreate_ItemsRequired() {
        IncomingInspectionRequest req = buildValidReq();
        req.setItems(new ArrayList<>());

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(40001, r.getCode());
        assertEquals("INSPECT_ITEMS_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createInspection 检验项目名称必填")
    void testCreate_ItemNameRequired() {
        IncomingInspectionRequest req = buildValidReq();
        req.setItems(Arrays.asList(mockItem("")));

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(40001, r.getCode());
        assertEquals("INSPECT_ITEM_NAME_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("createInspection 单一 163 邮箱：非 163 → 40001")
    void testCreate_EmailMustBe163() {
        IncomingInspectionRequest req = buildValidReq();
        req.setNotifyEmail("qa@qq.com");

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(40001, r.getCode());
        assertEquals("NOTIFY_EMAIL_MUST_BE_163", r.getMessage());
    }

    @Test
    @DisplayName("createInspection 163 邮箱通过")
    void testCreate_Email163_OK() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder());
        IncomingInspectionRequest req = buildValidReq();
        req.setNotifyEmail("qa@163.com");

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("createInspection 送检数量必填")
    void testCreate_InspectQtyRequired() {
        IncomingInspectionRequest req = buildValidReq();
        req.setInspectQty(null);

        Result<CrmOutsourceIncomingInspection> r = service.createInspection(req, 301L);
        assertEquals(40001, r.getCode());
    }

    // ====== addDefect 4 测例 ======
            @Test
    @DisplayName("addDefect 严重度 MINOR 接受")
    void testAddDefect_Minor() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(1L);
        ins.setInspectQty(10);
        ins.setFailedQty(0);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);

        AddDefectRequest req = new AddDefectRequest();
        req.setInspectionId(1L);
        req.setDefectType("毛刺");
        req.setSeverity("MINOR");
        req.setQty(1);

        Result<CrmOutsourceIncomingDefect> r = service.addDefect(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addDefect 严重度 MAJOR 接受")
    void testAddDefect_Major() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(1L);
        ins.setInspectQty(10);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);

        AddDefectRequest req = new AddDefectRequest();
        req.setInspectionId(1L);
        req.setDefectType("尺寸超差");
        req.setSeverity("MAJOR");
        req.setQty(2);

        Result<CrmOutsourceIncomingDefect> r = service.addDefect(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addDefect 严重度 CRITICAL 接受")
    void testAddDefect_Critical() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(1L);
        ins.setInspectQty(10);
        when(inspectionMapper.selectById(1L)).thenReturn(ins);

        AddDefectRequest req = new AddDefectRequest();
        req.setInspectionId(1L);
        req.setDefectType("裂纹");
        req.setSeverity("CRITICAL");
        req.setQty(1);

        Result<CrmOutsourceIncomingDefect> r = service.addDefect(req, 301L);
        assertEquals(0, r.getCode());
    }

    @Test
    @DisplayName("addDefect 严重度非法 → 40001")
    void testAddDefect_SeverityInvalid() {
        AddDefectRequest req = new AddDefectRequest();
        req.setInspectionId(1L);
        req.setDefectType("x");
        req.setSeverity("WRONG");
        req.setQty(1);

        Result<CrmOutsourceIncomingDefect> r = service.addDefect(req, 301L);
        assertEquals(40001, r.getCode());
        assertEquals("DEFECT_SEVERITY_INVALID", r.getMessage());
    }

    // ====== pass / reject 4 测例 ======
            @Test
    @DisplayName("pass 无 CRITICAL 缺陷 → PASSED")
    void testPass_OK() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(1L);
        ins.setInspectQty(10);
        ins.setFailedQty(1);
        ins.setResult("DRAFT");
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        when(defectMapper.countCriticalByInspectionId(1L)).thenReturn(0);

        Result<CrmOutsourceIncomingInspection> r = service.pass(1L, 301L);
        assertEquals(0, r.getCode());
        assertEquals("PASSED", r.getData().getResult());
    }

    @Test
    @DisplayName("pass 存在 CRITICAL 缺陷 → 拒绝 40903")
    void testPass_HasCritical() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(1L);
        ins.setInspectQty(10);
        ins.setResult("DRAFT");
        when(inspectionMapper.selectById(1L)).thenReturn(ins);
        when(defectMapper.countCriticalByInspectionId(1L)).thenReturn(1);

        Result<CrmOutsourceIncomingInspection> r = service.pass(1L, 301L);
        assertEquals(40903, r.getCode());
        assertEquals("INSPECTION_HAS_CRITICAL_DEFECT", r.getMessage());
    }

    @Test
    @DisplayName("reject 触发返修")
    void testReject_TriggerRework() {
        CrmOutsourceIncomingInspection ins = new CrmOutsourceIncomingInspection();
        ins.setId(1L);
        ins.setOutsourceId(1L);
        ins.setOutsourceNo("WW20260612-0001");
        ins.setInspectQty(10);
        ins.setResult("DRAFT");
        when(inspectionMapper.selectById(1L)).thenReturn(ins);

        Result<Map<String, Object>> r = service.reject(1L, "尺寸超差", 301L);
        assertEquals(0, r.getCode());
        assertEquals(true, r.getData().get("reworkTriggered"));
        assertEquals(1L, r.getData().get("outsourceId"));
    }

    @Test
    @DisplayName("list 按 outsourceId 查询")
    void testList() {
        CrmOutsourceIncomingInspection i = new CrmOutsourceIncomingInspection();
        i.setId(1L);
        when(inspectionMapper.selectByOutsourceId(1L)).thenReturn(List.of(i));

        Result<List<CrmOutsourceIncomingInspection>> r = service.list(1L, null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
