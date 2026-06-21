package com.btsheng.erp.business.crm.qualityinspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionTemplateItemDto;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionTemplateResponse;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionTemplateSaveRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionTemplate;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionTemplateItem;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionTemplateItemMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionTemplateMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * V1.3.9 · 检验方案模板 · DRAFT/ACTIVE/ARCHIVED
 */
@Service
public class QualityInspectionTemplateService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    private final CrmQualityInspectionTemplateMapper templateMapper;
    private final CrmQualityInspectionTemplateItemMapper itemMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public QualityInspectionTemplateService(CrmQualityInspectionTemplateMapper templateMapper,
                                              CrmQualityInspectionTemplateItemMapper itemMapper,
                                              DocNoGenerator docNoGenerator) {
        this.templateMapper = templateMapper;
        this.itemMapper = itemMapper;
        this.docNoGenerator = docNoGenerator;
    }

    public Result<Map<String, Object>> list(String status, String drawingNo, String inspectionType,
                                            String materialCode) {
        List<CrmQualityInspectionTemplate> rows;
        if (drawingNo != null && !drawingNo.isBlank()) {
            rows = templateMapper.selectActiveForType(normalizeInspectType(inspectionType));
            rows = rows.stream()
                    .filter(t -> matchesDrawing(t.getDrawingNoPattern(), drawingNo))
                    .filter(t -> matchesMaterial(t.getMaterialCode(), materialCode))
                    .sorted(Comparator.comparingInt(this::specificityScore).reversed())
                    .collect(Collectors.toList());
        } else if (status != null && !status.isBlank()) {
            rows = templateMapper.selectByStatus(status.trim().toUpperCase());
        } else {
            rows = templateMapper.selectList(new LambdaQueryWrapper<CrmQualityInspectionTemplate>()
                    .orderByDesc(CrmQualityInspectionTemplate::getUpdatedAt));
        }

        List<InspectionTemplateResponse> items = rows.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        if (drawingNo != null && !drawingNo.isBlank()) {
            for (InspectionTemplateResponse resp : items) {
                resp.setItems(loadItemDtos(resp.getId()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", items.size());
        return Result.ok(data);
    }

    public Result<InspectionTemplateResponse> getById(Long id) {
        CrmQualityInspectionTemplate t = templateMapper.selectById(id);
        if (t == null) {
            return Result.fail(404, "模板不存在");
        }
        return Result.ok(toDetail(t));
    }

    @Transactional
    public Result<InspectionTemplateResponse> create(InspectionTemplateSaveRequest req, Long userId, String userRoles) {
        if (!QualityTemplateRoleGuard.canEdit(userRoles)) {
            return Result.fail(403, "无权限创建检验模板（需工程师或品质主管）");
        }
        String err = validateSave(req);
        if (err != null) {
            return Result.fail(400, err);
        }

        CrmQualityInspectionTemplate t = new CrmQualityInspectionTemplate();
        t.setTemplateNo(docNoGenerator.nextQualityTemplateNo());
        applySave(t, req);
        t.setStatus(STATUS_DRAFT);
        t.setVersion(1);
        t.setCreatedBy(userId);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(t);
        saveItems(t.getId(), req.getItems());

        return Result.ok(toDetail(t));
    }

    @Transactional
    public Result<InspectionTemplateResponse> update(Long id, InspectionTemplateSaveRequest req,
                                                       Long userId, String userRoles) {
        if (!QualityTemplateRoleGuard.canEdit(userRoles)) {
            return Result.fail(403, "无权限编辑检验模板");
        }
        CrmQualityInspectionTemplate t = templateMapper.selectById(id);
        if (t == null) {
            return Result.fail(404, "模板不存在");
        }
        if (!STATUS_DRAFT.equals(t.getStatus())) {
            return Result.fail(400, "仅草稿状态可编辑");
        }
        String err = validateSave(req);
        if (err != null) {
            return Result.fail(400, err);
        }

        applySave(t, req);
        t.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(t);
        itemMapper.deleteByTemplateId(id);
        saveItems(id, req.getItems());

        return Result.ok(toDetail(t));
    }

    @Transactional
    public Result<Void> delete(Long id, String userRoles) {
        if (!QualityTemplateRoleGuard.canEdit(userRoles)) {
            return Result.fail(403, "无权限删除检验模板");
        }
        CrmQualityInspectionTemplate t = templateMapper.selectById(id);
        if (t == null) {
            return Result.fail(404, "模板不存在");
        }
        if (!STATUS_DRAFT.equals(t.getStatus())) {
            return Result.fail(400, "仅草稿状态可删除");
        }
        itemMapper.deleteByTemplateId(id);
        templateMapper.deleteById(id);
        return Result.ok(null);
    }

    @Transactional
    public Result<InspectionTemplateResponse> publish(Long id, Long userId, String userRoles) {
        if (!QualityTemplateRoleGuard.canPublish(userRoles)) {
            return Result.fail(403, "无权限发布检验模板（需品质主管）");
        }
        CrmQualityInspectionTemplate t = templateMapper.selectById(id);
        if (t == null) {
            return Result.fail(404, "模板不存在");
        }
        if (!STATUS_DRAFT.equals(t.getStatus())) {
            return Result.fail(400, "仅草稿状态可发布");
        }
        List<CrmQualityInspectionTemplateItem> items = itemMapper.selectByTemplateId(id);
        if (items.isEmpty()) {
            return Result.fail(400, "至少包含 1 个检验项才可发布");
        }

        archiveConflictingActive(t, userId);

        t.setStatus(STATUS_ACTIVE);
        t.setPublishedBy(userId);
        t.setPublishedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(t);

        return Result.ok(toDetail(t));
    }

    @Transactional
    public Result<InspectionTemplateResponse> archive(Long id, Long userId, String userRoles) {
        if (!QualityTemplateRoleGuard.canPublish(userRoles)) {
            return Result.fail(403, "无权限停用检验模板（需品质主管）");
        }
        CrmQualityInspectionTemplate t = templateMapper.selectById(id);
        if (t == null) {
            return Result.fail(404, "模板不存在");
        }
        if (!STATUS_ACTIVE.equals(t.getStatus())) {
            return Result.fail(400, "仅生效中模板可停用");
        }
        t.setStatus(STATUS_ARCHIVED);
        t.setArchivedBy(userId);
        t.setArchivedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(t);
        return Result.ok(toDetail(t));
    }

    private void archiveConflictingActive(CrmQualityInspectionTemplate draft, Long userId) {
        List<CrmQualityInspectionTemplate> activeList = templateMapper.selectByStatus(STATUS_ACTIVE);
        String key = scopeKey(draft);
        LocalDateTime now = LocalDateTime.now();
        for (CrmQualityInspectionTemplate other : activeList) {
            if (Objects.equals(other.getId(), draft.getId())) {
                continue;
            }
            if (scopeKey(other).equals(key)) {
                other.setStatus(STATUS_ARCHIVED);
                other.setArchivedBy(userId);
                other.setArchivedAt(now);
                other.setUpdatedAt(now);
                templateMapper.updateById(other);
            }
        }
    }

    private String scopeKey(CrmQualityInspectionTemplate t) {
        return normalizeBlank(t.getDrawingNoPattern()) + "|"
                + normalizeBlank(t.getMaterialCode()) + "|"
                + normalizeBlank(t.getInspectionType());
    }

    private String normalizeBlank(String s) {
        return s == null ? "" : s.trim();
    }

    private String normalizeInspectType(String inspectionType) {
        if (inspectionType == null || inspectionType.isBlank()) {
            return "";
        }
        return switch (inspectionType.toUpperCase()) {
            case "INCOMING" -> "IQC";
            case "IN_PROCESS" -> "IPQC";
            case "OUTGOING" -> "OQC";
            default -> inspectionType.toUpperCase();
        };
    }

    static boolean matchesDrawing(String pattern, String drawingNo) {
        if (pattern == null || pattern.isBlank()) {
            return true;
        }
        if (drawingNo == null || drawingNo.isBlank()) {
            return false;
        }
        String p = pattern.trim();
        if (p.contains("%")) {
            String prefix = p.replace("%", "");
            return drawingNo.startsWith(prefix) || drawingNo.contains(prefix);
        }
        return drawingNo.contains(p) || drawingNo.equalsIgnoreCase(p);
    }

    static boolean matchesMaterial(String templateMaterial, String materialCode) {
        if (templateMaterial == null || templateMaterial.isBlank()) {
            return true;
        }
        if (materialCode == null || materialCode.isBlank()) {
            return false;
        }
        return templateMaterial.equalsIgnoreCase(materialCode.trim());
    }

    private int specificityScore(CrmQualityInspectionTemplate t) {
        int score = 0;
        if (t.getMaterialCode() != null && !t.getMaterialCode().isBlank()) {
            score += 2;
        }
        if (t.getDrawingNoPattern() != null && !t.getDrawingNoPattern().isBlank()) {
            score += 1;
        }
        return score;
    }

    private String validateSave(InspectionTemplateSaveRequest req) {
        if (req.getTemplateName() == null || req.getTemplateName().isBlank()) {
            return "模板名称必填";
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            return "至少添加 1 个检验项";
        }
        for (InspectionTemplateItemDto item : req.getItems()) {
            if (item.getItemName() == null || item.getItemName().isBlank()) {
                return "检验项名称不能为空";
            }
        }
        return null;
    }

    private void applySave(CrmQualityInspectionTemplate t, InspectionTemplateSaveRequest req) {
        t.setTemplateName(req.getTemplateName().trim());
        t.setDrawingNoPattern(blankToNull(req.getDrawingNoPattern()));
        t.setMaterialCode(blankToNull(req.getMaterialCode()));
        t.setInspectionType(normalizeInspectTypeOrNull(req.getInspectionType()));
        t.setSampleRatio(req.getSampleRatio());
        t.setRemark(blankToNull(req.getRemark()));
    }

    private String normalizeInspectTypeOrNull(String inspectionType) {
        if (inspectionType == null || inspectionType.isBlank()) {
            return null;
        }
        return normalizeInspectType(inspectionType);
    }

    private String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private void saveItems(Long templateId, List<InspectionTemplateItemDto> items) {
        int order = 0;
        for (InspectionTemplateItemDto dto : items) {
            CrmQualityInspectionTemplateItem item = new CrmQualityInspectionTemplateItem();
            item.setTemplateId(templateId);
            item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order);
            item.setItemName(dto.getItemName().trim());
            item.setStandard(blankToNull(dto.getStandard()));
            item.setToleranceUpper(blankToNull(dto.getToleranceUpper()));
            item.setToleranceLower(blankToNull(dto.getToleranceLower()));
            item.setSeverity(dto.getSeverity() != null && !dto.getSeverity().isBlank()
                    ? dto.getSeverity().trim().toUpperCase() : "INFO");
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
            order++;
        }
    }

    private InspectionTemplateResponse toSummary(CrmQualityInspectionTemplate t) {
        InspectionTemplateResponse resp = mapBase(t);
        List<CrmQualityInspectionTemplateItem> items = itemMapper.selectByTemplateId(t.getId());
        resp.setItemCount(items.size());
        return resp;
    }

    private InspectionTemplateResponse toDetail(CrmQualityInspectionTemplate t) {
        InspectionTemplateResponse resp = mapBase(t);
        List<InspectionTemplateItemDto> items = loadItemDtos(t.getId());
        resp.setItems(items);
        resp.setItemCount(items.size());
        return resp;
    }

    private List<InspectionTemplateItemDto> loadItemDtos(Long templateId) {
        return itemMapper.selectByTemplateId(templateId).stream()
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    private InspectionTemplateResponse mapBase(CrmQualityInspectionTemplate t) {
        InspectionTemplateResponse resp = new InspectionTemplateResponse();
        resp.setId(t.getId());
        resp.setTemplateNo(t.getTemplateNo());
        resp.setTemplateName(t.getTemplateName());
        resp.setDrawingNoPattern(t.getDrawingNoPattern());
        resp.setMaterialCode(t.getMaterialCode());
        resp.setInspectionType(t.getInspectionType());
        resp.setSampleRatio(t.getSampleRatio());
        resp.setStatus(t.getStatus());
        resp.setVersion(t.getVersion());
        resp.setRemark(t.getRemark());
        resp.setPublishedBy(t.getPublishedBy());
        resp.setPublishedAt(t.getPublishedAt());
        resp.setArchivedBy(t.getArchivedBy());
        resp.setArchivedAt(t.getArchivedAt());
        resp.setCreatedBy(t.getCreatedBy());
        resp.setCreatedAt(t.getCreatedAt());
        resp.setUpdatedAt(t.getUpdatedAt());
        return resp;
    }

    private InspectionTemplateItemDto mapItem(CrmQualityInspectionTemplateItem item) {
        InspectionTemplateItemDto dto = new InspectionTemplateItemDto();
        dto.setId(item.getId());
        dto.setSortOrder(item.getSortOrder());
        dto.setItemName(item.getItemName());
        dto.setStandard(item.getStandard());
        dto.setToleranceUpper(item.getToleranceUpper());
        dto.setToleranceLower(item.getToleranceLower());
        dto.setSeverity(item.getSeverity());
        return dto;
    }
}
