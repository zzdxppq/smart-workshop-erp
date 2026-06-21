package com.btsheng.erp.production.process.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.production.integration.BusinessMaterialRouteGateway;
import com.btsheng.erp.production.material.entity.CrmMaterial;
import com.btsheng.erp.production.material.entity.MdmProcess;
import com.btsheng.erp.production.material.entity.MdmProductRoute;
import com.btsheng.erp.production.process.dto.ProcessCreateRequest;
import com.btsheng.erp.production.process.dto.ProductRouteCreateRequest;
import com.btsheng.erp.production.process.entity.CrmProcess;
import com.btsheng.erp.production.process.entity.CrmProcessRoute;
import com.btsheng.erp.production.process.mapper.CrmProcessRouteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** E3-S3.4 · 产品工艺路线（cnc_business 经 Feign · cnc_production 写 crm_process） */
@Service
public class ProductRouteService {

    private static final String[] SEGMENTS = {"原材料", "粗加工", "精加工", "表面处理", "检验"};

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_RELEASED = "RELEASED";

    private final BusinessMaterialRouteGateway businessGateway;
    private final ProcessService processService;
    private final CrmProcessRouteMapper routeMapper;

    @Autowired
    public ProductRouteService(BusinessMaterialRouteGateway businessGateway,
                               ProcessService processService,
                               CrmProcessRouteMapper routeMapper) {
        this.businessGateway = businessGateway;
        this.processService = processService;
        this.routeMapper = routeMapper;
    }

    @Transactional
    @AuditLog(module = "process", action = "product_route.create")
    public Result<Map<String, Object>> createProductRoute(String productId, ProductRouteCreateRequest req, Long operatorUserId) {
        CrmMaterial material = businessGateway.resolveMaterial(productId);
        if (material == null) {
            return Result.fail(40404, "PRODUCT_NOT_FOUND");
        }
        if (req == null || req.getProcesses() == null || req.getProcesses().isEmpty()) {
            return Result.fail(40001, "PROCESSES_REQUIRED");
        }
        List<ProductRouteCreateRequest.RouteProcessInput> sorted = new ArrayList<>(req.getProcesses());
        sorted.sort(Comparator.comparingInt(p -> p.getProcessSeq() != null ? p.getProcessSeq() : 0));

        ProcessCreateRequest createReq = new ProcessCreateRequest();
        createReq.setProcessName(material.getMaterialName() + " 产品工艺路线");
        createReq.setProcessType("STANDARD");
        createReq.setComment(req.getChangeReason());
        List<ProcessCreateRequest.StepInput> steps = new ArrayList<>();
        int seq = 1;
        for (ProductRouteCreateRequest.RouteProcessInput item : sorted) {
            if (item.getProcessCode() == null || item.getProcessCode().isBlank()) {
                return Result.fail(40001, "PROCESS_CODE_REQUIRED");
            }
            MdmProcess mdm = businessGateway.getMdmProcess(item.getProcessCode());
            if (mdm == null) {
                return Result.fail(40404, "MDM_PROCESS_NOT_FOUND:" + item.getProcessCode());
            }
            int stepNo = item.getProcessSeq() != null ? item.getProcessSeq() : seq;
            ProcessCreateRequest.StepInput step = new ProcessCreateRequest.StepInput();
            step.setStepNo(stepNo);
            step.setStepName(mdm.getProcessName());
            step.setSegment(SEGMENTS[(stepNo - 1) % SEGMENTS.length]);
            step.setMachineType(mdm.getMachineType() != null ? mdm.getMachineType() : "CNC");
            BigDecimal hours = item.getStdTimeMin() != null ? item.getStdTimeMin()
                    : (mdm.getStdTimeMin() != null ? mdm.getStdTimeMin() : BigDecimal.ZERO);
            step.setEstimatedHours(hours.divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP));
            step.setUnitCost(mdm.getUnitPrice() != null ? mdm.getUnitPrice() : BigDecimal.ZERO);
            steps.add(step);
            seq++;
        }
        createReq.setSteps(steps);

