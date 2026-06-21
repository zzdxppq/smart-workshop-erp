package com.btsheng.erp.business.crm.engineer.controller;

import com.btsheng.erp.business.crm.engineer.entity.CrmBomDetailItem;
import com.btsheng.erp.business.crm.engineer.entity.CrmEngineeringWorkbench;
import com.btsheng.erp.business.crm.engineer.entity.CrmProcessDetail;
import com.btsheng.erp.business.crm.engineer.service.EngineeringWorkbenchService;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.CurrentUserHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * V2.1 · 工程转化工作台 Controller
 */
@Tag(name = "E3-Engineering-Workbench", description = "工程转化工作台（V2.1）")
@RestController
@RequestMapping("/engineering-workbench")
public class EngineeringWorkbenchController {

    private final EngineeringWorkbenchService workbenchService;

    @Autowired
    public EngineeringWorkbenchController(EngineeringWorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @Operation(summary = "获取订单的工程转化工作台列表（V2.1）")
    @GetMapping("/order/{orderId}")
    public Result<List<CrmEngineeringWorkbench>> getByOrder(@PathVariable("orderId") Long orderId) {
        return workbenchService.getWorkbenchByOrder(orderId);
    }

    @Operation(summary = "确保订单工程转化工作台存在（V2.1）")
    @PostMapping("/ensure/{orderId}")
    public Result<List<CrmEngineeringWorkbench>> ensure(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "engineerUserId", required = false) Long engineerUserId) {
        Long userId = engineerUserId != null ? engineerUserId : CurrentUserHelper.currentUserId();
        return workbenchService.ensureWorkbenchForOrder(orderId, userId);
    }

    @Operation(summary = "获取工程师待处理的工作台列表（V2.1）")
    @GetMapping("/pending")
    public Result<List<CrmEngineeringWorkbench>> getPending(
            @RequestParam(value = "engineerUserId", required = false) Long engineerUserId) {
        Long userId = engineerUserId != null ? engineerUserId : CurrentUserHelper.currentUserId();
        return workbenchService.getPendingWorkbenches(userId);
    }

    @Operation(summary = "获取工作台详情（含工艺和BOM，V2.1）")
    @GetMapping("/{workbenchId}")
    public Result<Map<String, Object>> getDetail(@PathVariable("workbenchId") Long workbenchId) {
        return workbenchService.getWorkbenchDetail(workbenchId);
    }

    @Operation(summary = "开始工程转化（领取任务，V2.1）")
    @PostMapping("/{workbenchId}/start")
    public Result<CrmEngineeringWorkbench> start(@PathVariable("workbenchId") Long workbenchId,
                                                  @RequestParam(value = "engineerUserId", required = false) Long engineerUserId) {
        Long userId = engineerUserId != null ? engineerUserId : CurrentUserHelper.currentUserId();
        return workbenchService.startWork(workbenchId, userId);
    }

    @Operation(summary = "保存工艺明细（V2.1）")
    @PostMapping("/{workbenchId}/process")
    public Result<CrmEngineeringWorkbench> saveProcess(
            @PathVariable("workbenchId") Long workbenchId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> processList = (List<Map<String, Object>>) request.get("processes");
        List<CrmProcessDetail> processes = parseProcesses(processList);
        Long userId = CurrentUserHelper.currentUserId();
        return workbenchService.saveProcessDetail(workbenchId, processes, userId);
    }

    @Operation(summary = "保存BOM明细（V2.1）")
    @PostMapping("/{workbenchId}/bom")
    public Result<CrmEngineeringWorkbench> saveBom(
            @PathVariable("workbenchId") Long workbenchId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bomList = (List<Map<String, Object>>) request.get("bomItems");
        List<CrmBomDetailItem> bomItems = parseBomItems(bomList);
        Long userId = CurrentUserHelper.currentUserId();
        return workbenchService.saveBomDetail(workbenchId, bomItems, userId);
    }

