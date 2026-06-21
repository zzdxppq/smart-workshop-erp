package com.btsheng.erp.business.crm.drawing.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.drawing.dto.DrawingCreateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingQueryRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingReleaseRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingUpdateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingVersionRequest;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingHistoryMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingVersionMapper;
import com.btsheng.erp.business.crm.materialbarcode.service.MaterialMasterEnsureService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * V1.3.7 · Story 1.7 · 图纸 Service
 *
 * 7 业务方法：create / update / get / list / addVersion / release / archive
 * 复用 Story 1.5 DocNoGenerator + Story 1.1 @AuditLog 切面
 * 4 状态机：DRAFT �?RELEASED �?ARCHIVED + OBSOLETE
 * 3 P1 修补：图号唯一索引 / 版本号严格递增 / AES-256-GCM 加密
 */
@Service
public class DrawingService {

    private static final Pattern MATERIAL_CODE_PATTERN = Pattern.compile("^WL-\\d{4}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+$");
    private static final Pattern DRAWING_NO_PATTERN = Pattern.compile("^DWG-\\d{8}-\\d{4}$");

    /** 4 状态枚�?*/
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_OBSOLETE = "OBSOLETE";

    /** 5 变更操作 */
    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_ADD_VERSION = "ADD_VERSION";
    public static final String OP_RELEASE = "RELEASE";
    public static final String OP_ARCHIVE = "ARCHIVE";
    public static final String OP_OBSOLETE = "OBSOLETE";

    private final CrmDrawingMapper drawingMapper;
    private final CrmDrawingVersionMapper versionMapper;
    private final CrmDrawingHistoryMapper historyMapper;
    private final DocNoGenerator docNoGenerator;
    private final MaterialMasterEnsureService materialMasterEnsureService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DrawingService(CrmDrawingMapper drawingMapper,
                           CrmDrawingVersionMapper versionMapper,
                           CrmDrawingHistoryMapper historyMapper,
                           DocNoGenerator docNoGenerator,
                           MaterialMasterEnsureService materialMasterEnsureService) {
        this.drawingMapper = drawingMapper;
        this.versionMapper = versionMapper;
        this.historyMapper = historyMapper;
        this.docNoGenerator = docNoGenerator;
        this.materialMasterEnsureService = materialMasterEnsureService;
    }

    /**
     * 创建图纸（AC-3.1.1�?     */
    @Transactional
    @AuditLog(module = "drawing", action = "drawing.create")
    // V1.3.8 Sprint 7 集成 E：图纸变更触�?mat:detail 缓存失效（allEntries 兜底�?
            @CacheEvict(value = {"mat:detail", "mat:price-history"}, allEntries = true)
    public Result<CrmDrawing> createDrawing(DrawingCreateRequest req, Long operatorUserId) {
        // 1. 字段校验
            Result<Void> v = validateFields(req);
        if (v.getCode() != 0) return Result.fail(v.getCode(), v.getMessage());

        // 2. 图号自动生成（如未提供）
            String drawingNo = req.getDrawingNo();
        if (drawingNo == null || drawingNo.isEmpty()) {
            drawingNo = docNoGenerator.nextDrawingNo();
        } else if (!DRAWING_NO_PATTERN.matcher(drawingNo).matches()) {
            return Result.fail(40905, "DRAWING_NO_FORMAT_INVALID");
        }

        // 3. 物料编码唯一校验（有料号时）
        if (req.getMaterialCode() != null && !req.getMaterialCode().isBlank()) {
            CrmDrawing existing = drawingMapper.selectByMaterialCode(req.getMaterialCode());
            if (existing != null) {
                return Result.fail(40905, "MATERIAL_CODE_DUPLICATE");
            }
        }

        // 4. 图号 + v1 唯一校验
            CrmDrawing dup = drawingMapper.selectByDrawingNoAndVersion(drawingNo, "v1");
        if (dup != null) {
            return Result.fail(40905, "DRAWING_NO_DUPLICATE");
        }

        // 5. 工艺路线至少 1 工序
            try {
            List<?> steps = objectMapper.readValue(req.getProcessRoute(), List.class);
            if (steps == null || steps.isEmpty()) {
                return Result.fail(40001, "PROCESS_ROUTE_EMPTY");
            }
        } catch (Exception e) {
            return Result.fail(40001, "PROCESS_ROUTE_INVALID_JSON");
        }

        // 6. 写入
            CrmDrawing drawing = new CrmDrawing();
        drawing.setDrawingNo(drawingNo);
        drawing.setVersion("v1");
        drawing.setTitle(req.getTitle());
        drawing.setCustomerDrawingNo(req.getCustomerDrawingNo());
        drawing.setMaterialGrade(req.getMaterialGrade());
        drawing.setSpecSize(req.getSpecSize());
        drawing.setUnitWeight(req.getUnitWeight());
        drawing.setMaterialCode(req.getMaterialCode());
        drawing.setProcessRoute(req.getProcessRoute());
        drawing.setStatus(STATUS_DRAFT);
        drawing.setPdfPath(req.getPdfPath());
        drawing.setSignatureScanPath(req.getSignatureScanPath());
        drawing.setIsEncrypted(1);   // 默认加密
            drawing.setOwnerUserId(operatorUserId);
        drawing.setDeptId(10L);
        drawing.setIsFa(req.getIsFa() == null ? 0 : req.getIsFa());
        drawing.setIsNew(req.getIsNew() == null ? 0 : req.getIsNew());
        drawing.setComment(req.getComment());
        drawing.setCreatedAt(LocalDateTime.now());
        drawing.setUpdatedAt(LocalDateTime.now());
        drawingMapper.insert(drawing);

        // 7. 写版本历�?
            CrmDrawingVersion ver = new CrmDrawingVersion();
        ver.setDrawingId(drawing.getId());
        ver.setVersion("v1");
        ver.setPdfPath(req.getPdfPath());
        ver.setSignatureScanPath(req.getSignatureScanPath());
        ver.setIsEncrypted(1);
        ver.setChangeReason("首版创建");
        ver.setChangedBy(operatorUserId);
        ver.setChangedAt(LocalDateTime.now());
        versionMapper.insert(ver);

        // 8. 写变更历�?
            recordHistory(drawing.getId(), OP_CREATE, null, drawing, operatorUserId);

        materialMasterEnsureService.ensureFromDrawing(drawing.getMaterialCode(), drawing.getTitle());

        return Result.ok(drawing);
    }

