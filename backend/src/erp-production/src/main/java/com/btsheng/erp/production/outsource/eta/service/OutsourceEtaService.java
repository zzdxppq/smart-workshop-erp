package com.btsheng.erp.production.outsource.eta.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceActual;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceEta;
import com.btsheng.erp.production.outsource.eta.mapper.CrmOutsourceActualMapper;
import com.btsheng.erp.production.outsource.eta.mapper.CrmOutsourceEtaMapper;
import com.btsheng.erp.production.outsource.eta.dto.PredictEtaRequest;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.24 · 委外历史交期预估 Service (FR-6-4)
 *
 * <p>3 业务方法：predictEta / updateActualEta / getEtaHistory
 * <p>预估单号：OE{yyyyMMdd}{seq:4}
 * <p>5 状态：PREDICTED/IN_PROGRESS/COMPLETED/OVERDUE/ALERTED
 * <p>3 P1 修补：偏差超 20% 自动告警 / 预估准确�?�?80% / 预估必填
 */
@Service
public class OutsourceEtaService {

    public static final String STATUS_PREDICTED = "PREDICTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_OVERDUE = "OVERDUE";
    public static final String STATUS_ALERTED = "ALERTED";

    /** P1 修补 1：偏差超 20% 自动告警 */
    public static final BigDecimal DEVIATION_ALERT_PCT = new BigDecimal("20.00");

    /** P1 修补 2：预估准确率 �?80% */
    public static final BigDecimal ACCURACY_THRESHOLD = new BigDecimal("0.80");

    /** 历史样本不足时的默认预估天数 */
    public static final int DEFAULT_PREDICT_DAYS = 10;

    private final CrmOutsourceEtaMapper etaMapper;
    private final CrmOutsourceActualMapper actualMapper;
    private final CrmOutsourceOrderMapper orderMapper;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public OutsourceEtaService(CrmOutsourceEtaMapper etaMapper,
                                 CrmOutsourceActualMapper actualMapper,
                                 CrmOutsourceOrderMapper orderMapper,
                                 ErpDocNoGenerator docNoGenerator) {
        this.etaMapper = etaMapper;
        this.actualMapper = actualMapper;
        this.orderMapper = orderMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-6.4.1 基于供应商历史交期数据预�?     */
    @Transactional
    @AuditLog(module = "outsource_eta", action = "outsource_eta.predict")
    public Result<CrmOutsourceEta> predictEta(PredictEtaRequest req, Long operatorUserId) {
        if (req == null || req.getOutsourceId() == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        CrmOutsourceOrder order = orderMapper.selectById(req.getOutsourceId());
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }

        Long supplierId = req.getSupplierId() != null ? req.getSupplierId() : order.getSupplierId();
        String processName = req.getProcessName() != null ? req.getProcessName() : order.getProcessName();
        int qty = req.getQty() != null ? req.getQty() : (order.getQty() == null ? 1 : order.getQty());
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();

        // 1. 拉取历史样本（基�?supplier_id + process_name，限�?50 条）
            List<CrmOutsourceActual> samples = actualMapper.selectBySupplierAndProcess(supplierId, processName, 50);

        // 2. 计算预估天数（平均天数，向上取整�?
            int predictedDays = DEFAULT_PREDICT_DAYS;
        BigDecimal confidence = new BigDecimal("0.80");
        if (samples != null && !samples.isEmpty()) {
            long sum = 0;
            for (CrmOutsourceActual s : samples) {
                if (s.getActualDays() != null) sum += s.getActualDays();
            }
            predictedDays = (int) Math.max(1, Math.round((double) sum / samples.size()));
            // 样本越多置信度越高（最�?0.95�?
            double conf = Math.min(0.95, 0.70 + 0.05 * Math.min(samples.size(), 5));
            confidence = BigDecimal.valueOf(conf).setScale(2, RoundingMode.HALF_UP);
        }

        // 3. 写入预估�?
            CrmOutsourceEta eta = new CrmOutsourceEta();
        eta.setEtaNo(docNoGenerator.nextOutsourceEtaNo());
        eta.setOutsourceId(order.getId());
        eta.setOutsourceNo(order.getOutsourceNo());
        eta.setSupplierId(supplierId);
        eta.setSupplierName(order.getSupplierName());
        eta.setProcessName(processName);
        eta.setQty(qty);
        eta.setPredictedDays(predictedDays);
        eta.setPredictedDeliveryDate(start.plusDays(predictedDays));
        eta.setConfidence(confidence);
        eta.setBaseSamples(samples == null ? 0 : samples.size());
        eta.setStatus(STATUS_PREDICTED);
        eta.setCreatedBy(operatorUserId);
        eta.setCreatedAt(LocalDateTime.now());
        eta.setUpdatedAt(LocalDateTime.now());
        etaMapper.insert(eta);

        return Result.ok(eta);
    }

    /**
     * AC-6.4.2 交期偏差�?20% 自动告警
     */
    @Transactional
    @AuditLog(module = "outsource_eta", action = "outsource_eta.update_actual")
    public Result<CrmOutsourceEta> updateActualEta(Long etaId, LocalDate actualDate, Long operatorUserId) {
        if (etaId == null) {
            return Result.fail(40001, "ETA_ID_REQUIRED");
        }
        if (actualDate == null) {
            return Result.fail(40001, "ACTUAL_DELIVERY_DATE_REQUIRED");
        }
        CrmOutsourceEta eta = etaMapper.selectById(etaId);
        if (eta == null) {
            return Result.fail(40404, "ETA_NOT_FOUND");
        }

        // 写实际交期历�?
            int actualDays = (int) (actualDate.toEpochDay() - eta.getCreatedAt().toLocalDate().toEpochDay());
        if (actualDays < 0) actualDays = 0;

        BigDecimal deviationPct = BigDecimal.ZERO;
        if (eta.getPredictedDays() != null && eta.getPredictedDays() > 0) {
            deviationPct = new BigDecimal(actualDays - eta.getPredictedDays())
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(eta.getPredictedDays()), 2, RoundingMode.HALF_UP);
        }

        CrmOutsourceActual actual = new CrmOutsourceActual();
        actual.setEtaId(eta.getId());
        actual.setOutsourceId(eta.getOutsourceId());
        actual.setOutsourceNo(eta.getOutsourceNo());
        actual.setSupplierId(eta.getSupplierId());
        actual.setSupplierName(eta.getSupplierName());
        actual.setProcessName(eta.getProcessName());
        actual.setQty(eta.getQty());
        actual.setPromisedDate(eta.getPredictedDeliveryDate());
        actual.setActualDate(actualDate);
        actual.setActualDays(actualDays);
        actual.setPredictedDays(eta.getPredictedDays());
        actual.setDeviationPct(deviationPct);
        actual.setOnTime(deviationPct.abs().compareTo(DEVIATION_ALERT_PCT) <= 0 ? 1 : 0);
        actual.setCreatedAt(LocalDateTime.now());
        actualMapper.insert(actual);

        // AC-6.4.3 准确率判�?+ 状态更�?
            int accuracyPassed = deviationPct.abs().compareTo(new BigDecimal("20.00")) <= 0 ? 1 : 0;
        String newStatus = accuracyPassed == 1 ? STATUS_COMPLETED : STATUS_ALERTED;
        eta.setActualDeliveryDate(actualDate);
        eta.setDeviationPct(deviationPct);
        eta.setAccuracyPassed(accuracyPassed);
        eta.setStatus(newStatus);
        eta.setUpdatedAt(LocalDateTime.now());
        etaMapper.updateById(eta);

        return Result.ok(eta);
    }

