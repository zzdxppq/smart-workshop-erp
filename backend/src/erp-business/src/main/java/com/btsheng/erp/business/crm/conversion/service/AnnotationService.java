package com.btsheng.erp.business.crm.conversion.service;

import com.btsheng.erp.business.crm.conversion.dto.AnnotationRequest;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingAnnotation;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingAnnotationHistory;
import com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingAnnotationHistoryMapper;
import com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingAnnotationMapper;
import com.btsheng.erp.business.crm.conversion.mapper.CrmEngineerWorkloadMapper;
import com.btsheng.erp.business.crm.conversion.entity.CrmEngineerWorkload;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.1 图纸标注 Service
 *
 * 4 方法：addAnnotation / listAnnotationsByVersion / getAnnotations / getDrawingWithAnnotations
 * 4 标注类型：DIMENSION(尺寸) / TOLERANCE(公差) / PROCESS_REQ(工艺要求) / TECH_NOTE(技术注�?
 * P1 修补：标注必须挂载版本（�?v1→v2 标注丢失�? 标注不可修改（只追加�? * P2 修补：SVG 嵌入 + 工程师工作量统计 hook
 */
@Slf4j
@Service
public class AnnotationService {

    private static final Set<String> VALID_TYPES = Set.of("DIMENSION", "TOLERANCE", "PROCESS_REQ", "TECH_NOTE");
    private static final Set<String> VALID_COLORS = Set.of("RED", "YELLOW", "BLUE", "GREEN");

    private final CrmDrawingAnnotationMapper annotationMapper;
    private final CrmDrawingAnnotationHistoryMapper historyMapper;
    private final CrmDrawingMapper drawingMapper;
    private final CrmEngineerWorkloadMapper workloadMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AnnotationService(CrmDrawingAnnotationMapper annotationMapper,
                              CrmDrawingAnnotationHistoryMapper historyMapper,
                              CrmDrawingMapper drawingMapper,
                              CrmEngineerWorkloadMapper workloadMapper) {
        this.annotationMapper = annotationMapper;
        this.historyMapper = historyMapper;
        this.drawingMapper = drawingMapper;
        this.workloadMapper = workloadMapper;
    }

    /**
     * 新增标注（P1 修补：必须挂载版本）
     */
    @Transactional
    @AuditLog(module = "drawing", action = "drawing.add_annotation")
    public Result<CrmDrawingAnnotation> addAnnotation(Long drawingId, AnnotationRequest req, Long operatorUserId) {
        // 1. 字段校验
            if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        if (req.getVersion() == null || req.getVersion().isEmpty()) {
            return Result.fail(40001, "VERSION_REQUIRED");
        }
        if (req.getType() == null || !VALID_TYPES.contains(req.getType())) {
            return Result.fail(40001, "ANNOTATION_TYPE_INVALID");
        }
        if (req.getContent() == null || req.getContent().trim().isEmpty()) {
            return Result.fail(40001, "ANNOTATION_CONTENT_EMPTY");
        }
        if (req.getColor() != null && !VALID_COLORS.contains(req.getColor())) {
            return Result.fail(40001, "ANNOTATION_COLOR_INVALID");
        }
        if (req.getPriority() != null && (req.getPriority() < 1 || req.getPriority() > 10)) {
            return Result.fail(40001, "ANNOTATION_PRIORITY_OUT_OF_RANGE");
        }

        // 2. 校验图纸存在
            CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) return Result.fail(40404, "DRAWING_NOT_FOUND");

        // 3. P1 修补 1：版本必须匹配（防标注挂错版本）
            if (!drawing.getVersion().equals(req.getVersion())) {
            return Result.fail(40001, "ANNOTATION_VERSION_MISMATCH");
        }

        // 4. 检查位置唯一
            CrmDrawingAnnotation dup = annotationMapper.selectByPosition(
            drawingId, req.getVersion(), req.getType(), req.getX(), req.getY());
        if (dup != null) {
            return Result.fail(40905, "ANNOTATION_POSITION_DUPLICATE");
        }

        // 5. 写入标注
            CrmDrawingAnnotation ann = new CrmDrawingAnnotation();
        ann.setDrawingId(drawingId);
        ann.setDrawingNo(drawing.getDrawingNo());
        ann.setVersion(req.getVersion());
        ann.setType(req.getType());
        ann.setContent(req.getContent());
        ann.setColor(req.getColor() == null ? "RED" : req.getColor());
        ann.setX(req.getX() == null ? java.math.BigDecimal.ZERO : req.getX());
        ann.setY(req.getY() == null ? java.math.BigDecimal.ZERO : req.getY());
        ann.setWidth(req.getWidth() == null ? java.math.BigDecimal.ZERO : req.getWidth());
        ann.setHeight(req.getHeight() == null ? java.math.BigDecimal.ZERO : req.getHeight());
        ann.setPriority(req.getPriority() == null ? 5 : req.getPriority());
        ann.setIsArchived(0);
        ann.setSvgData(req.getSvgData());
        ann.setCreatedBy(operatorUserId);
        ann.setCreatedAt(LocalDateTime.now());
        ann.setUpdatedAt(LocalDateTime.now());
        annotationMapper.insert(ann);

        // 6. 写历史（P1 修补：只追加不留�?+ 工程师工作量统计�?
            try {
            CrmDrawingAnnotationHistory h = new CrmDrawingAnnotationHistory();
            h.setAnnotationId(ann.getId());
            h.setDrawingId(drawingId);
            h.setOperation("CREATE");
            h.setActorUserId(operatorUserId);
            h.setSnapshot(objectMapper.writeValueAsString(ann));
            h.setCreatedAt(LocalDateTime.now());
            historyMapper.insert(h);
        } catch (Exception e) {
            log.warn("写标注历史失�? {}", e.getMessage());
        }

        // 7. P2 修补：工程师工作量统�?hook
            incrementWorkload(operatorUserId, "工程师" + operatorUserId, LocalDate.now(), 1);

        return Result.ok(ann);
    }

    /**
     * 查询某图纸某版本的标注列�?     */
    public Result<List<CrmDrawingAnnotation>> listAnnotationsByVersion(Long drawingId, String version) {
        if (version == null || version.isEmpty()) {
            return Result.fail(40001, "VERSION_REQUIRED");
        }
        List<CrmDrawingAnnotation> list = annotationMapper.selectByDrawingAndVersion(drawingId, version);
        return Result.ok(list);
    }

    /**
     * 查询图纸所有版本标�?     */
    public Result<List<CrmDrawingAnnotation>> listAllAnnotations(Long drawingId) {
        List<CrmDrawingAnnotation> list = annotationMapper.selectByDrawing(drawingId);
        return Result.ok(list);
    }

    /**
     * 查询图纸详情 + 标注列表（联合查询）
     */
    public Result<Map<String, Object>> getDrawingWithAnnotations(Long drawingId, String version) {
        CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) return Result.fail(40404, "DRAWING_NOT_FOUND");
        String v = version == null || version.isEmpty() ? drawing.getVersion() : version;
        List<CrmDrawingAnnotation> annotations = annotationMapper.selectByDrawingAndVersion(drawingId, v);
        Map<String, Object> data = new HashMap<>();
        data.put("drawing", drawing);
        data.put("annotations", annotations);
        data.put("version", v);
        data.put("annotationCount", annotations.size());
        return Result.ok(data);
    }

    /**
     * 工程师工作量统计 hook
     */
    private void incrementWorkload(Long userId, String userName, LocalDate date, int delta) {
        try {
            CrmEngineerWorkload load = workloadMapper.selectByUserAndDate(userId, date);
            if (load == null) {
                load = new CrmEngineerWorkload();
                load.setUserId(userId);
                load.setUserName(userName);
                load.setWorkDate(date);
                load.setAnnotationCount(delta);
                load.setConversionCount(0);
                load.setDrawingCreatedCount(0);
                workloadMapper.insert(load);
            } else {
                load.setAnnotationCount(load.getAnnotationCount() + delta);
                workloadMapper.updateById(load);
            }
        } catch (Exception e) {
            log.warn("工程师工作量统计失败: userId={}, err={}", userId, e.getMessage());
        }
    }
}
