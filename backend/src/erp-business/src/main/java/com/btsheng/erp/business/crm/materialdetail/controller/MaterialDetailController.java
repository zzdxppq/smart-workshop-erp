package com.btsheng.erp.business.crm.materialdetail.controller;

import com.btsheng.erp.business.crm.materialdetail.dto.ChangeLogEntry;
import com.btsheng.erp.business.crm.materialdetail.dto.MaterialDetailDTO;
import com.btsheng.erp.business.crm.materialdetail.service.MaterialDetailService;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.materialbarcode.service.MaterialMasterEnsureService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.8 · Story 2.1 · 料号详情 Controller
 *
 * <p>4 端点（与 Story 2.1 端点契约一致）：
 * <ul>
 *   <li>GET /materials/{id}/detail          AC-2.1.1 详情聚合</li>
 *   <li>GET /materials/{id}/price-history   AC-2.1.2 价格走势</li>
 *   <li>GET /materials/{id}/process-route   AC-2.1.2 工艺路线</li>
 *   <li>GET /materials/{id}/change-log      AC-2.1.2 历史变更</li>
 * </ul>
 *
 * <p>Redis 缓存：Sprint 7 集成阶段实装 @Cacheable(value="mat:detail", key="#id", ttl=300)
 * <p>@PreAuthorize 7 Tab 权限隔离：Sprint 7 集成阶段实装（按角色过滤 DTO 字段）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@RestController
@RequestMapping("/materials")
@Tag(name = "V1.3.8-Story2.1-料号详情")
public class MaterialDetailController {

    private final MaterialDetailService service;
    private final CrmMaterialMapper materialMapper;
    private final CrmDrawingMapper drawingMapper;
    private final MaterialMasterEnsureService materialMasterEnsureService;

    @Autowired
    public MaterialDetailController(MaterialDetailService service,
                                    CrmMaterialMapper materialMapper,
                                    CrmDrawingMapper drawingMapper,
                                    MaterialMasterEnsureService materialMasterEnsureService) {
        this.service = service;
        this.materialMapper = materialMapper;
        this.drawingMapper = drawingMapper;
        this.materialMasterEnsureService = materialMasterEnsureService;
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "按物料编码查询料号 ID（供列表跳转详情）")
    public Result<Map<String, Object>> lookup(@RequestParam("code") String code) {
        if (code == null || code.isBlank()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "物料编码必填");
        }
        String trimmed = code.trim();
        CrmMaterial material = materialMapper.selectByMaterialCode(trimmed);
        if (material == null) {
            CrmDrawing drawing = drawingMapper.selectByMaterialCode(trimmed);
            if (drawing != null) {
                material = materialMasterEnsureService.ensureFromDrawing(drawing.getMaterialCode(), drawing.getTitle());
            }
        }
        if (material == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "物料不存在：" + trimmed);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", material.getId());
        data.put("materialCode", material.getMaterialCode());
        data.put("materialName", material.getMaterialName());
        data.put("spec", material.getSpec());
        return Result.ok(data);
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "料号详情聚合（Story 2.1 AC-2.1.1 · 7 Tab）")
    public Result<MaterialDetailDTO> detail(@PathVariable("id") Long id) {
        return service.getMaterialDetail(id);
    }

    @GetMapping("/{id}/price-history")
    @PreAuthorize("hasAnyRole('PURCHASER', 'PURCHASER_LEAD', 'GM', 'ADMIN')")
    @Operation(summary = "价格走势（Story 2.1 AC-2.1.2）")
    public Result<List<MaterialDetailDTO.PriceInfo.TrendPoint>> priceHistory(@PathVariable("id") Long id) {
        return service.getPriceHistory(id);
    }

    @GetMapping("/{id}/process-route")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'WAREHOUSE_LEAD', 'GM', 'ADMIN')")
    @Operation(summary = "工艺路线（Story 2.1 AC-2.1.2）")
    public Result<List<MaterialDetailDTO.ProcessInfo.ProcessRoute>> processRoute(@PathVariable("id") Long id) {
        return service.getProcessRoute(id);
    }

    @GetMapping("/{id}/change-log")
    @PreAuthorize("hasAnyRole('GM', 'ADMIN')")
    @Operation(summary = "历史变更（Story 2.1 AC-2.1.2 · sys_change_log）")
    public Result<List<ChangeLogEntry>> changeLog(
            @PathVariable("id") Long id,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        return service.getChangeLog(id, limit);
    }
}