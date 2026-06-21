package com.btsheng.erp.business.crm.quality.controller;

import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickup;
import com.btsheng.erp.business.crm.quality.pickup.mapper.CrmQualityPickupMapper;
import com.btsheng.erp.business.crm.quality.pickup.service.QualityPickupService;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmm;
import com.btsheng.erp.business.crm.qualitycmm.service.QualityCmmService;
import com.btsheng.erp.business.crm.qualitydefect.dto.DefectCreateRequest;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefect;
import com.btsheng.erp.business.crm.qualitydefect.service.QualityDefectService;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFa;
import com.btsheng.erp.business.crm.qualityfa.service.QualityFaService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.CurrentUserHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Web 品质模块路径别名（/quality/fa|cmm|defects|pickups） */
@Tag(name = "E7-Quality-Web", description = "品质 Web 端点")
@RestController
@RequestMapping("/quality")
public class QualityWebController {

    private final QualityFaService faService;
    private final QualityCmmService cmmService;
    private final QualityDefectService defectService;
    private final QualityPickupService pickupService;
    private final CrmQualityPickupMapper pickupMapper;

    public QualityWebController(QualityFaService faService,
                                QualityCmmService cmmService,
                                QualityDefectService defectService,
                                QualityPickupService pickupService,
                                CrmQualityPickupMapper pickupMapper) {
        this.faService = faService;
        this.cmmService = cmmService;
        this.defectService = defectService;
        this.pickupService = pickupService;
        this.pickupMapper = pickupMapper;
    }

    // ── FA ──

    @GetMapping("/fa")
    @Operation(summary = "FA 首件列表（Web 路径）")
    public Result<Map<String, Object>> listFa(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Result<List<CrmQualityFa>> raw = faService.list(null, null, null);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw.isSuccess() && raw.getData() != null) {
            for (CrmQualityFa fa : raw.getData()) {
                if (keyword != null && !keyword.isBlank()) {
                    String k = keyword.trim().toLowerCase();
                    boolean hit = (fa.getFaNo() != null && fa.getFaNo().toLowerCase().contains(k))
                            || (fa.getWorkOrderNo() != null && fa.getWorkOrderNo().toLowerCase().contains(k));
                    if (!hit) continue;
                }
                rows.add(faRow(fa));
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/fa/{id}")
    @Operation(summary = "FA 详情（Web 路径）")
    public Result<Map<String, Object>> faDetail(@PathVariable Long id) {
        Result<List<CrmQualityFa>> raw = faService.list(null, null, null);
        if (!raw.isSuccess() || raw.getData() == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "FA 不存在");
        }
        return raw.getData().stream()
                .filter(f -> id.equals(f.getId()))
                .findFirst()
                .map(f -> Result.ok(faRow(f)))
                .orElseGet(() -> Result.fail(Result.CODE_NOT_FOUND, "FA 不存在"));
    }

    @PostMapping("/fa/{id}/submit")
    @Operation(summary = "FA 提交/放行（Web 路径）")
    public Result<?> submitFa(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = CurrentUserHelper.currentUserId();
        long uid = userId == null ? 401L : userId;
        if (body != null && "FAIL".equalsIgnoreCase(String.valueOf(body.get("result")))) {
            String reason = body.get("reason") != null ? String.valueOf(body.get("reason")) : null;
            return faService.reject(id, reason, uid);
        }
        return faService.pass(id, uid);
    }

    @GetMapping("/fa/{id}/report")
    @Operation(summary = "FA 报告（Web 路径）")
    public Result<Map<String, Object>> faReport(@PathVariable Long id) {
        Result<List<CrmQualityFa>> raw = faService.list(null, null, null);
        if (!raw.isSuccess() || raw.getData() == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "FA 不存在");
        }
        return raw.getData().stream()
                .filter(f -> id.equals(f.getId()))
                .findFirst()
                .map(f -> {
                    Map<String, Object> report = new HashMap<>(faRow(f));
                    report.put("pdfUrl", f.getPdfUrl());
                    return Result.ok(report);
                })
                .orElseGet(() -> Result.fail(Result.CODE_NOT_FOUND, "FA 不存在"));
    }

    // ── CMM ──