    @Operation(summary = "提交工程转化（V2.1）")
    @PostMapping("/{workbenchId}/submit")
    public Result<CrmOrder> submit(@PathVariable("workbenchId") Long workbenchId,
                                    @RequestParam(value = "engineerUserId", required = false) Long engineerUserId) {
        Long userId = engineerUserId != null ? engineerUserId : CurrentUserHelper.currentUserId();
        return workbenchService.submitWork(workbenchId, userId);
    }

    @Operation(summary = "从报价单引用工艺（V2.1）")
    @PostMapping("/{workbenchId}/import-quote/{quoteItemId}")
    public Result<CrmEngineeringWorkbench> importFromQuote(
            @PathVariable("workbenchId") Long workbenchId,
            @PathVariable("quoteItemId") Long quoteItemId,
            @RequestParam(value = "engineerUserId", required = false) Long engineerUserId) {
        Long userId = engineerUserId != null ? engineerUserId : CurrentUserHelper.currentUserId();
        return workbenchService.importFromQuote(workbenchId, quoteItemId, userId);
    }

    @Operation(summary = "获取订单工程转化进度（V2.1）")
    @GetMapping("/order/{orderId}/progress")
    public Result<Map<String, Object>> getProgress(@PathVariable("orderId") Long orderId) {
        return workbenchService.getOrderEngineeringProgress(orderId);
    }

    @SuppressWarnings("unchecked")
    private List<CrmProcessDetail> parseProcesses(List<Map<String, Object>> processList) {
        if (processList == null) return null;
        return processList.stream().map(p -> {
            CrmProcessDetail pd = new CrmProcessDetail();
            pd.setSequence((Integer) p.getOrDefault("sequence", 0));
            pd.setProcessCode((String) p.get("processCode"));
            pd.setProcessName((String) p.get("processName"));
            pd.setMachineType((String) p.get("machineType"));
            if (p.get("machineId") != null) {
                pd.setMachineId(Long.parseLong(p.get("machineId").toString()));
            }
            if (p.get("spindleSpeed") != null) {
                pd.setSpindleSpeed(Integer.parseInt(p.get("spindleSpeed").toString()));
            }
            if (p.get("feedRate") != null) {
                pd.setFeedRate(new BigDecimal(p.get("feedRate").toString()));
            }
            if (p.get("cuttingDepth") != null) {
                pd.setCuttingDepth(new BigDecimal(p.get("cuttingDepth").toString()));
            }
            pd.setToolNo((String) p.get("toolNo"));
            pd.setToolSpec((String) p.get("toolSpec"));
            pd.setFixture((String) p.get("fixture"));
            if (p.get("unitTimeMinutes") != null) {
                pd.setUnitTimeMinutes(Integer.parseInt(p.get("unitTimeMinutes").toString()));
            }
            if (p.get("costPerHour") != null) {
                pd.setCostPerHour(new BigDecimal(p.get("costPerHour").toString()));
            }
            pd.setOutsourceFlag((Integer) p.getOrDefault("outsourceFlag", 0));
            pd.setRemark((String) p.get("remark"));
            return pd;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<CrmBomDetailItem> parseBomItems(List<Map<String, Object>> bomList) {
        if (bomList == null) return null;
        return bomList.stream().map(b -> {
            CrmBomDetailItem item = new CrmBomDetailItem();
            item.setSequence((Integer) b.getOrDefault("sequence", 0));
            item.setItemType((String) b.getOrDefault("itemType", "MATERIAL"));
            item.setMaterialCode((String) b.get("materialCode"));
            item.setMaterialName((String) b.get("materialName"));
            item.setSpec((String) b.get("spec"));
            if (b.get("quantity") != null) {
                item.setQuantity(new BigDecimal(b.get("quantity").toString()));
            }
            item.setUnit((String) b.getOrDefault("unit", "个"));
            item.setSource((String) b.getOrDefault("source", "STOCK"));
            item.setRemark((String) b.get("remark"));
            return item;
        }).toList();
    }
}
