package com.btsheng.erp.business.crm.qualityinspection.service;

import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionFinalizeRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionCreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389CreateRequest;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389Response;
import com.btsheng.erp.business.crm.qualityinspection.dto.InspectionV1389SubmitRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V1.3.9 Sprint 13.1 · OpenAPI /quality/inspections 适配层
 */
@Service
public class QualityInspectionV1389Service {

    private final QualityInspectionService inspectionService;
    private final CrmMaterialMapper materialMapper;

    @Autowired
    public QualityInspectionV1389Service(QualityInspectionService inspectionService,
                                           CrmMaterialMapper materialMapper) {
        this.inspectionService = inspectionService;
        this.materialMapper = materialMapper;
    }

    public Result<InspectionV1389Response> create(InspectionV1389CreateRequest req, Long userId) {
        String overall = resolveOverallResult(req);
        Long inspectionId = req.getInspectionId();

        if (inspectionId != null) {
            InspectionFinalizeRequest fin = buildFinalizeRequest(req, overall);
            Result<Map<String, Object>> finalized = inspectionService.finalizeInspection(inspectionId, fin, userId);
            if (finalized.getCode() != 0) {
                return Result.fail(finalized.getCode(), finalized.getMessage());
            }
            return Result.ok(buildResponse(inspectionId, finalized.getData()));
        }

        Result<InspectionCreateRequest> mapped = mapCreateRequest(req);
        if (mapped.getCode() != 0) {
            return Result.fail(mapped.getCode(), mapped.getMessage());
        }
        Result<CrmQualityInspection> created = inspectionService.createInspection(mapped.getData(), userId);
        if (created.getCode() != 0) {
            return Result.fail(created.getCode(), created.getMessage());
        }
        CrmQualityInspection q = created.getData();
        if (req.getDrawingNo() != null && !req.getDrawingNo().isBlank()) {
            q.setDrawingNo(req.getDrawingNo().trim());
            // drawingNo persisted via finalize or direct - use finalize path
        }

        if (overall == null) {
            InspectionV1389Response resp = new InspectionV1389Response();
            resp.setInspectionId(q.getId());
            resp.setInspectionNo(q.getInspectionNo());
            resp.setStatus(QualityInspectionService.STATUS_DRAFT);
            resp.setStatusLabel(QualityInspectionService.statusLabel(QualityInspectionService.STATUS_DRAFT));
            if (q.getCreatedAt() != null) {
                resp.setCreatedAt(q.getCreatedAt().toString());
            }
            return Result.ok(resp);
        }

        InspectionFinalizeRequest fin = buildFinalizeRequest(req, overall);
        fin.setDrawingNo(req.getDrawingNo());
        Result<Map<String, Object>> finalized = inspectionService.finalizeInspection(q.getId(), fin, userId);
        if (finalized.getCode() != 0) {
            return Result.fail(finalized.getCode(), finalized.getMessage());
        }
        return Result.ok(buildResponse(q.getId(), finalized.getData()));
    }