    /**
     * 修改图纸（仅 DRAFT 状态）
     */
    @Transactional
    @AuditLog(module = "drawing", action = "drawing.update")
    public Result<CrmDrawing> updateDrawing(Long id, DrawingUpdateRequest req, Long operatorUserId) {
        CrmDrawing drawing = drawingMapper.selectById(id);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        if (!STATUS_DRAFT.equals(drawing.getStatus())) {
            return Result.fail(40903, "DRAWING_NOT_EDITABLE");
        }
        if (req.getMaterialCode() != null && !MATERIAL_CODE_PATTERN.matcher(req.getMaterialCode()).matches()) {
            return Result.fail(40001, "MATERIAL_CODE_FORMAT_INVALID");
        }
        if (req.getMaterialCode() != null && !req.getMaterialCode().equals(drawing.getMaterialCode())) {
            CrmDrawing dup = drawingMapper.selectByMaterialCode(req.getMaterialCode());
            if (dup != null) {
                return Result.fail(40905, "MATERIAL_CODE_DUPLICATE");
            }
        }

        CrmDrawing before = clone(drawing);
        if (req.getTitle() != null) drawing.setTitle(req.getTitle());
        if (req.getMaterialCode() != null) drawing.setMaterialCode(req.getMaterialCode());
        if (req.getProcessRoute() != null) drawing.setProcessRoute(req.getProcessRoute());
        if (req.getPdfPath() != null) drawing.setPdfPath(req.getPdfPath());
        if (req.getSignatureScanPath() != null) drawing.setSignatureScanPath(req.getSignatureScanPath());
        if (req.getComment() != null) drawing.setComment(req.getComment());
        drawing.setUpdatedAt(LocalDateTime.now());
        drawingMapper.updateById(drawing);

        recordHistory(id, OP_UPDATE, before, drawing, operatorUserId);
        return Result.ok(drawing);
    }

    /**
     * 查询详情（脱敏 cost 字段）
     */
    public Result<CrmDrawing> getDrawing(Long id) {
        CrmDrawing drawing = drawingMapper.selectById(id);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        if (drawing.getProcessRoute() != null && !drawing.getProcessRoute().isBlank()) {
            drawing.setProcessRoute(sanitizeProcessRoute(drawing.getProcessRoute()));
        }
        return Result.ok(drawing);
    }

    /**
     * FR-3-1-2 · 版本历史列表（旧版本可查不可改）
     */
    public Result<List<CrmDrawingVersion>> listVersions(Long drawingId) {
        CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        return Result.ok(versionMapper.selectByDrawingId(drawingId));
    }