        Result<CrmProcess> created = processService.createProcess(createReq, operatorUserId);
        if (!created.isSuccess()) {
            return Result.fail(created.getCode(), created.getMessage());
        }

        List<MdmProductRoute> routes = new ArrayList<>();
        for (ProductRouteCreateRequest.RouteProcessInput item : sorted) {
            MdmProductRoute row = new MdmProductRoute();
            row.setProductCode(material.getMaterialCode());
            row.setProcessSeq(item.getProcessSeq() != null ? item.getProcessSeq() : 1);
            row.setProcessCode(item.getProcessCode());
            row.setIsOutsource(Boolean.TRUE.equals(item.getIsOutsource()));
            routes.add(row);
        }
        businessGateway.replaceRoutes(material.getMaterialCode(), routes);
        businessGateway.updateMaterialProcessId(material.getId(), created.getData().getId());

        String routeStatus = STATUS_DRAFT;
        if (req.getDrawingId() != null) {
            routeStatus = upsertDrawingRoute(req.getDrawingId(),
                    req.getDrawingNo() != null ? req.getDrawingNo() : material.getMaterialCode(),
                    created.getData(), operatorUserId, req.getChangeReason(), STATUS_DRAFT);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("productId", material.getId());
        resp.put("productCode", material.getMaterialCode());
        resp.put("processId", created.getData().getId());
        resp.put("processCode", created.getData().getProcessCode());
        resp.put("routeStatus", routeStatus);
        resp.put("routes", businessGateway.listRoutes(material.getMaterialCode()));
        return Result.ok(resp);
    }

    @Transactional
    @AuditLog(module = "process", action = "product_route.publish")
    public Result<Map<String, Object>> publishProductRoute(String productId, Long drawingId, Long operatorUserId) {
        CrmMaterial material = businessGateway.resolveMaterial(productId);
        if (material == null) {
            return Result.fail(40404, "PRODUCT_NOT_FOUND");
        }
        if (material.getProcessId() == null) {
            return Result.fail(40903, "PRODUCT_ROUTE_NOT_SAVED");
        }
        CrmProcessRoute route = null;
        if (drawingId != null) {
            route = routeMapper.selectByDrawingIdAndVersion(drawingId, "v1");
            if (route == null) {
                List<CrmProcessRoute> routes = routeMapper.selectByDrawingId(drawingId);
                route = routes.isEmpty() ? null : routes.get(0);
            }
        }
        if (route == null) {
            route = routeMapper.selectLatestByProcessId(material.getProcessId());
        }
        if (route == null) {
            return Result.fail(40404, "PROCESS_ROUTE_BINDING_NOT_FOUND");
        }
        route.setStatus(STATUS_RELEASED);
        route.setReleasedBy(operatorUserId);
        route.setReleasedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());
        routeMapper.updateById(route);

