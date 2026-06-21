package com.btsheng.erp.production.mrp.service;

import com.btsheng.erp.production.integration.client.BusinessMrpPurchaseClient;
import com.btsheng.erp.production.mrp.dto.MrpRunRequest;
import com.btsheng.erp.production.mrp.dto.MrpRunResponse;
import com.btsheng.erp.production.mrp.entity.CrmMrpResult;
import com.btsheng.erp.production.mrp.entity.CrmMrpRun;
import com.btsheng.erp.production.mrp.entity.CrmMrpShortage;
import com.btsheng.erp.production.mrp.mapper.CrmMrpResultMapper;
import com.btsheng.erp.production.mrp.mapper.CrmMrpRunMapper;
import com.btsheng.erp.production.mrp.mapper.CrmMrpShortageMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MrpService {

    private static final Logger log = LoggerFactory.getLogger(MrpService.class);

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String RUN_TYPE_FULL = "FULL";
    public static final String RUN_TYPE_INCREMENTAL = "INCREMENTAL";

    private final CrmMrpRunMapper runMapper;
    private final CrmMrpResultMapper resultMapper;
    private final CrmMrpShortageMapper shortageMapper;
    private final CrmWorkorderMapper workorderMapper;
    private final ErpDocNoGenerator docNoGenerator;
    private final BusinessMrpPurchaseClient mrpPurchaseClient;

    @Autowired
    public MrpService(CrmMrpRunMapper runMapper,
                       CrmMrpResultMapper resultMapper,
                       CrmMrpShortageMapper shortageMapper,
                       CrmWorkorderMapper workorderMapper,
                       ErpDocNoGenerator docNoGenerator,
                       BusinessMrpPurchaseClient mrpPurchaseClient) {
        this.runMapper = runMapper;
        this.resultMapper = resultMapper;
        this.shortageMapper = shortageMapper;
        this.workorderMapper = workorderMapper;
        this.docNoGenerator = docNoGenerator;
        this.mrpPurchaseClient = mrpPurchaseClient;
    }

    @Transactional
    @AuditLog(module = "production", action = "production.run_mrp")
    public Result<MrpRunResponse> runMrp(MrpRunRequest req, Long operatorUserId) {
        if (req.getDateRangeStart() == null || req.getDateRangeEnd() == null) {
            return Result.fail(40001, "DATE_RANGE_REQUIRED");
        }
        if (req.getDateRangeEnd().isBefore(req.getDateRangeStart())) {
            return Result.fail(40001, "DATE_RANGE_INVALID");
        }
        if (req.getWarehouseIds() == null || req.getWarehouseIds().isEmpty()) {
            return Result.fail(40001, "WAREHOUSE_IDS_REQUIRED");
        }

        long t0 = System.currentTimeMillis();
        String runNo = docNoGenerator.nextMrpRunNo();
        CrmMrpRun run = new CrmMrpRun();
        run.setRunNo(runNo);
        run.setRunType(req.getRunType() != null ? req.getRunType() : RUN_TYPE_FULL);
        run.setDateRangeStart(req.getDateRangeStart());
        run.setDateRangeEnd(req.getDateRangeEnd());
        run.setWarehouseIds(req.getWarehouseIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        run.setStatus(STATUS_RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setTriggeredBy(operatorUserId);
        run.setRemark(buildTriggerRemark(req));
        runMapper.insert(run);

        try {
            List<CrmWorkorder> workorders = workorderMapper.selectList(null);
            Map<String, MrpAccumulator> acc = new HashMap<>();
            for (CrmWorkorder wo : workorders) {
                if (wo.getScheduledStart() == null
                    || wo.getScheduledStart().toLocalDate().isBefore(req.getDateRangeStart())
                    || wo.getScheduledStart().toLocalDate().isAfter(req.getDateRangeEnd())) {
                    continue;
                }
                if ("OUTSOURCE".equals(wo.getEquipmentType())) {
                    continue;
                }
                acc.computeIfAbsent(wo.getMaterialCode(), k -> new MrpAccumulator())
                    .addRequired(wo.getQty() != null ? wo.getQty() : 0);
            }

            int totalShortage = 0;
            int totalPurchase = 0;
            List<MrpRunResponse.ShortageItem> shortageItems = new ArrayList<>();
            for (Map.Entry<String, MrpAccumulator> e : acc.entrySet()) {
                MrpAccumulator a = e.getValue();
                int shortage = Math.max(0, a.required - a.currentStock - a.onOrderQty);
                int purchase = shortage;

                CrmMrpResult result = new CrmMrpResult();
                result.setRunId(run.getId());
                result.setMaterialCode(e.getKey());
                result.setMaterialName(e.getKey());
                result.setRequiredQty(a.required);
                result.setCurrentStock(a.currentStock);
                result.setOnOrderQty(a.onOrderQty);
                result.setShortageQty(shortage);
                result.setPurchaseSuggestion(purchase);
                resultMapper.insert(result);

                if (shortage > 0) {
                    CrmMrpShortage s = new CrmMrpShortage();
                    s.setRunId(run.getId());
                    s.setMaterialCode(e.getKey());
                    s.setShortageQty(shortage);
                    s.setRequiredDate(req.getDateRangeStart());
                    s.setPriority(5);
                    shortageMapper.insert(s);

                    MrpRunResponse.ShortageItem item = new MrpRunResponse.ShortageItem();
                    item.setMaterialCode(e.getKey());
                    item.setMaterialName(e.getKey());
                    item.setRequiredQty(a.required);
                    item.setCurrentStock(a.currentStock);
                    item.setOnOrderQty(a.onOrderQty);
                    item.setShortageQty(shortage);
                    item.setPurchaseSuggestion(purchase);
                    shortageItems.add(item);
                }
                totalShortage += shortage;
                totalPurchase += purchase;
            }

            run.setStatus(STATUS_COMPLETED);
            run.setCompletedAt(LocalDateTime.now());
            run.setTotalShortage(totalShortage);
            run.setTotalPurchaseSuggestion(totalPurchase);
            long elapsedMs = System.currentTimeMillis() - t0;
            run.setRemark(run.getRemark() + " | elapsedMs=" + elapsedMs);
            runMapper.updateById(run);

            MrpRunResponse resp = new MrpRunResponse();
            resp.setRunId(run.getId());
            resp.setRunNo(runNo);
            resp.setStatus(STATUS_COMPLETED);
            resp.setTriggerType(req.getTriggerType());
            resp.setTotalShortage(totalShortage);
            resp.setTotalPurchaseSuggestion(totalPurchase);
            resp.setStartedAt(run.getStartedAt().toString());
            resp.setCompletedAt(run.getCompletedAt().toString());
            resp.setShortages(shortageItems);
            return Result.ok(resp);
        } catch (Exception ex) {
            run.setStatus(STATUS_FAILED);
            run.setCompletedAt(LocalDateTime.now());
            run.setRemark("MRP_FAILED: " + ex.getMessage());
            runMapper.updateById(run);
            return Result.fail(50001, "MRP_RUN_FAILED: " + ex.getMessage());
        }
    }

    private static String buildTriggerRemark(MrpRunRequest req) {
        String type = req.getTriggerType() != null ? req.getTriggerType() : MrpTriggerService.TRIGGER_MANUAL;
        String source = req.getTriggerSource() != null ? req.getTriggerSource() : "UI";
        return "TRIGGER:" + type + "|source=" + source;
    }

    public Result<List<CrmMrpResult>> getMrpResult(Long runId) {
        if (runId == null) {
            return Result.fail(40001, "RUN_ID_REQUIRED");
        }
        CrmMrpRun run = runMapper.selectById(runId);
        if (run == null) {
            return Result.fail(40404, "MRP_RUN_NOT_FOUND");
        }
        return Result.ok(resultMapper.selectByRunId(runId));
    }

    public Result<List<CrmMrpShortage>> listShortages(Long runId) {
        if (runId == null) {
            return Result.fail(40001, "RUN_ID_REQUIRED");
        }
        return Result.ok(shortageMapper.selectByRunId(runId));
    }

    public Result<List<CrmMrpShortage>> listShortagesByMaterial(String materialCode) {
        return Result.ok(shortageMapper.selectByMaterial(materialCode));
    }

    public Result<Map<String, Object>> listRuns(String status, int page, int size) {
        int limit = size > 0 ? size : 20;
        int offset = Math.max(page, 0) * limit;
        List<Map<String, Object>> list = runMapper.selectRuns(status, limit, offset);
        for (Map<String, Object> row : list) {
            row.put("triggerType", parseTriggerType(row.get("remark")));
            if (row.get("startedAt") != null && row.get("completedAt") != null) {
                try {
                    LocalDateTime start = LocalDateTime.parse(String.valueOf(row.get("startedAt")).replace(' ', 'T'));
                    LocalDateTime end = LocalDateTime.parse(String.valueOf(row.get("completedAt")).replace(' ', 'T'));
                    row.put("durationMs", Duration.between(start, end).toMillis());
                } catch (Exception ignored) {
                    row.put("durationMs", 0);
                }
            }
        }
        Map<String, Object> p = new HashMap<>();
        p.put("list", list);
        p.put("page", page);
        p.put("size", limit);
        return Result.ok(p);
    }

    public Result<Map<String, Object>> exportMrpToPurchase(Long runId, Long operatorUserId) {
        List<CrmMrpShortage> shortages = shortageMapper.selectByRunId(runId);
        if (shortages == null || shortages.isEmpty()) {
            return Result.fail(40001, "MRP_NO_SHORTAGES");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (CrmMrpShortage s : shortages) {
            Map<String, Object> line = new HashMap<>();
            line.put("materialCode", s.getMaterialCode());
            line.put("quantity", s.getShortageQty());
            line.put("shortageId", s.getId());
            line.put("sourceWorkorders", s.getSourceWorkorders());
            line.put("requiredDate", s.getRequiredDate());
            items.add(line);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("runId", runId);
        body.put("note", "MRP runId=" + runId + " 缺料转采购申请");
        try {
            Result<Map<String, Object>> created = mrpPurchaseClient.createFromShortages(body);
            if (created == null || !created.isSuccess()) {
                String msg = created != null ? created.getMessage() : "PR_CREATE_FAILED";
                return Result.fail(50001, msg);
            }
            Map<String, Object> result = new HashMap<>(created.getData());
            result.put("runId", runId);
            result.put("shortageCount", shortages.size());
            result.put("exportedAt", LocalDateTime.now().toString());
            return Result.ok(result);
        } catch (Exception ex) {
            log.warn("[MrpService] export to purchase failed runId={}: {}", runId, ex.getMessage());
            return Result.fail(50001, "PO_SERVICE_UNAVAILABLE");
        }
    }

    private static String parseTriggerType(Object remark) {
        if (remark == null) return MrpTriggerService.TRIGGER_MANUAL;
        String s = String.valueOf(remark);
        if (s.startsWith("TRIGGER:")) {
            int pipe = s.indexOf('|');
            return pipe > 0 ? s.substring(8, pipe) : s.substring(8);
        }
        return MrpTriggerService.TRIGGER_MANUAL;
    }

    private static class MrpAccumulator {
        int required = 0;
        int currentStock = 0;
        int onOrderQty = 0;

        void addRequired(int qty) {
            this.required += qty;
        }
    }
}