    @GetMapping("/cmm")
    @Operation(summary = "CMM 列表（Web 路径）")
    public Result<Map<String, Object>> listCmm(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Result<List<CrmQualityCmm>> raw = cmmService.listCmms(null, null);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw.isSuccess() && raw.getData() != null) {
            for (CrmQualityCmm c : raw.getData()) {
                if (keyword != null && !keyword.isBlank()) {
                    String k = keyword.trim().toLowerCase();
                    boolean hit = (c.getCmmNo() != null && c.getCmmNo().toLowerCase().contains(k))
                            || (c.getWorkOrderNo() != null && c.getWorkOrderNo().toLowerCase().contains(k));
                    if (!hit) continue;
                }
                rows.add(cmmRow(c));
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/cmm/{id}")
    @Operation(summary = "CMM 详情（Web 路径）")
    public Result<Map<String, Object>> cmmDetail(@PathVariable Long id) {
        Result<Map<String, Object>> report = cmmService.getReport(id);
        if (report.isSuccess()) {
            return report;
        }
        Result<List<CrmQualityCmm>> raw = cmmService.listCmms(null, null);
        if (raw.isSuccess() && raw.getData() != null) {
            for (CrmQualityCmm c : raw.getData()) {
                if (id.equals(c.getId())) {
                    return Result.ok(cmmRow(c));
                }
            }
        }
        return Result.fail(Result.CODE_NOT_FOUND, "CMM 不存在");
    }

    @PostMapping("/cmm/{id}/submit")
    @Operation(summary = "CMM 提交（Web 路径 · 占位）")
    public Result<Map<String, Object>> submitCmm(@PathVariable Long id) {
        Result<List<CrmQualityCmm>> raw = cmmService.listCmms(null, null);
        if (raw.isSuccess() && raw.getData() != null) {
            for (CrmQualityCmm c : raw.getData()) {
                if (id.equals(c.getId())) {
                    return Result.ok(cmmRow(c));
                }
            }
        }
        return Result.fail(Result.CODE_NOT_FOUND, "CMM 不存在");
    }

    @GetMapping("/cmm/{id}/report")
    @Operation(summary = "CMM 报告（Web 路径）")
    public Result<Map<String, Object>> cmmReport(@PathVariable Long id) {
        Result<Map<String, Object>> raw = cmmService.getReport(id);
        if (raw.isSuccess()) {
            return raw;
        }
        Result<List<CrmQualityCmm>> list = cmmService.listCmms(null, null);
        if (list.isSuccess() && list.getData() != null) {
            for (CrmQualityCmm c : list.getData()) {
                if (id.equals(c.getId())) {
                    return Result.ok(cmmRow(c));
                }
            }
        }
        return Result.fail(Result.CODE_NOT_FOUND, "CMM 不存在");
    }

    // ── Defects ──

    @GetMapping("/defects")
    @Operation(summary = "不良品列表（Web 路径）")
    public Result<Map<String, Object>> listDefects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Result<List<CrmQualityDefect>> raw = defectService.list(null, null, null);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw.isSuccess() && raw.getData() != null) {
            for (CrmQualityDefect d : raw.getData()) {
                if (level != null && !level.isBlank() && !level.equalsIgnoreCase(d.getSeverity())) continue;
                if (keyword != null && !keyword.isBlank()) {
                    String k = keyword.trim().toLowerCase();
                    boolean hit = (d.getDefectNo() != null && d.getDefectNo().toLowerCase().contains(k))
                            || (d.getWorkOrderNo() != null && d.getWorkOrderNo().toLowerCase().contains(k));
                    if (!hit) continue;
                }
                rows.add(defectRow(d));
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/defects/{id}")
    @Operation(summary = "不良品详情（Web 路径）")
    public Result<Map<String, Object>> defectDetail(@PathVariable Long id) {
        return defectService.list(null, null, null).getData().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst()
                .map(d -> Result.ok(defectRow(d)))
                .orElseGet(() -> Result.fail(Result.CODE_NOT_FOUND, "不良品单不存在"));
    }

    @PostMapping("/defects")
    @Operation(summary = "登记不良品（Web 路径）")
    public Result<CrmQualityDefect> createDefect(@RequestBody DefectCreateRequest req) {
        Long userId = CurrentUserHelper.currentUserId();
        return defectService.createDefect(req, userId == null ? 401L : userId);
    }

    @GetMapping("/defects/report")
    @Operation(summary = "不良品汇总报告（Web 路径）")
    public Result<Map<String, Object>> defectReport(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status) {
        Result<List<CrmQualityDefect>> raw = defectService.list(sourceType, status, null);
        Map<String, Object> report = new HashMap<>();
        report.put("items", raw.getData());
        report.put("total", raw.getData() == null ? 0 : raw.getData().size());
        return Result.ok(report);
    }

    // ── Pickups ──

    @GetMapping("/pickups")
    @Operation(summary = "提货检列表（Web 路径）")
    public Result<Map<String, Object>> listPickups(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<CrmQualityPickup> all = pickupMapper.selectList(null);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (all != null) {
            for (CrmQualityPickup p : all) {
                if (keyword != null && !keyword.isBlank()) {
                    String k = keyword.trim().toLowerCase();
                    boolean hit = (p.getPickupNo() != null && p.getPickupNo().toLowerCase().contains(k))
                            || (p.getScanNo() != null && p.getScanNo().toLowerCase().contains(k));
                    if (!hit) continue;
                }
                rows.add(pickupRow(p));
            }
        }
        return Result.ok(pageSlice(rows, pageNum, pageSize));
    }

    @GetMapping("/pickups/{id}")
    @Operation(summary = "提货检详情（Web 路径）")
    public Result<Map<String, Object>> pickupDetail(@PathVariable Long id) {
        CrmQualityPickup p = pickupMapper.selectById(id);
        if (p == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "领料单不存在");
        }
        return pickupService.getPickup(p.getPickupNo());
    }

    @PostMapping("/pickups/{id}/inspect")
    @Operation(summary = "提货检执行（Web 路径）")
    public Result<Map<String, Object>> inspectPickup(@PathVariable Long id,
                                                      @RequestBody List<com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickupItem> items) {
        CrmQualityPickup p = pickupMapper.selectById(id);
        if (p == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "领料单不存在");
        }
        return pickupService.inspectPickup(p.getPickupNo(), items);
    }

    private static Map<String, Object> faRow(CrmQualityFa fa) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", fa.getId());
        row.put("faNo", fa.getFaNo());
        row.put("workorderNo", fa.getWorkOrderNo());
        row.put("materialCode", fa.getProcessName());
        row.put("inspector", fa.getInspectorUserId());
        row.put("status", fa.getResult());
        row.put("inspectedAt", fa.getInspectedAt());
        return row;
    }

