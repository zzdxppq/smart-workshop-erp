package com.btsheng.erp.production.allocation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.production.allocation.dto.AllocationBoardResponse;
import com.btsheng.erp.production.allocation.dto.AllocationStepRow;
import com.btsheng.erp.production.allocation.dto.BatchAllocationRequest;
import com.btsheng.erp.production.allocation.dto.CreateAllocationRequest;
import com.btsheng.erp.production.allocation.dto.PendingAllocationResponse;
import com.btsheng.erp.production.allocation.entity.OutsubAllocation;
import com.btsheng.erp.production.allocation.entity.OutsubAllocationVendor;
import com.btsheng.erp.production.allocation.mapper.OutsubAllocationMapper;
import com.btsheng.erp.production.allocation.mapper.OutsubAllocationVendorMapper;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderStepMapper;
import com.btsheng.erp.production.mrp.service.MrpTriggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 委外工序分配 Service（生管专用 · V1.3.7 AD-1）
 *
 * <p>仅决定工序归属（INHOUSE/OUTSOURCE），不接收 vendorId。
 */
@Service
public class OutsubAllocationService {

    public static final String DECISION_INHOUSE = "INHOUSE";
    public static final String DECISION_OUTSOURCE = "OUTSOURCE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ALLOCATED = "ALLOCATED";

    private final OutsubAllocationMapper allocationMapper;
    private final OutsubAllocationVendorMapper vendorMapper;
    private final CrmWorkorderMapper workorderMapper;
    private final CrmWorkorderStepMapper stepMapper;
    private final MrpTriggerService mrpTriggerService;

    @Autowired
    public OutsubAllocationService(OutsubAllocationMapper allocationMapper,
                                   OutsubAllocationVendorMapper vendorMapper,
                                   CrmWorkorderMapper workorderMapper,
                                   CrmWorkorderStepMapper stepMapper,
                                   MrpTriggerService mrpTriggerService) {
        this.allocationMapper = allocationMapper;
        this.vendorMapper = vendorMapper;
        this.workorderMapper = workorderMapper;
        this.stepMapper = stepMapper;
        this.mrpTriggerService = mrpTriggerService;
    }

    @Transactional
    @AuditLog(module = "production", action = "production.create_allocation")
    public Result<OutsubAllocation> createAllocation(CreateAllocationRequest req, Long operatorUserId) {
        if (req.getWorkorderId() == null || req.getProcessSeq() == null) {
            return Result.fail(40001, "WORKORDER_PROCESS_REQUIRED");
        }
        if (req.getDecision() == null
                || (!DECISION_INHOUSE.equals(req.getDecision()) && !DECISION_OUTSOURCE.equals(req.getDecision()))) {
            return Result.fail(40001, "DECISION_INVALID");
        }
        CrmWorkorder workorder = workorderMapper.selectById(req.getWorkorderId());
        if (workorder == null) {
            return Result.fail(40401, "WORKORDER_NOT_FOUND");
        }

        QueryWrapper<OutsubAllocation> dup = new QueryWrapper<>();
        dup.eq("workorder_id", req.getWorkorderId()).eq("process_seq", req.getProcessSeq());
        OutsubAllocation existing = allocationMapper.selectOne(dup);
        if (existing != null) {
            if (hasVendorAssigned(existing.getId())) {
                return Result.fail(40902, "ALLOCATION_VENDOR_LOCKED");
            }
            existing.setDecision(req.getDecision());
            existing.setDecidedByUserId(operatorUserId);
            existing.setDecidedAt(LocalDateTime.now());
            allocationMapper.updateById(existing);
            return Result.ok(existing);
        }

        OutsubAllocation allocation = new OutsubAllocation();
        allocation.setWorkorderId(req.getWorkorderId());
        allocation.setProcessSeq(req.getProcessSeq());
        allocation.setDecision(req.getDecision());
        allocation.setDecidedByUserId(operatorUserId);
        allocation.setDecidedAt(LocalDateTime.now());
        allocationMapper.insert(allocation);
        return Result.ok(allocation);
    }

