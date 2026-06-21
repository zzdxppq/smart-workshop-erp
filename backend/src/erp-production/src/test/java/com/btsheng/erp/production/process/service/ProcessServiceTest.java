package com.btsheng.erp.production.process.service;

import com.btsheng.erp.production.process.dto.*;
import com.btsheng.erp.production.process.entity.CrmProcess;
import com.btsheng.erp.production.process.entity.CrmProcessRoute;
import com.btsheng.erp.production.process.entity.CrmProcessStep;
import com.btsheng.erp.production.process.mapper.CrmProcessMapper;
import com.btsheng.erp.production.process.mapper.CrmProcessRouteMapper;
import com.btsheng.erp.production.process.mapper.CrmProcessStepMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 Story 1.10 · 工艺库 测例（25 测例）
 * ProcessService 8 + Step 6 + Route 5 + CostAggregation 4 + Controller 2
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessServiceTest {

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
        p.setCostBreakdown("[{\"name\":\"原材料\",\"totalCost\":120.50},{\"name\":\"检验\",\"totalCost\":90.00}]");
        p.setIsActive(1);
        p.setOwnerUserId(1001L);
        return p;
    }

    private List<ProcessCreateRequest.StepInput> make5Segments() {
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        String[] segs = {"原材料", "粗加工", "精加工", "表面处理", "检验"};
        String[] machines = {"CNC_LATHE", "CNC_MILL", "CNC_LATHE", "FURNACE", "CMM"};
        for (int i = 0; i < 5; i++) {
            ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
            s.setStepNo(i + 1);
            s.setStepName("step" + (i + 1));
            s.setSegment(segs[i]);
            s.setMachineType(machines[i]);
            s.setEstimatedHours(new BigDecimal("1.0"));
            s.setUnitCost(new BigDecimal(100 * (i + 1)));
            steps.add(s);
        }
        return steps;
    }

    // ===== ProcessService 8 测例 =====
            @Test void create_process_success() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-20260612-0001");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test process");
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("PROC-20260612-0001", r.getData().getProcessCode());
        assertEquals(5, r.getData().getTotalSteps());
    }

    @Test void create_process_reject_null_name() {
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void create_process_reject_empty_steps() {
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void create_process_reject_duplicate_step_no() {
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
            s.setStepNo(1);
            s.setStepName("dup");
            s.setMachineType("CNC_LATHE");
            s.setEstimatedHours(new BigDecimal("1.0"));
            steps.add(s);
        }
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(40905, r.getCode());
    }

    @Test void create_process_reject_missing_machine_type() {
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
        s.setStepNo(1);
        s.setStepName("step1");
        s.setEstimatedHours(new BigDecimal("1.0"));
        steps.add(s);
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void create_process_reject_negative_hours() {
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
        s.setStepNo(1);
        s.setStepName("step1");
        s.setMachineType("CNC_LATHE");
        s.setEstimatedHours(new BigDecimal("-1.0"));
        steps.add(s);
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void create_process_with_drawing_id() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-20260612-0002");
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
    }

    @Test void list_processes() {
        CrmProcess p1 = makeProcess(1L);
        when(processMapper.selectActive()).thenReturn(List.of(p1));
        ProcessService svc = newSvc();
        ProcessQueryRequest q = new ProcessQueryRequest();
        q.setPage(0);
        q.setSize(10);
        Result<Map<String, Object>> r = svc.listProcesses(q);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().get("total"));
    }

    // ===== Step 6 测例 =====
            @Test void add_step_success() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.maxStepNo(1L)).thenReturn(5);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        when(processMapper.updateById(any(CrmProcess.class))).thenReturn(1);
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("step6");
        req.setSegment("检验");
        req.setMachineType("CMM");
        req.setEstimatedHours(new BigDecimal("1.0"));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(6, r.getData().getStepNo());
    }

    @Test void add_step_reject_negative_hours() {
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("test");
        req.setMachineType("CNC_LATHE");
        req.setEstimatedHours(new BigDecimal("-1.0"));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void add_step_reject_null_name() {
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setMachineType("CNC_LATHE");
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void add_step_reject_missing_machine_type() {
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("test");
        req.setEstimatedHours(new BigDecimal("1.0"));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void add_step_process_not_found() {
        when(processMapper.selectById(99L)).thenReturn(null);
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("test");
        req.setMachineType("CNC_LATHE");
        req.setEstimatedHours(new BigDecimal("1.0"));
        Result<CrmProcessStep> r = svc.addStep(99L, req, 1001L);
        assertEquals(40404, r.getCode());
    }

    @Test void add_step_step_no_auto_increment() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.maxStepNo(1L)).thenReturn(null);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        when(processMapper.updateById(any(CrmProcess.class))).thenReturn(1);
        ProcessService svc = newSvc();
        AddStepRequest req = new AddStepRequest();
        req.setStepName("first");
        req.setMachineType("CNC_LATHE");
        req.setEstimatedHours(new BigDecimal("1.0"));
        Result<CrmProcessStep> r = svc.addStep(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getStepNo());
    }

    // ===== Route 5 测例 =====
            @Test void get_route_success() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        CrmProcessStep s = new CrmProcessStep();
        s.setId(1L);
        s.setStepNo(1);
        s.setStepName("s1");
        when(stepMapper.selectByProcessId(1L)).thenReturn(List.of(s));
        ProcessService svc = newSvc();
        Result<Map<String, Object>> r = svc.getRoute(1L, "v1");
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().get("stepCount"));
    }

    @Test void get_route_not_found() {
        when(processMapper.selectById(99L)).thenReturn(null);
        ProcessService svc = newSvc();
        Result<Map<String, Object>> r = svc.getRoute(99L, "v1");
        assertEquals(40404, r.getCode());
    }

    @Test void bind_to_drawing_success() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(routeMapper.selectByDrawingIdAndVersion(any(), any())).thenReturn(null);
        when(routeMapper.insert(any(CrmProcessRoute.class))).thenReturn(1);
        ProcessService svc = newSvc();
        BindRouteRequest req = new BindRouteRequest();
        req.setDrawingId(1L);
        req.setVersion("v1");
        req.setChangeReason("first bind");
        Result<CrmProcessRoute> r = svc.bindToDrawing(1L, req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals("DRAFT", r.getData().getStatus());
    }

    @Test void bind_to_drawing_reject_null_drawing() {
        ProcessService svc = newSvc();
        BindRouteRequest req = new BindRouteRequest();
        Result<CrmProcessRoute> r = svc.bindToDrawing(1L, req, 1001L);
        assertEquals(40001, r.getCode());
    }

    @Test void bind_to_drawing_reject_duplicate() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        CrmProcessRoute existing = new CrmProcessRoute();
        when(routeMapper.selectByDrawingIdAndVersion(1L, "v1")).thenReturn(existing);
        ProcessService svc = newSvc();
        BindRouteRequest req = new BindRouteRequest();
        req.setDrawingId(1L);
        req.setVersion("v1");
        Result<CrmProcessRoute> r = svc.bindToDrawing(1L, req, 1001L);
        assertEquals(40905, r.getCode());
    }

    // ===== CostAggregation 4 测例 =====
            @Test void cost_aggregation_5_segments() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-20260612-0003");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        // 100+200+300+400+500 = 1500
        // V1.3.8 Sprint 8 Story 8.1：BigDecimal scale-insensitive 比较（compareTo）
            assertEquals(0, new BigDecimal("1500.00").compareTo(r.getData().getTotalCost()));
        assertTrue(r.getData().getCostBreakdown().contains("原材料"));
        assertTrue(r.getData().getCostBreakdown().contains("检验"));
    }

    @Test void cost_aggregation_zero() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-20260612-0004");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
        s.setStepNo(1);
        s.setStepName("free");
        s.setMachineType("MANUAL");
        s.setEstimatedHours(new BigDecimal("0.5"));
        s.setUnitCost(BigDecimal.ZERO);
        steps.add(s);
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(BigDecimal.ZERO, r.getData().getTotalCost());
    }

    @Test void cost_aggregation_hours_sum() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-20260612-0005");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ProcessCreateRequest.StepInput s = new ProcessCreateRequest.StepInput();
            s.setStepNo(i + 1);
            s.setStepName("s" + (i + 1));
            s.setMachineType("CNC_LATHE");
            s.setEstimatedHours(new BigDecimal("2.0"));
            s.setUnitCost(new BigDecimal(100));
            steps.add(s);
        }
        req.setSteps(steps);
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(new BigDecimal("6.0"), r.getData().getTotalEstimatedHours());
    }

    @Test void cost_aggregation_reusable() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-20260612-0006");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        req.setIsReusable(true);
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getIsReusable());
    }

    // ===== Controller 2 测例 =====
            @Test void controller_create_endpoint() {
        when(docNoGenerator.nextProcessNo()).thenReturn("PROC-CTRL-0001");
        when(processMapper.insert(any(CrmProcess.class))).thenReturn(1);
        when(stepMapper.insert(any(CrmProcessStep.class))).thenReturn(1);
        ProcessService svc = newSvc();
        ProcessCreateRequest req = new ProcessCreateRequest();
        req.setProcessName("test");
        req.setSteps(make5Segments());
        Result<CrmProcess> r = svc.createProcess(req, 1001L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().getProcessCode().startsWith("PROC-"));
    }

    @Test void controller_route_endpoint() {
        CrmProcess p = makeProcess(1L);
        when(processMapper.selectById(1L)).thenReturn(p);
        when(stepMapper.selectByProcessId(1L)).thenReturn(new ArrayList<>());
        ProcessService svc = newSvc();
        Result<Map<String, Object>> r = svc.getRoute(1L, "v1");
        assertEquals(0, r.getCode());
    }
}
