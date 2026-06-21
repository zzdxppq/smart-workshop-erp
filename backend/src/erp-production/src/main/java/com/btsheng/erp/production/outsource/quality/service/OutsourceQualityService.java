package com.btsheng.erp.production.outsource.quality.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.quality.dto.AddQualityDefectRequest;
import com.btsheng.erp.production.outsource.quality.dto.QualityCreateRequest;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQuality;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityDefect;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityItem;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityDefectMapper;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityItemMapper;
import com.btsheng.erp.production.outsource.quality.mapper.CrmOutsourceQualityMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.27 · 委外工序质检 Service (FR-6-7)
 *
 * <p>5 业务方法：createQuality / addItem / pass / reject / list
 * <p>质检单号：OQ{yyyyMMdd}{seq:4}
 * <p>4 状态：DRAFT/PASSED/FAILED/CONDITIONAL
 * <p>2 检验类型：FA（首件）/CMM（三次元�? * <p>3 P1 修补：检验项目必�?/ 严重度分�?/ 不良�?> 10% 告警
 */
@Service
public class OutsourceQualityService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CONDITIONAL = "CONDITIONAL";

    public static final String INSPECT_FA = "FA";
    public static final String INSPECT_CMM = "CMM";

    public static final String SEVERITY_MINOR = "MINOR";
    public static final String SEVERITY_MAJOR = "MAJOR";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    /** P1 修补 3：不良率 > 10% 告警 */
    public static final BigDecimal ALERT_DEFECT_RATE = new BigDecimal("10.00");

    private final CrmOutsourceQualityMapper qualityMapper;
    private final CrmOutsourceQualityItemMapper itemMapper;
    private final CrmOutsourceQualityDefectMapper defectMapper;
    private final CrmOutsourceOrderMapper orderMapper;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public OutsourceQualityService(CrmOutsourceQualityMapper qualityMapper,
                                    CrmOutsourceQualityItemMapper itemMapper,
                                    CrmOutsourceQualityDefectMapper defectMapper,
                                    CrmOutsourceOrderMapper orderMapper,
                                    ErpDocNoGenerator docNoGenerator) {
        this.qualityMapper = qualityMapper;
        this.itemMapper = itemMapper;
        this.defectMapper = defectMapper;
        this.orderMapper = orderMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-6.7.1 委外工序独立质检（区别于 7 品质�?     * P1 修补 1：检验项目必�?     */
    @Transactional
    @AuditLog(module = "outsource_quality", action = "outsource_quality.create")
    public Result<CrmOutsourceQuality> createQuality(QualityCreateRequest req, Long operatorUserId) {
        if (req == null || req.getOutsourceId() == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        if (req.getProcessName() == null || req.getProcessName().isEmpty()) {
            return Result.fail(40001, "PROCESS_NAME_REQUIRED");
        }
        if (!INSPECT_FA.equals(req.getInspectType()) && !INSPECT_CMM.equals(req.getInspectType())) {
            return Result.fail(40001, "INSPECT_TYPE_INVALID");
        }
        if (req.getInspectQty() == null || req.getInspectQty() <= 0) {
            return Result.fail(40001, "INSPECT_QTY_REQUIRED");
        }
        // P1 修补 1：检验项目必�?
            if (req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(40001, "QUALITY_ITEMS_REQUIRED");
        }
        for (QualityCreateRequest.QualityItemDto item : req.getItems()) {
            if (item.getItemName() == null || item.getItemName().isEmpty()) {
                return Result.fail(40001, "QUALITY_ITEM_NAME_REQUIRED");
            }
        }

        CrmOutsourceOrder order = orderMapper.selectById(req.getOutsourceId());
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }

        CrmOutsourceQuality quality = new CrmOutsourceQuality();
        quality.setQualityNo(docNoGenerator.nextOutsourceQualityNo());
        quality.setOutsourceId(order.getId());
        quality.setOutsourceNo(order.getOutsourceNo());
        quality.setProcessName(req.getProcessName());
        quality.setSupplierId(order.getSupplierId());
        quality.setSupplierName(order.getSupplierName());
        quality.setInspectType(req.getInspectType());
        quality.setInspectQty(req.getInspectQty());
        quality.setPassedQty(0);
        quality.setFailedQty(0);
        quality.setDefectRate(BigDecimal.ZERO);
        quality.setAlerted(0);
        quality.setResult(STATUS_DRAFT);
        quality.setRemark(req.getRemark());
        quality.setCreatedBy(operatorUserId);
        quality.setCreatedAt(LocalDateTime.now());
        quality.setUpdatedAt(LocalDateTime.now());
        qualityMapper.insert(quality);

        // 写检验项�?
            for (QualityCreateRequest.QualityItemDto itemDto : req.getItems()) {
            CrmOutsourceQualityItem item = new CrmOutsourceQualityItem();
            item.setQualityId(quality.getId());
            item.setItemType(itemDto.getItemType() == null ? INSPECT_FA : itemDto.getItemType());
            item.setItemName(itemDto.getItemName());
            item.setStandard(itemDto.getStandard());
            item.setMeasuredValue(itemDto.getMeasuredValue());
            item.setTolerance(itemDto.getTolerance());
            item.setPassed(itemDto.getPassed() == null ? 0 : itemDto.getPassed());
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
        }

        return Result.ok(quality);
    }

    /**
     * 添加不良项（带严重度分级 + 不良率自动累�?+ > 10% 告警�?     */
    @Transactional
    @AuditLog(module = "outsource_quality", action = "outsource_quality.add_defect")
    public Result<CrmOutsourceQualityDefect> addDefect(AddQualityDefectRequest req, Long operatorUserId) {
        if (req == null || req.getQualityId() == null) {
            return Result.fail(40001, "QUALITY_ID_REQUIRED");
        }
        if (req.getDefectType() == null || req.getDefectType().isEmpty()) {
            return Result.fail(40001, "DEFECT_TYPE_REQUIRED");
        }
        // P1 修补 2：严重度分级
            if (req.getSeverity() == null
                || (!SEVERITY_MINOR.equals(req.getSeverity())
                && !SEVERITY_MAJOR.equals(req.getSeverity())
                && !SEVERITY_CRITICAL.equals(req.getSeverity()))) {
            return Result.fail(40001, "DEFECT_SEVERITY_INVALID");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            return Result.fail(40001, "DEFECT_QTY_REQUIRED");
        }

        CrmOutsourceQuality quality = qualityMapper.selectById(req.getQualityId());
        if (quality == null) {
            return Result.fail(40404, "QUALITY_NOT_FOUND");
        }

        CrmOutsourceQualityDefect defect = new CrmOutsourceQualityDefect();
        defect.setQualityId(req.getQualityId());
        defect.setItemId(req.getItemId());
        defect.setDefectType(req.getDefectType());
        defect.setSeverity(req.getSeverity());
        defect.setQty(req.getQty());
        defect.setDescription(req.getDescription());
        defect.setCreatedAt(LocalDateTime.now());
        defectMapper.insert(defect);

        // 同步累加 failed_qty + 不良�?
            int oldFailed = quality.getFailedQty() == null ? 0 : quality.getFailedQty();
        quality.setFailedQty(oldFailed + req.getQty());
        quality.setUpdatedAt(LocalDateTime.now());
        if (quality.getInspectQty() != null && quality.getInspectQty() > 0) {
            int passedQty = quality.getInspectQty() - quality.getFailedQty();
            if (passedQty < 0) passedQty = 0;
            quality.setPassedQty(passedQty);
            BigDecimal rate = new BigDecimal(quality.getFailedQty())
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(quality.getInspectQty()), 2, RoundingMode.HALF_UP);
            quality.setDefectRate(rate);
            // P1 修补 3：不良率 > 10% 告警
            if (rate.compareTo(ALERT_DEFECT_RATE) > 0) {
                quality.setAlerted(1);
            }
        }
        qualityMapper.updateById(quality);

        return Result.ok(defect);
    }

    /**
     * AC-6.7.2 FA/CMM 通过
     */
    @Transactional
    @AuditLog(module = "outsource_quality", action = "outsource_quality.pass")
    public Result<CrmOutsourceQuality> pass(Long qualityId, Long operatorUserId) {
        if (qualityId == null) {
            return Result.fail(40001, "QUALITY_ID_REQUIRED");
        }
        CrmOutsourceQuality quality = qualityMapper.selectById(qualityId);
        if (quality == null) {
            return Result.fail(40404, "QUALITY_NOT_FOUND");
        }
        if (STATUS_FAILED.equals(quality.getResult())) {
            return Result.fail(40903, "QUALITY_ALREADY_FAILED");
        }
        if (STATUS_PASSED.equals(quality.getResult())) {
            return Result.fail(40903, "QUALITY_ALREADY_PASSED");
        }
        int criticalCount = defectMapper.countCriticalByQualityId(qualityId);
        if (criticalCount > 0) {
            return Result.fail(40903, "QUALITY_HAS_CRITICAL_DEFECT");
        }
        int passedQty = quality.getInspectQty() - (quality.getFailedQty() == null ? 0 : quality.getFailedQty());
        if (passedQty < 0) passedQty = 0;
        quality.setPassedQty(passedQty);
        quality.setResult(STATUS_PASSED);
        quality.setInspectorUserId(operatorUserId);
        quality.setInspectedAt(LocalDateTime.now());
        quality.setUpdatedAt(LocalDateTime.now());
        qualityMapper.updateById(quality);

        return Result.ok(quality);
    }

    /**
     * AC-6.7.3 质检不通过自动返修
     */
    @Transactional
    @AuditLog(module = "outsource_quality", action = "outsource_quality.reject")
    public Result<Map<String, Object>> reject(Long qualityId, String reason, Long operatorUserId) {
        if (qualityId == null) {
            return Result.fail(40001, "QUALITY_ID_REQUIRED");
        }
        CrmOutsourceQuality quality = qualityMapper.selectById(qualityId);
        if (quality == null) {
            return Result.fail(40404, "QUALITY_NOT_FOUND");
        }
        if (STATUS_FAILED.equals(quality.getResult())) {
            return Result.fail(40903, "QUALITY_ALREADY_FAILED");
        }
        quality.setResult(STATUS_FAILED);
        quality.setFailedQty(quality.getInspectQty());
        quality.setPassedQty(0);
        quality.setDefectRate(new BigDecimal("100.00"));
        quality.setAlerted(1);
        quality.setInspectorUserId(operatorUserId);
        quality.setInspectedAt(LocalDateTime.now());
        quality.setUpdatedAt(LocalDateTime.now());
        qualityMapper.updateById(quality);

        // 触发返修（信号给 controller�?
            Map<String, Object> result = new HashMap<>();
        result.put("quality", quality);
        result.put("reworkTriggered", true);
        result.put("reworkReason", reason != null ? reason : "委外工序质检不通过");
        result.put("outsourceId", quality.getOutsourceId());
        result.put("outsourceNo", quality.getOutsourceNo());
        return Result.ok(result);
    }

    /**
     * 分页列表（Web 委外检页）
     */
    public Result<Map<String, Object>> listPaged(String keyword, Long outsourceId, String processName,
                                                 String result, int pageNum, int pageSize) {
        List<CrmOutsourceQuality> list;
        if (outsourceId != null) {
            list = qualityMapper.selectByOutsourceId(outsourceId);
        } else if (processName != null && !processName.isEmpty()) {
            list = qualityMapper.selectByProcess(processName);
        } else if (result != null && !result.isEmpty()) {
            list = qualityMapper.selectByResult(result);
        } else {
            list = qualityMapper.selectList(null);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (CrmOutsourceQuality q : list) {
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.trim();
                boolean hit = (q.getOutsourceNo() != null && q.getOutsourceNo().contains(kw))
                        || (q.getQualityNo() != null && q.getQualityNo().contains(kw))
                        || (q.getSupplierName() != null && q.getSupplierName().contains(kw));
                if (!hit) continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", q.getId());
            row.put("inspectionNo", q.getQualityNo());
            row.put("outsourceNo", q.getOutsourceNo());
            row.put("vendorName", q.getSupplierName());
            row.put("materialCode", q.getProcessName());
            row.put("qty", q.getInspectQty());
            row.put("result", q.getResult());
            items.add(row);
        }
        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, items.size());
        List<Map<String, Object>> slice = from < items.size() ? items.subList(from, to) : List.of();
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", slice);
        pageData.put("records", slice);
        pageData.put("total", items.size());
        pageData.put("pageNum", page);
        pageData.put("pageSize", size);
        return Result.ok(pageData);
    }

    /**
     * 查询列表
     */
    @AuditLog(module = "outsource_quality", action = "outsource_quality.list")
    public Result<List<CrmOutsourceQuality>> list(Long outsourceId, String processName, String result) {
        List<CrmOutsourceQuality> list;
        if (outsourceId != null) {
            list = qualityMapper.selectByOutsourceId(outsourceId);
        } else if (processName != null && !processName.isEmpty()) {
            list = qualityMapper.selectByProcess(processName);
        } else if (result != null && !result.isEmpty()) {
            list = qualityMapper.selectByResult(result);
        } else {
            list = qualityMapper.selectList(null);
        }
        return Result.ok(list);
    }
}