    public Result<List<PendingAllocationResponse>> listPendingAllocations() {
        QueryWrapper<OutsubAllocation> q = new QueryWrapper<>();
        q.eq("decision", DECISION_OUTSOURCE).orderByDesc("decided_at");
        List<OutsubAllocation> allocations = allocationMapper.selectList(q);
        if (allocations.isEmpty()) {
            return Result.ok(List.of());
        }

        Set<Long> allocationIds = allocations.stream().map(OutsubAllocation::getId).collect(Collectors.toSet());
        QueryWrapper<OutsubAllocationVendor> vendorQ = new QueryWrapper<>();
        vendorQ.in("allocation_id", allocationIds);
        Set<Long> assigned = vendorMapper.selectList(vendorQ).stream()
                .map(OutsubAllocationVendor::getAllocationId)
                .collect(Collectors.toSet());

        List<PendingAllocationResponse> pending = new ArrayList<>();
        for (OutsubAllocation a : allocations) {
            if (assigned.contains(a.getId())) {
                continue;
            }
            PendingAllocationResponse row = new PendingAllocationResponse();
            row.setId(a.getId());
            row.setWorkorderId(a.getWorkorderId());
            row.setProcessSeq(a.getProcessSeq());
            row.setDecision(a.getDecision());
            row.setDecidedByUserId(a.getDecidedByUserId());
            row.setDecidedAt(a.getDecidedAt());

            CrmWorkorder wo = workorderMapper.selectById(a.getWorkorderId());
            if (wo != null) {
                row.setWorkorderNo(wo.getWorkorderNo());
                row.setProductCode(wo.getMaterialCode());
            }
            QueryWrapper<CrmWorkorderStep> stepQ = new QueryWrapper<>();
            stepQ.eq("workorder_id", a.getWorkorderId()).eq("step_no", a.getProcessSeq());
            CrmWorkorderStep step = stepMapper.selectOne(stepQ);
            if (step != null) {
                row.setProcessName(step.getStepName());
            }
            pending.add(row);
        }
        return Result.ok(pending);
    }

    public Result<List<OutsubAllocation>> listByWorkorder(Long workorderId) {
        if (workorderId == null) {
            return Result.fail(40001, "WORKORDER_ID_REQUIRED");
        }
        QueryWrapper<OutsubAllocation> q = new QueryWrapper<>();
        q.eq("workorder_id", workorderId).orderByAsc("process_seq");
        return Result.ok(allocationMapper.selectList(q));
    }

