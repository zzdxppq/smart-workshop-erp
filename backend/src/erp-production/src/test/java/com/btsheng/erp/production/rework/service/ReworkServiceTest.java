package com.btsheng.erp.production.rework.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.rework.entity.CrmRework;
import com.btsheng.erp.production.rework.entity.CrmReworkAlert;
import com.btsheng.erp.production.rework.entity.CrmReworkHistory;
import com.btsheng.erp.production.rework.mapper.CrmReworkAlertMapper;
import com.btsheng.erp.production.rework.mapper.CrmReworkHistoryMapper;
import com.btsheng.erp.production.rework.mapper.CrmReworkMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.23 · ReworkService 单元测试（FR-6-3）
 * 14 测例：createRework + 次数限制 + 原因必填 + 成本非负 + finishRework + 历史 + 预警 + 审计 + 跨模块
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReworkServiceTest {

    @Mock private CrmReworkMapper reworkMapper;
    @Mock private CrmReworkHistoryMapper historyMapper;
    @Mock private CrmReworkAlertMapper alertMapper;
    @Mock private CrmOutsourceOrderMapper orderMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private ReworkService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ReworkService(reworkMapper, historyMapper, alertMapper, orderMapper, docNoGenerator);

        when(docNoGenerator.nextReworkNo()).thenReturn("RW20260612-0001");

        when(reworkMapper.insert(any(CrmRework.class))).thenAnswer(inv -> {
            CrmRework r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });
        when(historyMapper.insert(any(CrmReworkHistory.class))).thenAnswer(inv -> {
            CrmReworkHistory h = inv.getArgument(0);
            h.setId(1L);
            return 1;
        });
        when(alertMapper.insert(any(CrmReworkAlert.class))).thenAnswer(inv -> {
            CrmReworkAlert a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        });
        when(orderMapper.updateById(any(CrmOutsourceOrder.class))).thenReturn(1);
    }

    private CrmOutsourceOrder mockOrder(Long id, int reworkCount) {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(id);
        o.setOutsourceNo("WW20260612-0001");
        o.setStatus("INSPECTED");
        o.setReworkCount(reworkCount);
        return o;
    }

    // ====== createRework 2 测例 ======
            @Test
    @DisplayName("createRework 首次返修 happy path")
    void testCreateRework_First() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 0));

        Result<CrmRework> r = service.createRework(1L, "调质不良", new BigDecimal("200"), 201L);
        assertEquals(0, r.getCode());
        assertEquals("RW20260612-0001", r.getData().getReworkNo());
        assertEquals(1, r.getData().getReworkCount());
    }

    @Test
    @DisplayName("createRework 第三次返修 happy path")
    void testCreateRework_Third() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 2));

        Result<CrmRework> r = service.createRework(1L, "第三次返工", new BigDecimal("300"), 201L);
        assertEquals(0, r.getCode());
        assertEquals(3, r.getData().getReworkCount());
    }

    // ====== 返修次数 > 3 抛 40905 = 2 测例 ======
            @Test
    @DisplayName("返修次数 > 3 抛 40905")
    void testCreateRework_ExceedLimit() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 3));

        Result<CrmRework> r = service.createRework(1L, "第四次返工", new BigDecimal("100"), 201L);
        assertEquals(40905, r.getCode());
        assertEquals("REWORK_COUNT_EXCEED_MAX_3", r.getMessage());
    }

    @Test
    @DisplayName("返修次数 4 抛 40905（更严格）")
    void testCreateRework_ExceedLimit_4() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 4));

        Result<CrmRework> r = service.createRework(1L, "第五次", new BigDecimal("100"), 201L);
        assertEquals(40905, r.getCode());
    }

    // ====== 返修原因必填 = 1 测例 ======
            @Test
    @DisplayName("返修原因必填 缺失 → 40001")
    void testCreateRework_ReasonRequired() {
        Result<CrmRework> r = service.createRework(1L, null, new BigDecimal("100"), 201L);
        assertEquals(40001, r.getCode());
    }

    // ====== 返修成本非负 = 1 测例 ======
            @Test
    @DisplayName("返修成本非负 负数 → 40001")
    void testCreateRework_CostNonNegative() {
        Result<CrmRework> r = service.createRework(1L, "ok", new BigDecimal("-1"), 201L);
        assertEquals(40001, r.getCode());
    }

    // ====== finishRework 1 测例 ======
            @Test
    @DisplayName("finishRework 成功")
    void testFinishRework_Happy() {
        CrmRework r = new CrmRework();
        r.setId(1L);
        r.setStatus("IN_PROGRESS");
        when(reworkMapper.selectById(1L)).thenReturn(r);

        Result<CrmRework> result = service.finishRework(1L, 201L);
        assertEquals(0, result.getCode());
        assertEquals("COMPLETED", result.getData().getStatus());
        assertNotNull(result.getData().getFinishedAt());
    }

    // ====== 返修历史 = 1 测例 ======
            @Test
    @DisplayName("返修历史查询")
    void testGetReworkHistory() {
        CrmReworkHistory h = new CrmReworkHistory();
        h.setId(1L);
        h.setReworkId(1L);
        h.setOperation("CREATE");
        when(historyMapper.selectByOutsourceId(1L)).thenReturn(List.of(h));

        Result<List<CrmReworkHistory>> r = service.getReworkHistory(1L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    // ====== 返修次数预警 4 级别 = 4 测例 ======
            @Test
    @DisplayName("预警级别 0 → INFO")
    void testAlert_Info() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 0));
        when(alertMapper.selectByOutsourceId(1L)).thenReturn(new ArrayList<>());

        Result<Map<String, Object>> r = service.getReworkAlert(1L);
        assertEquals(0, r.getCode());
        assertEquals("INFO", r.getData().get("level"));
    }

    @Test
    @DisplayName("预警级别 1 → INFO")
    void testAlert_Info1() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 1));
        when(alertMapper.selectByOutsourceId(1L)).thenReturn(new ArrayList<>());

        Result<Map<String, Object>> r = service.getReworkAlert(1L);
        assertEquals(0, r.getCode());
        assertEquals("INFO", r.getData().get("level"));
    }

    @Test
    @DisplayName("预警级别 2 → WARN")
    void testAlert_Warn() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 2));
        when(alertMapper.selectByOutsourceId(1L)).thenReturn(new ArrayList<>());

        Result<Map<String, Object>> r = service.getReworkAlert(1L);
        assertEquals(0, r.getCode());
        assertEquals("WARN", r.getData().get("level"));
    }

    @Test
    @DisplayName("预警级别 3 → CRITICAL")
    void testAlert_Critical() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 3));
        when(alertMapper.selectByOutsourceId(1L)).thenReturn(new ArrayList<>());

        Result<Map<String, Object>> r = service.getReworkAlert(1L);
        assertEquals(0, r.getCode());
        assertEquals("CRITICAL", r.getData().get("level"));
    }

    // ====== 审计留痕 = 1 测例 ======
            @Test
    @DisplayName("审计留痕：createRework 写 1 history + 1 alert")
    void testAudit_CreateRework() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 0));

        service.createRework(1L, "审计", new BigDecimal("100"), 201L);
        verify(historyMapper, times(1)).insert(any(CrmReworkHistory.class));
        verify(alertMapper, times(1)).insert(any(CrmReworkAlert.class));
    }

    // ====== 跨模块 1.22 状态机联动 = 1 测例 ======
            @Test
    @DisplayName("跨模块 1.22 状态机联动：返修次数累加同步委外单")
    void testCrossModule_22_23_ReworkCountSync() {
        when(orderMapper.selectById(1L)).thenReturn(mockOrder(1L, 0));

        service.createRework(1L, "联动", new BigDecimal("100"), 201L);
        verify(orderMapper, atLeastOnce()).updateById(any(CrmOutsourceOrder.class));
    }
}