    public Result<Map<String, Object>> list(String type, String keyword, String status, String source,
                                            int pageNum, int pageSize) {
        String inspectType = mapTabType(type);
        Result<List<CrmQualityInspection>> raw = inspectionService.list(inspectType, null, null, null);
        if (raw.getCode() != 0) {
            return Result.fail(raw.getCode(), raw.getMessage());
        }
        List<CrmQualityInspection> all = raw.getData() != null ? raw.getData() : List.of();
        List<CrmQualityInspection> filtered = all.stream()
                .filter(q -> keyword == null || keyword.isBlank()
                        || (q.getInspectionNo() != null && q.getInspectionNo().contains(keyword))
                        || (q.getMaterialCode() != null && q.getMaterialCode().contains(keyword))
                        || (q.getWorkOrderNo() != null && q.getWorkOrderNo().contains(keyword)))
                .filter(q -> matchesStatusFilter(q, status))
                .filter(q -> matchesSourceFilter(q, source, inspectType))
                .collect(Collectors.toList());

        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, filtered.size());
        List<Map<String, Object>> items = new ArrayList<>();
        if (from < filtered.size()) {
            for (CrmQualityInspection q : filtered.subList(from, to)) {
                items.add(toListRow(q));
            }
        }
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", items);
        pageData.put("records", items);
        pageData.put("total", filtered.size());
        pageData.put("pageNum", page);
        pageData.put("pageSize", size);
        return Result.ok(pageData);
    }

    public Result<Map<String, Object>> getDetail(Long id) {
        return inspectionService.getDetail(id);
    }

    public Result<Map<String, Object>> getReport(Long id) {
        return inspectionService.getReport(id);
    }

    public Result<Map<String, Object>> submit(Long id, InspectionV1389SubmitRequest req, Long userId) {
        Result<Map<String, Object>> base = inspectionService.submitInspection(id, req, userId);
        if (base.getCode() != 0) {
            return base;
        }
        if (base.getData() != null && base.getData().get("status") != null) {
            base.getData().put("statusLabel",
                    QualityInspectionService.statusLabel(String.valueOf(base.getData().get("status"))));
        }
        return base;
    }

    public Result<Map<String, Object>> approveConcession(Long id, String approverRole, String action,
                                                         String comment, Long userId) {
        return inspectionService.approveConcession(id, approverRole, action, comment, userId);
    }

    public Result<List<Map<String, Object>>> getConcessionApprovals(Long id) {
        return inspectionService.getConcessionApprovals(id);
    }

    private boolean matchesSourceFilter(CrmQualityInspection q, String source, String inspectType) {
        if (source == null || source.isBlank()) {
            return true;
        }
        if (!QualityInspectionService.TYPE_IPQC.equals(inspectType)) {
            return true;
        }
        String actual = q.getInspectSource() == null || q.getInspectSource().isBlank()
                ? QualityInspectionService.SOURCE_INTERNAL
                : q.getInspectSource().trim().toUpperCase();
        return actual.equalsIgnoreCase(source.trim());
    }

    private boolean matchesStatusFilter(CrmQualityInspection q, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String r = q.getResult() == null ? QualityInspectionService.STATUS_DRAFT : q.getResult();
        if ("PENDING".equalsIgnoreCase(status)) {
            return QualityInspectionService.STATUS_DRAFT.equals(r);
        }
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) {
            return QualityInspectionService.STATUS_PENDING_APPROVAL.equals(r);
        }
        return r.equalsIgnoreCase(status);
    }

    private InspectionV1389Response buildResponse(Long inspectionId, Map<String, Object> finalized) {
        InspectionV1389Response resp = new InspectionV1389Response();
        resp.setInspectionId(inspectionId);
        if (finalized != null) {
            resp.setInspectionNo(finalized.get("inspectionNo") != null
                    ? String.valueOf(finalized.get("inspectionNo")) : null);
            String st = finalized.get("status") != null ? String.valueOf(finalized.get("status")) : null;
            resp.setStatus(st);
            resp.setStatusLabel(finalized.get("statusLabel") != null
                    ? String.valueOf(finalized.get("statusLabel"))
                    : QualityInspectionService.statusLabel(st));
            if (finalized.get("downstreamOrderNo") != null) {
                resp.setDownstreamOrderNo(String.valueOf(finalized.get("downstreamOrderNo")));
            }
        }
        return resp;
    }

    private InspectionFinalizeRequest buildFinalizeRequest(InspectionV1389CreateRequest req, String overall) {
        InspectionFinalizeRequest fin = new InspectionFinalizeRequest();
        fin.setOverallResult(overall);
        fin.setDisposition(req.getDisposition());
        fin.setDefectQty(req.getDefectQty());
        fin.setConditionalReason(req.getConditionalReason());
        fin.setDrawingNo(req.getDrawingNo());
        fin.setRemark(req.getRemark());
        fin.setInspectItems(req.getInspectItems());
        return fin;
    }

    private String resolveOverallResult(InspectionV1389CreateRequest req) {
        if (req.getOverallResult() != null && !req.getOverallResult().isBlank()) {
            return req.getOverallResult().trim().toUpperCase();
        }
        if ("CONDITIONAL".equalsIgnoreCase(req.getQualityStatus())) {
            return "CONDITIONAL";
        }
        if ("FAIL".equalsIgnoreCase(req.getQualityStatus())) {
            return "FAIL";
        }
        if ("PASS".equalsIgnoreCase(req.getQualityStatus())) {
            return "PASS";
        }
        return "PASS";
    }

    private Result<InspectionCreateRequest> mapCreateRequest(InspectionV1389CreateRequest req) {
        if (req == null || req.getMaterialCode() == null || req.getMaterialCode().isBlank()) {
            return Result.fail(40001, "materialCode 必填");
        }
        if (req.getInspectItems() == null || req.getInspectItems().isEmpty()) {
            return Result.fail(40001, "inspectItems 至少 1 项");
        }
        String inspectType = mapInspectionType(req.getInspectionType());
        if (inspectType == null) {
            return Result.fail(40001, "inspectionType 无效");
        }

        CrmMaterial material = materialMapper.selectByMaterialCode(req.getMaterialCode().trim());
        if (material == null && QualityInspectionService.TYPE_IQC.equals(inspectType)) {
            return Result.fail(40404, "MATERIAL_NOT_FOUND");
        }

        InspectionCreateRequest legacy = new InspectionCreateRequest();
        legacy.setInspectType(inspectType);
        if (material != null) {
            legacy.setMaterialId(material.getId());
            legacy.setMaterialCode(material.getMaterialCode());
            legacy.setMaterialName(material.getMaterialName());
        } else {
            legacy.setMaterialCode(req.getMaterialCode().trim());
            legacy.setMaterialName(req.getMaterialCode().trim());
        }
        legacy.setWorkOrderNo(req.getWorkOrderNo());
        legacy.setProcessName(req.getProcessName());
        legacy.setRemark(req.getRemark());
        legacy.setLotSize(req.getDefectQty() != null && req.getDefectQty() > 0 ? req.getDefectQty() : 1);
        legacy.setSampleSize(req.getInspectItems().size());

        List<InspectionCreateRequest.InspectionItemDto> items = new ArrayList<>();
        for (InspectionV1389CreateRequest.InspectionV1389ItemDto src : req.getInspectItems()) {
            if (src.getItemName() == null || src.getItemName().isBlank()) {
                return Result.fail(40001, "itemName 必填");
            }
            InspectionCreateRequest.InspectionItemDto item = new InspectionCreateRequest.InspectionItemDto();
            item.setItemName(src.getItemName());
            item.setStandard(src.getStandard());
            item.setMeasuredValue(src.getMeasuredValue());
            item.setSeverity("INFO");
            item.setPassed("OK".equalsIgnoreCase(src.getResult()) ? 1 : 0);
            items.add(item);
        }
        legacy.setItems(items);
        return Result.ok(legacy);
    }

    private String mapInspectionType(String inspectionType) {
        if (inspectionType == null) return null;
        return switch (inspectionType) {
            case "INCOMING", "IQC" -> QualityInspectionService.TYPE_IQC;
            case "IN_PROCESS", "IPQC" -> QualityInspectionService.TYPE_IPQC;
            case "OUTGOING", "OQC" -> QualityInspectionService.TYPE_OQC;
            case "FA", "CMM" -> QualityInspectionService.TYPE_IPQC;
            default -> null;
        };
    }

    private String mapTabType(String type) {
        if (type == null || type.isBlank()) return null;
        return switch (type) {
            case "IQC", "INCOMING" -> QualityInspectionService.TYPE_IQC;
            case "IPQC", "IN_PROCESS" -> QualityInspectionService.TYPE_IPQC;
            case "OQC", "OUTGOING" -> QualityInspectionService.TYPE_OQC;
            default -> null;
        };
    }

    private Map<String, Object> toListRow(CrmQualityInspection q) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", q.getId());
        row.put("inspectionNo", q.getInspectionNo());
        row.put("type", q.getInspectType());
        row.put("materialCode", q.getMaterialCode());
        row.put("workOrderNo", q.getWorkOrderNo());
        row.put("processName", q.getProcessName());
        row.put("inspectSource", resolveListSource(q));
        row.put("sourceLabel", sourceLabel(q.getInspectSource()));
        row.put("qty", q.getInspectQty());
        row.put("passQty", q.getPassedQty());
        row.put("failQty", q.getFailedQty());
        row.put("inspector", q.getInspectorUserId());
        row.put("inspectedAt", q.getInspectedAt());
        row.put("result", q.getResult());
        row.put("inspectionStatus", mapListStatus(q.getResult()));
        row.put("statusLabel", QualityInspectionService.statusLabel(q.getResult()));
        row.put("disposition", q.getDisposition());
        return row;
    }

    private static String mapListStatus(String result) {
        if (result == null || QualityInspectionService.STATUS_DRAFT.equals(result)) {
            return "PENDING";
        }
        return result;
    }

    private static String resolveListSource(CrmQualityInspection q) {
        if (q.getInspectSource() == null || q.getInspectSource().isBlank()) {
            return QualityInspectionService.SOURCE_INTERNAL;
        }
        return q.getInspectSource().trim().toUpperCase();
    }

    static String sourceLabel(String inspectSource) {
        if (QualityInspectionService.SOURCE_OUTSOURCE.equalsIgnoreCase(inspectSource)) {
            return "委外";
        }
        return "厂内";
    }
}