    private static Map<String, Object> cmmRow(CrmQualityCmm c) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", c.getId());
        row.put("cmmNo", c.getCmmNo());
        row.put("workorderNo", c.getWorkOrderNo());
        row.put("drawingNo", c.getDrawingNo());
        row.put("partName", c.getPartName());
        row.put("status", c.getResult());
        row.put("cpk", c.getCpk());
        row.put("inspectedAt", c.getInspectedAt());
        return row;
    }

    private static Map<String, Object> defectRow(CrmQualityDefect d) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", d.getId());
        row.put("defectNo", d.getDefectNo());
        row.put("workorderNo", d.getWorkOrderNo());
        row.put("materialCode", d.getMaterialCode());
        row.put("qty", d.getQty());
        row.put("level", d.getSeverity());
        row.put("reworkable", !"SCRAP".equalsIgnoreCase(d.getResult()));
        row.put("reporter", d.getCreatedBy());
        row.put("status", d.getStatus());
        return row;
    }

    private static Map<String, Object> pickupRow(CrmQualityPickup p) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", p.getId());
        row.put("pickupNo", p.getPickupNo());
        row.put("scanNo", p.getScanNo());
        row.put("vendorName", p.getVendorName());
        row.put("status", p.getInspectStatus());
        row.put("inspectStatus", p.getInspectStatus());
        row.put("inspectedAt", p.getInspectedAt());
        return row;
    }

    private static Map<String, Object> pageSlice(List<Map<String, Object>> all, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, all.size());
        List<Map<String, Object>> slice = from < all.size() ? all.subList(from, to) : List.of();
        Map<String, Object> page = new HashMap<>();
        page.put("items", slice);
        page.put("records", slice);
        page.put("list", slice);
        page.put("total", all.size());
        page.put("pageNum", pageNum);
        page.put("pageSize", pageSize);
        return page;
    }
}