        Map<String, Object> resp = new HashMap<>();
        resp.put("productId", material.getId());
        resp.put("productCode", material.getMaterialCode());
        resp.put("processId", material.getProcessId());
        resp.put("routeStatus", STATUS_RELEASED);
        resp.put("routes", businessGateway.listRoutes(material.getMaterialCode()));
        if (material.getProcessId() != null) {
            Result<Map<String, Object>> detail = processService.getRoute(material.getProcessId(), null);
            if (detail.isSuccess()) {
                resp.put("process", detail.getData().get("process"));
                resp.put("steps", detail.getData().get("steps"));
            }
        }
        return Result.ok(resp);
    }

    private String upsertDrawingRoute(Long drawingId, String drawingNo, CrmProcess process,
                                      Long operatorUserId, String changeReason, String status) {
        CrmProcessRoute existing = routeMapper.selectByDrawingIdAndVersion(drawingId, "v1");
        if (existing == null) {
            List<CrmProcessRoute> list = routeMapper.selectByDrawingId(drawingId);
            existing = list.isEmpty() ? null : list.get(0);
        }
        if (existing != null) {
            existing.setProcessId(process.getId());
            existing.setProcessCode(process.getProcessCode());
            existing.setStatus(status);
            existing.setChangeReason(changeReason);
            existing.setUpdatedAt(LocalDateTime.now());
            routeMapper.updateById(existing);
            return existing.getStatus();
        }
        CrmProcessRoute r = new CrmProcessRoute();
        r.setDrawingId(drawingId);
        r.setDrawingNo(drawingNo != null ? drawingNo : "DWG-" + drawingId);
        r.setProcessId(process.getId());
        r.setProcessCode(process.getProcessCode());
        r.setVersion("v1");
        r.setStatus(status);
        r.setChangeReason(changeReason);
        r.setCreatedBy(operatorUserId);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        routeMapper.insert(r);
        return status;
    }

    public Result<Map<String, Object>> getProductRoute(String productId) {
        CrmMaterial material = businessGateway.resolveMaterial(productId);
        if (material == null) {
            return Result.fail(40404, "PRODUCT_NOT_FOUND");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("productId", material.getId());
        resp.put("productCode", material.getMaterialCode());
        resp.put("processId", material.getProcessId());
        resp.put("routes", businessGateway.listRoutes(material.getMaterialCode()));
        String routeStatus = STATUS_DRAFT;
        if (material.getProcessId() != null) {
            CrmProcessRoute binding = routeMapper.selectLatestByProcessId(material.getProcessId());
            if (binding != null) {
                routeStatus = binding.getStatus() != null ? binding.getStatus() : STATUS_DRAFT;
            }
            Result<Map<String, Object>> route = processService.getRoute(material.getProcessId(), null);
            if (route.isSuccess()) {
                resp.put("process", route.getData().get("process"));
                resp.put("steps", route.getData().get("steps"));
            }
        }
        resp.put("routeStatus", routeStatus);
        return Result.ok(resp);
    }

    @Transactional
    @AuditLog(module = "process", action = "product_route.copy_from")
    public Result<Map<String, Object>> copyProductRouteFrom(String targetProductId, String srcProductId, Long operatorUserId) {
        CrmMaterial target = businessGateway.resolveMaterial(targetProductId);
        CrmMaterial source = businessGateway.resolveMaterial(srcProductId);
        if (target == null || source == null) {
            return Result.fail(40404, "PRODUCT_NOT_FOUND");
        }
        if (source.getProcessId() == null) {
            return Result.fail(40903, "SOURCE_PRODUCT_HAS_NO_ROUTE");
        }
        List<MdmProductRoute> srcRoutes = businessGateway.listRoutes(source.getMaterialCode());
        if (srcRoutes.isEmpty()) {
            return Result.fail(40903, "SOURCE_PRODUCT_ROUTE_EMPTY");
        }

        Result<CrmProcess> copied = processService.copyProcess(
                source.getProcessId(),
                target.getMaterialName() + " 工艺路线(复制自 " + source.getMaterialCode() + ")",
                operatorUserId);
        if (!copied.isSuccess()) {
            return Result.fail(copied.getCode(), copied.getMessage());
        }

        List<MdmProductRoute> copiedRoutes = new ArrayList<>();
        for (MdmProductRoute src : srcRoutes) {
            MdmProductRoute row = new MdmProductRoute();
            row.setProductCode(target.getMaterialCode());
            row.setProcessSeq(src.getProcessSeq());
            row.setProcessCode(src.getProcessCode());
            row.setIsOutsource(src.getIsOutsource());
            copiedRoutes.add(row);
        }
        businessGateway.replaceRoutes(target.getMaterialCode(), copiedRoutes);
        businessGateway.updateMaterialProcessId(target.getId(), copied.getData().getId());

        Map<String, Object> resp = new HashMap<>();
        resp.put("productId", target.getId());
        resp.put("productCode", target.getMaterialCode());
        resp.put("copiedFrom", source.getMaterialCode());
        resp.put("processId", copied.getData().getId());
        resp.put("routes", businessGateway.listRoutes(target.getMaterialCode()));
        return Result.ok(resp);
    }
}