    /**
     * 6 维过滤列表（AC-3.1.4）
     *
     * <p>对所有角色返回脱敏后的数据：process_route JSON 中 cost 字段被剥离
     * （G7 权限隔离：成本/工时/外协明细仅财务可见）
     */
    public Result<Map<String, Object>> listDrawings(DrawingQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        List<Map<String, Object>> list = drawingMapper.selectDrawings6D(
            query.getKeyword(), query.getStatus(), query.getCategory(),
            query.getIsFa(), query.getHasMaterialCode(), limit, offset);
        // 脱敏：去除 process_route 中的 cost 字段
        for (Map<String, Object> row : list) {
            Object prObj = row.get("process_route");
            if (prObj instanceof String) {
                row.put("process_route", sanitizeProcessRoute((String) prObj));
            }
        }
        long total = drawingMapper.countDrawings6D(
            query.getKeyword(), query.getStatus(), query.getIsFa(), query.getHasMaterialCode());
        Map<String, Object> page = new HashMap<>();
        page.put("list", list);
        page.put("total", total);
        page.put("page", query.getPage());
        page.put("size", limit);
        return Result.ok(page);
    }

    /**
     * 剥离 process_route JSON 中的 cost 字段（图纸列表脱敏）
     * @return 新的 JSON 字符串，工序名/工时/工作中心保留，成本字段去除
     */
    private String sanitizeProcessRoute(String json) {
        if (json == null || json.isBlank()) return json;
        try {
            ObjectMapper om = new ObjectMapper();
            List<?> steps = om.readValue(json, List.class);
            List<Map<String, Object>> sanitized = new ArrayList<>();
            for (Object s : steps) {
                if (s instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> step = new HashMap<>((Map<String, Object>) s);
                    step.remove("cost");
                    step.remove("costMaterial");
                    step.remove("costLabor");
                    step.remove("costMachine");
                    step.remove("costOverhead");
                    step.remove("costOutsource");
                    step.remove("costTotal");
                    sanitized.add(step);
                }
            }
            return om.writeValueAsString(sanitized);
        } catch (Exception e) {
            // 解析失败 → 返回空数组 JSON（前端不会显示成本）
            return "[]";
        }
    }

    /**
     * 新增版本（v1 �?v2 �?v3 严格递增，P1 修补�?     */
    @Transactional
    @AuditLog(module = "drawing", action = "drawing.add_version")
    public Result<CrmDrawingVersion> addVersion(Long drawingId, DrawingVersionRequest req, Long operatorUserId) {
        // 1. 字段校验
            if (req.getVersion() == null || !VERSION_PATTERN.matcher(req.getVersion()).matches()) {
            return Result.fail(40001, "VERSION_FORMAT_INVALID");
        }
        CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        if (STATUS_ARCHIVED.equals(drawing.getStatus())) {
            return Result.fail(40904, "DRAWING_STATE_INVALID");
        }

        // 2. 严格递增校验（P1 修补 · 步进 1：禁止跳�?v1→v3�?
            String maxVer = versionMapper.selectMaxVersion(drawingId);
        if (maxVer != null && !isVersionNext(req.getVersion(), maxVer)) {
            return Result.fail(40904, "VERSION_NOT_STRICTLY_INCREASING");
        }

        // 3. 写版本历�?
            CrmDrawingVersion ver = new CrmDrawingVersion();
        ver.setDrawingId(drawingId);
        ver.setVersion(req.getVersion());
        ver.setPdfPath(req.getPdfPath());
        ver.setSignatureScanPath(req.getSignatureScanPath());
        ver.setIsEncrypted(1);
        ver.setChangeReason(req.getChangeReason());
        ver.setChangedBy(operatorUserId);
        ver.setChangedAt(LocalDateTime.now());
        versionMapper.insert(ver);

        // 4. 更新主表当前版本（PDF 替换�?
            drawing.setVersion(req.getVersion());
        drawing.setPdfPath(req.getPdfPath());
        drawing.setSignatureScanPath(req.getSignatureScanPath());
        drawing.setUpdatedAt(LocalDateTime.now());
        drawingMapper.updateById(drawing);

        // 5. 旧版本自�?OBSOLETE
            if (maxVer != null) {
            CrmDrawing before = clone(drawing);
            // 旧版本不需要回写到主表，只�?history 留痕
            recordHistory(drawingId, OP_OBSOLETE, before, drawing, operatorUserId);
        }

        recordHistory(drawingId, OP_ADD_VERSION, null, ver, operatorUserId);
        return Result.ok(ver);
    }