    /**
     * E5-S4 AC-5.4.1 · 工单工序划分看板：待分配 / 已分配分区
     */
    @Transactional(readOnly = true)
    public Result<AllocationBoardResponse> getAllocationBoard(Long workorderId) {
        if (workorderId == null) {
            return Result.fail(40001, "WORKORDER_ID_REQUIRED");
        }
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40401, "WORKORDER_NOT_FOUND");
        }

        List<CrmWorkorderStep> steps = stepMapper.selectByWorkorderId(workorderId);
        QueryWrapper<OutsubAllocation> aq = new QueryWrapper<>();
        aq.eq("workorder_id", workorderId);
        List<OutsubAllocation> allocations = allocationMapper.selectList(aq);
        Map<Integer, OutsubAllocation> allocBySeq = new HashMap<>();
        for (OutsubAllocation a : allocations) {
            allocBySeq.put(a.getProcessSeq(), a);
        }

        Set<Long> vendorLockedIds = loadVendorAssignedIds(
                allocations.stream().map(OutsubAllocation::getId).collect(Collectors.toSet()));

        AllocationBoardResponse board = new AllocationBoardResponse();
        board.setWorkorderId(workorderId);
        board.setWorkorderNo(wo.getWorkorderNo());
        board.setMaterialCode(wo.getMaterialCode());
        board.setProductName(wo.getProductName());
        board.setTotalSteps(steps.size());

        int pending = 0;
        int allocated = 0;
        int outsourcePending = 0;

        for (CrmWorkorderStep step : steps) {
            AllocationStepRow row = new AllocationStepRow();
            row.setProcessSeq(step.getStepNo());
            row.setStepName(step.getStepName());
            row.setEquipmentType(step.getEquipmentType());

            OutsubAllocation alloc = allocBySeq.get(step.getStepNo());
            if (alloc == null) {
                row.setAllocationStatus(STATUS_PENDING);
                pending++;
                board.getPendingSteps().add(row);
            } else {
                row.setAllocationStatus(STATUS_ALLOCATED);
                row.setDecision(alloc.getDecision());
                row.setAllocationId(alloc.getId());
                row.setDecidedAt(alloc.getDecidedAt());
                row.setVendorAssigned(vendorLockedIds.contains(alloc.getId()));
                allocated++;
                if (DECISION_OUTSOURCE.equals(alloc.getDecision()) && !Boolean.TRUE.equals(row.getVendorAssigned())) {
                    outsourcePending++;
                }
                board.getAllocatedSteps().add(row);
            }
        }

        board.setPendingCount(pending);
        board.setAllocatedCount(allocated);
        board.setOutsourcePendingForBuyer(outsourcePending);
        return Result.ok(board);
    }

    @Transactional
    @AuditLog(module = "production", action = "production.batch_allocation")
    public Result<Map<String, Object>> batchAllocate(BatchAllocationRequest req, Long operatorUserId) {
        if (req == null || req.getWorkorderId() == null || req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(40001, "BATCH_ITEMS_REQUIRED");
        }
        int saved = 0;
        int skippedLocked = 0;
        for (BatchAllocationRequest.Item item : req.getItems()) {
            if (item.getProcessSeq() == null || item.getDecision() == null) continue;
            CreateAllocationRequest one = new CreateAllocationRequest();
            one.setWorkorderId(req.getWorkorderId());
            one.setProcessSeq(item.getProcessSeq());
            one.setDecision(item.getDecision());
            Result<OutsubAllocation> r = createAllocation(one, operatorUserId);
            if (r.isSuccess()) {
                saved++;
            } else if ("ALLOCATION_VENDOR_LOCKED".equals(r.getMessage())) {
                skippedLocked++;
            } else {
                return Result.fail(r.getCode(), r.getMessage());
            }
        }
        // 工序分配成功后，自动将工单状态从 DRAFT 更新为 SCHEDULED（待排产）
        if (saved > 0) {
            CrmWorkorder wo = workorderMapper.selectById(req.getWorkorderId());
            if (wo != null && "DRAFT".equals(wo.getStatus())) {
                wo.setStatus("SCHEDULED");
                wo.setUpdatedAt(LocalDateTime.now());
                workorderMapper.updateById(wo);
            }
            mrpTriggerService.publishTrigger("ALLOCATION_BATCH:wo=" + req.getWorkorderId(), operatorUserId);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("saved", saved);
        out.put("skippedLocked", skippedLocked);
        return Result.ok(out);
    }

    private Set<Long> loadVendorAssignedIds(Set<Long> allocationIds) {
        if (allocationIds == null || allocationIds.isEmpty()) {
            return Set.of();
        }
        QueryWrapper<OutsubAllocationVendor> vendorQ = new QueryWrapper<>();
        vendorQ.in("allocation_id", allocationIds);
        return vendorMapper.selectList(vendorQ).stream()
                .map(OutsubAllocationVendor::getAllocationId)
                .collect(Collectors.toSet());
    }

    private boolean hasVendorAssigned(Long allocationId) {
        if (allocationId == null) return false;
        QueryWrapper<OutsubAllocationVendor> q = new QueryWrapper<>();
        q.eq("allocation_id", allocationId);
        return vendorMapper.selectCount(q) > 0;
    }
}