    /**
     * AC-6.4.3 预估历史（同时返回历史准确率统计�?     */
    @AuditLog(module = "outsource_eta", action = "outsource_eta.get_history")
    public Result<Map<String, Object>> getEtaHistory(Long outsourceId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        List<CrmOutsourceEta> etas = etaMapper.selectByOutsourceId(outsourceId);

        // 计算该委外单关联供应商的准确�?
            Map<String, Object> result = new HashMap<>();
        result.put("outsourceId", outsourceId);
        result.put("etas", etas);
        if (etas != null && !etas.isEmpty()) {
            Long supplierId = etas.get(0).getSupplierId();
            Map<String, Object> stats = etaMapper.selectAccuracyStats(supplierId);
            result.put("accuracyStats", stats);
        }
        return Result.ok(result);
    }

    public Result<PageResponse<Map<String, Object>>> listEtas(String keyword, int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.max(pageSize, 1);
        int offset = (page - 1) * size;
        List<CrmOutsourceEta> etas = etaMapper.selectPage(keyword, size, offset);
        long total = etaMapper.countByKeyword(keyword);
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (CrmOutsourceEta eta : etas) {
            Map<String, Object> row = new HashMap<>();
            row.put("outsourceNo", eta.getOutsourceNo());
            row.put("workorderNo", eta.getOutsourceNo());
            row.put("supplierName", eta.getSupplierName());
            row.put("originalEta", eta.getPredictedDeliveryDate());
            row.put("predictedEta", eta.getPredictedDeliveryDate());
            int delayDays = 0;
            if (eta.getActualDeliveryDate() != null && eta.getPredictedDeliveryDate() != null) {
                delayDays = (int) (eta.getActualDeliveryDate().toEpochDay()
                        - eta.getPredictedDeliveryDate().toEpochDay());
            }
            row.put("delayDays", delayDays);
            rows.add(row);
        }
        return Result.ok(PageResponse.of(rows, total, page, size));
    }

    public Result<PageResponse<Map<String, Object>>> listEtaHistoryRecords(String keyword, int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.max(pageSize, 1);
        int offset = (page - 1) * size;
        List<CrmOutsourceEta> etas = etaMapper.selectPage(keyword, size, offset);
        long total = etaMapper.countByKeyword(keyword);
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (CrmOutsourceEta eta : etas) {
            Map<String, Object> row = new HashMap<>();
            row.put("outsourceNo", eta.getOutsourceNo());
            row.put("predictedAt", eta.getCreatedAt());
            row.put("predictedEta", eta.getPredictedDeliveryDate());
            row.put("actualEta", eta.getActualDeliveryDate());
            row.put("algorithm", "HIST_AVG");
            row.put("accuracy", eta.getAccuracyPassed() == null ? null : eta.getAccuracyPassed());
            rows.add(row);
        }
        return Result.ok(PageResponse.of(rows, total, page, size));
    }
}