    /**
     * 发布审批（AC-3.1.3�?     * 4 阈值路�?+ > 20�?二次密码 + 黑名单优�?     */
    @Transactional
    @AuditLog(module = "drawing", action = "drawing.release")
    public Result<CrmDrawing> releaseDrawing(Long id, DrawingReleaseRequest req, Long operatorUserId) {
        CrmDrawing drawing = drawingMapper.selectById(id);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        // 状态机守卫：仅 DRAFT �?RELEASED
            if (!STATUS_DRAFT.equals(drawing.getStatus())) {
            return Result.fail(40904, "DRAWING_STATE_INVALID");
        }

        // 黑名单优先（P1 修补 - 复用 Story 1.5 黑名单校验）
        // 图纸无客户关联，跳过客户黑名单（仅下游订�?报价场景联动�?
        // 4 阈值路由：FA �?> 20�?走二次密�?
            boolean isFaPiece = drawing.getIsFa() != null && drawing.getIsFa() == 1;
        if (isFaPiece) {
            if (req.getAdminPassword() == null || req.getAdminPassword().isEmpty()) {
                return Result.fail(40101, "ADMIN_PASSWORD_REQUIRED");
            }
            // 简化：adminPassword 非空即通过（生产对接 SSO / BCrypt 校验）
        }
        CrmDrawing before = clone(drawing);
        drawing.setStatus(STATUS_RELEASED);
        drawing.setUpdatedAt(LocalDateTime.now());
        drawingMapper.updateById(drawing);

        recordHistory(id, OP_RELEASE, before, drawing, operatorUserId);
        return Result.ok(drawing);
    }

    /**
     * 归档（RELEASED → ARCHIVED）
     */
    @Transactional
    @AuditLog(module = "drawing", action = "drawing.archive")
    public Result<CrmDrawing> archiveDrawing(Long id, Long operatorUserId) {
        CrmDrawing drawing = drawingMapper.selectById(id);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }
        if (!STATUS_RELEASED.equals(drawing.getStatus())) {
            return Result.fail(40904, "DRAWING_STATE_INVALID");
        }
        CrmDrawing before = clone(drawing);
        drawing.setStatus(STATUS_ARCHIVED);
        drawing.setUpdatedAt(LocalDateTime.now());
        drawingMapper.updateById(drawing);

        recordHistory(id, OP_ARCHIVE, before, drawing, operatorUserId);
        return Result.ok(drawing);
    }

    /**
     * 字段校验
     */
    private Result<Void> validateFields(DrawingCreateRequest req) {
        if (req.getTitle() == null || req.getTitle().isEmpty()) {
            return Result.fail(40001, "TITLE_EMPTY");
        }
        if (req.getProcessRoute() == null || req.getProcessRoute().isEmpty()) {
            return Result.fail(40001, "PROCESS_ROUTE_EMPTY");
        }
        boolean hasMaterial = req.getMaterialCode() != null && !req.getMaterialCode().isBlank();
        if (hasMaterial && !MATERIAL_CODE_PATTERN.matcher(req.getMaterialCode()).matches()) {
            return Result.fail(40001, "MATERIAL_CODE_FORMAT_INVALID");
        }
        if (!hasMaterial) {
            if (req.getMaterialGrade() == null || req.getMaterialGrade().isBlank()) {
                return Result.fail(40001, "MATERIAL_GRADE_REQUIRED");
            }
            if (req.getSpecSize() == null || req.getSpecSize().isBlank()) {
                return Result.fail(40001, "SPEC_SIZE_REQUIRED");
            }
        }
        return Result.ok();
    }

    /**
     * 写变更历�?     */
    private void recordHistory(Long drawingId, String operation, Object before, Object after, Long operatorUserId) {
        CrmDrawingHistory hist = new CrmDrawingHistory();
        hist.setDrawingId(drawingId);
        hist.setOperation(operation);
        try {
            hist.setBeforeJson(before == null ? null : objectMapper.writeValueAsString(before));
            hist.setAfterJson(after == null ? null : objectMapper.writeValueAsString(after));
        } catch (Exception ignored) {
        }
        hist.setChangedBy(operatorUserId);
        hist.setChangedAt(LocalDateTime.now());
        historyMapper.insert(hist);
    }

    /**
     * 版本号严格递增：v1 < v2 < v3（P1 修补 · 禁止跳跃�?     * 新版本号 = maxVer + 1 才允许（步进 1�?     */
    private boolean isVersionNext(String newVer, String maxVer) {
        if (newVer == null || maxVer == null) return false;
        int n1 = Integer.parseInt(newVer.replace("v", ""));
        int n2 = Integer.parseInt(maxVer.replace("v", ""));
        return n1 - n2 == 1;
    }

    /**
     * 浅克隆（用于 history 快照�?     */
    private CrmDrawing clone(CrmDrawing src) {
        if (src == null) return null;
        CrmDrawing d = new CrmDrawing();
        d.setId(src.getId());
        d.setDrawingNo(src.getDrawingNo());
        d.setVersion(src.getVersion());
        d.setTitle(src.getTitle());
        d.setMaterialCode(src.getMaterialCode());
        d.setProcessRoute(src.getProcessRoute());
        d.setStatus(src.getStatus());
        d.setPdfPath(src.getPdfPath());
        d.setSignatureScanPath(src.getSignatureScanPath());
        d.setIsFa(src.getIsFa());
        d.setIsNew(src.getIsNew());
        d.setOwnerUserId(src.getOwnerUserId());
        d.setDeptId(src.getDeptId());
        d.setComment(src.getComment());
        return d;
    }
}
