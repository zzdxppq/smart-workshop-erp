package com.btsheng.erp.production.process.integration;

import com.btsheng.erp.production.process.dto.*;
import com.btsheng.erp.production.process.entity.CrmProcess;
import com.btsheng.erp.production.process.entity.CrmProcessRoute;
import com.btsheng.erp.production.process.entity.CrmProcessStep;
import com.btsheng.erp.production.process.mapper.CrmProcessMapper;
import com.btsheng.erp.production.process.mapper.CrmProcessRouteMapper;
import com.btsheng.erp.production.process.mapper.CrmProcessStepMapper;
import com.btsheng.erp.production.process.service.ProcessService;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 Story 1.10 · 工艺库 集成测例（20 测例）
 * Service 8 + Step 5 + Route 4 + CrossModule 3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessIntegrationTest {

    @Mock private CrmProcessMapper processMapper;
    @Mock private CrmProcessStepMapper stepMapper;
    @Mock private CrmProcessRouteMapper routeMapper;
    @Mock private ErpDocNoGenerator docNoGenerator;

    private ProcessService newSvc() {
        return new ProcessService(processMapper, stepMapper, routeMapper, docNoGenerator);
    }

    private CrmProcess makeProcess(Long id) {
        CrmProcess p = new CrmProcess();
        p.setId(id);
        p.setProcessCode("PROC-20260612-0001");
        p.setProcessName("test process");
        p.setProcessType("STANDARD");
        p.setTotalSteps(5);
        p.setTotalCost(new BigDecimal("640.50"));
        p.setIsActive(1);
        p.setOwnerUserId(1001L);
        return p;
    }

    private List<ProcessCreateRequest.StepInput> make5Segments() {
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        String[] segs = {"原材料", "粗加工", "精加工", "表面处理", "检验"};
        for (int i = 0; i < 5; i++) {
            ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
            s.setStepNo(i + 1);
            s.setStepName("step" + (i + 1));
            s.setSegment(segs[i]);
            s.setMachineType("CNC_LATHE");
            s.setEstimatedHours(new BigDecimal("1.0"));
            s.setUnitCost(new BigDecimal(100 * (i + 1)));
            steps.add(s);
        }
        return steps;
    }

    // ===== Service 8 测例 =====
            @Test void integ_create_with_5_segments() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-I-0001");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(5, r.getData().getTotalSteps());
    }

    @Test void integ_create_reject_empty_name() {
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void integ_create_with_2_steps() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-I-0002");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
            s.setStepNo(i + 1);
            s.setStepName("s" + (i + 1));
            s.setMachineType("CNC_LATHE");
            s.setEstimatedHours(new BigDecimal("1.0"));
            s.setUnitCost(new BigDecimal(100));
            steps.add(s);
        }
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(2, r.getData().getTotalSteps());
    }

    @Test void integ_create_with_quality_check() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-I-0003");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
        s.setStepNo(1);
        s.setStepName("qc");
        s.setSegment("检验");
        s.setMachineType("CMM");
        s.setIsQualityCheck(true);
        s.setEstimatedHours(new BigDecimal("1.0"));
        s.setUnitCost(new BigDecimal(100));
        steps.add(s);
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void integ_create_reject_duplicate_step_no() {
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
            s.setStepNo(1);
            s.setStepName("dup");
            s.setMachineType("CNC");
            s.setEstimatedHours(new BigDecimal("1.0"));
            steps.add(s);
        }
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(40905, r.getCode());
    }

    @Test void integ_list_processes() {
        when(processMapper.selectActive()).thenReturn(List.of(makeProcess(1L), makeProcess(2L)));
        ProcessService svc = newSvc();
        ProcessQueryRequest q = new ProcessQueryRequest();
        q.setPage(0);
        q.setSize(20);
        Result<Map<String, Object>> r = svc.listProcesses(q);
        assertEquals(0, r.getCode());
        assertEquals(2, r.getData().get("total"));
    }

    @Test void integ_list_filter_by_type() {
        CrmProcess p1 = makeProcess(1L);
        p1.setProcessType("FA");
        when(processMapper.selectActive()).thenReturn(List.of(p1));
        ProcessService svc = newSvc();
        ProcessQueryRequest q = new ProcessQueryRequest();
        q.setProcessType("FA");
        Result<Map<String, Object>> r = svc.listProcesses(q);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().get("total"));
    }

    @Test void integ_create_drawing_link() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-I-0004");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        req.setDrawingId(1L);
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(1L, r.getData().getDrawingId());
        assertNotNull(r.getData().getDrawingNo());
    }

    // ===== Step 5 测例 =====
            @Test void integ_add_step_increments_total() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.maxStepNo(1L)).thenReturn(5);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        when(processMapper.updateById(any(CrmProcess.class))).thenReturn(1);
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("s6");
        req.setMachineType("CNC_LATHE");
        req.setEstimatedHours(new BigDecimal("1.0"));
        req.setUnitCost(new BigDecimal(100));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(0, r.getCode());
        verify(processMapper, atLeastOnce()).updateById(any(CrmProcess.class));
    }

    @Test void integ_add_step_reject_negative_hours() {
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("x");
        req.setMachineType("CNC");
        req.setEstimatedHours(new BigDecimal("-1.0"));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void integ_add_step_zero_hours_ok() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.maxStepNo(1L)).thenReturn(5);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        when(processMapper.updateById(any(CrmProcess.class))).thenReturn(1);
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("s6");
        req.setMachineType("MANUAL");
        req.setEstimatedHours(BigDecimal.ZERO);
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void integ_add_step_quality_check() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.maxStepNo(1L)).thenReturn(5);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        when(processMapper.updateById(any(CrmProcess.class))).thenReturn(1);
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("qc");
        req.setMachineType("CMM");
        req.setIsQualityCheck(true);
        req.setEstimatedHours(new BigDecimal("1.0"));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getIsQualityCheck());
    }

    @Test void integ_add_step_5_segments_covered() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.maxStepNo(1L)).thenReturn(4);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        when(processMapper.updateById(any(CrmProcess.class))).thenReturn(1);
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("s5");
        req.setSegment("检验");
        req.setMachineType("CMM");
        req.setEstimatedHours(new BigDecimal("1.0"));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(5, r.getData().getStepNo());
        assertEquals("检验", r.getData().getSegment());
    }

    // ===== Route 4 测例 =====
            @Test void integ_get_route_with_steps() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        List<CrmProcessStep> steps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CrmProcessStep s = new CrmProcessStep();
            s.setId((long) i);
            s.setStepNo(i + 1);
            steps.add(s);
        }
        when(stepMapper.selectByProcessId(1L)).thenReturn(steps);
        ProcessService svc = newSvc();
        Result<Map<String, Object>> r = svc.getRoute(1L, "v1");
        assertEquals(0, r.getCode());
        assertEquals(5, r.getData().get("stepCount"));
    }

    @Test void integ_bind_to_drawing() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(routeMapper.selectByDrawingIdAndVersion(any(), any())).thenReturn(null);
        when(routeMapper.insert(any(CrmProcessRoute.class))).thenReturn(1);
        ProcessService svc = newSvc();
        BindRouteRequest req = new BindRouteRequest();
        req.setDrawingId(1L);
        req.setVersion("v1");
        Result<CrmProcessRoute> r = svc.bindToDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
    }

    @Test void integ_bind_to_drawing_reject_duplicate() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        CrmProcessRoute existing = new CrmProcessRoute();
        existing.setId(99L);
        when(routeMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(existing);
        ProcessService svc = newSvc();
        BindRouteRequest req = new BindRouteRequest();
        req.setDrawingId(1L);
        req.setVersion("v1");
        Result<CrmProcessRoute> r = svc.bindToDrawing(1L, req, 1001L);
        assertEquals(40905, r.getCode());
    }

    @Test void integ_bind_with_change_reason() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(routeMapper.selectByDrawingIdAndVersion(any(), any())).thenReturn(null);
        when(routeMapper.insert(any(CrmProcessRoute.class))).thenReturn(1);
        ProcessService svc = newSvc();
        BindRouteRequest req = new BindRouteRequest();
        req.setDrawingId(1L);
        req.setVersion("v1");
        req.setChangeReason("client requested change");
        Result<CrmProcessRoute> r = svc.bindToDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("client requested change", r.getData().getChangeReason());
    }

    // ===== CrossModule 3 测例 =====
            @Test void crossmodule_5_segment_aggregator_for_1_9() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-I-0005");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        // 5 段聚合 = 100+200+300+400+500 = 1500
        // V1.3.8 Sprint 8 Story 8.1：BigDecimal scale-insensitive 比较（compareTo）
            assertEquals(0, new BigDecimal("1500.00").compareTo(r.getData().getTotalCost()));
        // 为 1.9 BOM 提供 5 段成本
            assertTrue(r.getData().getCostBreakdown().contains("原材料"));
    }

    @Test void crossmodule_process_reuse_for_multiple_drawings() {
        CrmProcess p = makeProcess(1L);
        p.setIsReusable(1);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(routeMapper.selectByDrawingIdAndVersion(any(), any())).thenReturn(null);
        when(routeMapper.insert(any(CrmProcessRoute.class))).thenReturn(1);
        ProcessService svc = newSvc();
        BindRouteRequest req1 = new BindRouteRequest();
        req1.setDrawingId(1L);
        req1.setVersion("v1");
        Result<CrmProcessRoute> r1 = svc.bindToDrawing(1L, req1, 1001L);
        assertEquals(0, r1.getCode());

        BindRouteRequest req2 = new BindRouteRequest();
        req2.setDrawingId(2L);
        req2.setVersion("v1");
        Result<CrmProcessRoute> r2 = svc.bindToDrawing(1L, req2, 1001L);
        assertEquals(0, r2.getCode());
    }

    @Test void crossmodule_process_to_1_8_conversion() {
        CrmProcess p = makeProcess(1L);
        p.setDrawingId(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.selectByProcessId(1L)).thenReturn(new ArrayList<>());
        ProcessService svc = newSvc();
        Result<Map<String, Object>> r = svc.getRoute(1L, "v1");
        assertEquals(0, r.getCode());
        // 1.8 工程转化引用此工艺的 5 段成本
            assertNotNull(r.getData());
    }
}
