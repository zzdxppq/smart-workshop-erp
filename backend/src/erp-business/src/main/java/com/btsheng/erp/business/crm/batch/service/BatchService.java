package com.btsheng.erp.business.crm.batch.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.batch.dto.BatchCreateRequest;
import com.btsheng.erp.business.crm.batch.dto.BatchCreateResponse;
import com.btsheng.erp.business.crm.batch.dto.PoStatusResponse;
import com.btsheng.erp.business.crm.batch.entity.CrmBatch;
import com.btsheng.erp.business.crm.batch.entity.CrmBatchShadow;
import com.btsheng.erp.business.crm.batch.mapper.CrmBatchMapper;
import com.btsheng.erp.business.crm.batch.mapper.CrmBatchShadowMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * V1.3.8 · Story 3.1 · 批次 Service（按物料粒度�? *
 * <p>核心方法�? * <ul>
 *   <li>{@link #createBatch} AC-3.1.1 批次创建 + PO 状态机更新 + 双写</li>
 *   <li>{@link #getPoStatus} AC-3.1.2 按物料粒度查�?PO 状�?/li>
 *   <li>{@link #compareShadow} AC-3.1.3 双写对比（cron 调用�?/li>
 * </ul>
 *
 * <p>灰度方案：Sprint 7 IMPL 起即双写（写 crm_batch + crm_batch_shadow）�? *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    /** PO 状态枚�?*/
    public static final String PO_PENDING_SHIP = "PENDING_SHIP";
    public static final String PO_PARTIAL_ARRIVED = "PARTIAL_ARRIVED";
    public static final String PO_ALL_ARRIVED = "ALL_ARRIVED";
    public static final String PO_CANCELLED = "CANCELLED";

    /** 批次号模板（DocNoGenerator BATCH-YYYYMMDD-流水�?*/
    private static final String BATCH_NO_PREFIX = "BATCH-";

    private final CrmBatchMapper batchMapper;
    private final CrmBatchShadowMapper batchShadowMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public BatchService(CrmBatchMapper batchMapper,
                        CrmBatchShadowMapper batchShadowMapper,
                        DocNoGenerator docNoGenerator) {
        this.batchMapper = batchMapper;
        this.batchShadowMapper = batchShadowMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-3.1.1：按物料粒度创建批次
     * <p>每个物料一�?�?生成一�?batch_no �?写入 crm_batch + crm_batch_shadow（双写）
     * <p>PO 状态机：PARTIAL_ARRIVED / ALL_ARRIVED
     */
    @AuditLog(action = "BATCH_CREATE", module = "incoming")
    @Transactional(rollbackFor = Exception.class)
    public Result<BatchCreateResponse> createBatch(BatchCreateRequest req, Long createdBy) {
        if (req == null || req.getPoId() == null || req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "poId �?items 必填");
        }

        BatchCreateResponse resp = new BatchCreateResponse();
        List<BatchCreateResponse.BatchInfo> batchInfos = new ArrayList<>();
        List<String> qualityOrders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (BatchCreateRequest.Item item : req.getItems()) {
            // 1. 生成批次号（DocNoGenerator BATCH-YYYYMMDD-流水�?
            String batchNo = docNoGenerator.nextMaterialBatchNo();

            // 2. �?crm_batch
            CrmBatch batch = new CrmBatch();
            batch.setBatchNo(batchNo);
            batch.setMaterialId(item.getMaterialId());
            batch.setPoId(req.getPoId());
            batch.setPoItemId(item.getPoItemId());
            batch.setQuantity(item.getQuantity());
            batch.setArrivedAt(req.getArrivedAt());
            batch.setQualityStatus("PENDING");
            batch.setCreatedBy(createdBy);
            batch.setCreatedAt(now);
            batchMapper.insert(batch);

            // 3. 双写 crm_batch_shadow（architect review 3.1 §3.1 硬性约束）
            CrmBatchShadow shadow = new CrmBatchShadow();
            shadow.setBatchNo(batchNo);
            shadow.setMaterialId(item.getMaterialId());
            shadow.setPoId(req.getPoId());
            shadow.setPoItemId(item.getPoItemId());
            shadow.setQuantity(item.getQuantity());
            shadow.setArrivedAt(req.getArrivedAt());
            shadow.setQualityStatus("PENDING");
            shadow.setCreatedBy(createdBy);
            shadow.setCreatedAt(now);
            batchShadowMapper.insert(shadow);

            BatchCreateResponse.BatchInfo info = new BatchCreateResponse.BatchInfo();
            info.setBatchNo(batchNo);
            info.setMaterialId(item.getMaterialId());
            info.setQuantity(item.getQuantity());
            batchInfos.add(info);

            // 4. 触发来料检单（按物料，非按 PO�?
            String qualityOrderNo = "LJ-" + batchNo.replace("BATCH-", "");
            qualityOrders.add(qualityOrderNo);
        }

        resp.setBatches(batchInfos);
        resp.setQualityOrders(qualityOrders);

        // 5. PO 状态机更新（聚合判断）
            String poStatusAfter = computePoStatus(req.getPoId());
        resp.setPoStatusAfter(poStatusAfter);

        log.info("[BatchService] createBatch ok: poId={} batches={} poStatus={}",
                req.getPoId(), batchInfos.size(), poStatusAfter);

        return Result.ok(resp);
    }

    /**
     * AC-3.1.2：按物料粒度查询 PO 状�?     */
    public Result<PoStatusResponse> getPoStatus(Long poId) {
        if (poId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "poId 必填");
        }
        PoStatusResponse resp = new PoStatusResponse();
        resp.setPoId(poId);
        resp.setPoStatus(computePoStatus(poId));

        List<Map<String, Object>> progressList = batchMapper.aggregatePoProgress(poId);
        List<PoStatusResponse.ItemStatus> items = new ArrayList<>();
        for (Map<String, Object> row : progressList) {
            PoStatusResponse.ItemStatus s = new PoStatusResponse.ItemStatus();
            Number materialId = (Number) row.get("material_id");
            Number ordered = (Number) row.get("ordered");
            Number arrived = (Number) row.get("arrived");
            Number batchCount = (Number) row.get("batch_count");
            if (materialId == null || ordered == null || arrived == null || batchCount == null) {
                continue;
            }
            s.setMaterialId(materialId.longValue());
            s.setOrdered(ordered.intValue());
            s.setArrived(arrived.intValue());
            s.setBatchCount(batchCount.intValue());
            s.setQualityStatus((String) row.get("quality_status"));
            items.add(s);
        }
        resp.setItems(items);

        return Result.ok(resp);
    }

    /**
     * AC-3.1.3：双写对比（cron �?1h 调用�?     * <p>对比 crm_batch �?crm_batch_shadow（按 batch_no + material_id 分组聚合�?     * <p>不一致率 > 0.1% 返回告警信号
     */
    public ShadowComparison compareShadow(LocalDateTime sinceTime) {
        Map<String, Object> result = batchMapper.compareShadow(sinceTime);
        long total = ((Number) result.getOrDefault("total", 0)).longValue();
        long matched = ((Number) result.getOrDefault("matched", 0)).longValue();
        long mismatched = total - matched;

        double mismatchRate = total == 0 ? 0.0 : (double) mismatched / total;
        boolean alert = mismatchRate > 0.001; // 0.1% 阈�?
            ShadowComparison comp = new ShadowComparison();
        comp.setTotal(total);
        comp.setMatched(matched);
        comp.setMismatched(mismatched);
        comp.setMismatchRate(mismatchRate);
        comp.setAlert(alert);

        if (alert) {
            log.warn("[BatchService] shadow compare ALERT: total={} matched={} mismatched={} rate={}",
                    total, matched, mismatched, mismatchRate);
        }

        return comp;
    }

    /**
     * 计算 PO 状态（聚合判断�?     */
    private String computePoStatus(Long poId) {
        List<Map<String, Object>> progress = batchMapper.aggregatePoProgress(poId);
        if (progress.isEmpty()) {
            return PO_PENDING_SHIP;
        }
        boolean allArrived = true;
        boolean anyArrived = false;
        for (Map<String, Object> row : progress) {
            Number orderedNum = (Number) row.get("ordered");
            Number arrivedNum = (Number) row.get("arrived");
            if (orderedNum == null || arrivedNum == null) {
                continue;
            }
            int ordered = orderedNum.intValue();
            int arrived = arrivedNum.intValue();
            if (arrived >= ordered) {
                anyArrived = true;
            } else {
                allArrived = false;
                if (arrived > 0) {
                    anyArrived = true;
                }
            }
        }
        if (allArrived) return PO_ALL_ARRIVED;
        if (anyArrived) return PO_PARTIAL_ARRIVED;
        return PO_PENDING_SHIP;
    }

    /** 影子表对比结�?*/
    @lombok.Data
    public static class ShadowComparison {
        private long total;
        private long matched;
        private long mismatched;
        private double mismatchRate;
        private boolean alert;
    }
}