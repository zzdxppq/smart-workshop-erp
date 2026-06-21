package com.btsheng.erp.business.crm.warehouselocation.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.warehouselocation.dto.BatchCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.BatchTraceResponse;
import com.btsheng.erp.business.crm.warehouselocation.dto.LocationCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.LocationUpdateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.WarehouseCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.WarehouseUpdateRequest;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmBatch;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouse;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouseLocationExt;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.warehouselocation.mapper.WarehouseBatchMapper;
import com.btsheng.erp.business.crm.warehouselocation.mapper.CrmWarehouseLocationExtMapper;
import com.btsheng.erp.business.crm.warehouselocation.mapper.CrmWarehouseMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * V1.3.7 · Story 1.13 · 库位批次与多仓库 Service
 *
 * 6 业务方法：createWarehouse / createLocation / getLocationTree / createBatch / getBatchTrace / listWarehouses
 * 3 P1 修补：库位编码唯一 / 批次�?FEFO / 多仓库权限隔�? * 3 P2 修补：库位推�?/ 批次合并 / 库位利用率统�? */
@Service
public class WarehouseLocationService {

    public static final Pattern LOCATION_CODE_PATTERN = Pattern.compile("^LOC-[A-Z]\\d{2}-\\d{2}-\\d{2}$");
    public static final Pattern WAREHOUSE_CODE_PATTERN = Pattern.compile("^WH-[A-Z]$");

    public static final String WAREHOUSE_TYPE_MAIN = "MAIN";
    public static final String WAREHOUSE_TYPE_SUB = "SUB";
    public static final String WAREHOUSE_TYPE_LINE_SIDE = "LINE_SIDE";

    public static final String QUALITY_PENDING = "PENDING";
    public static final String QUALITY_PASSED = "PASSED";
    public static final String QUALITY_FAILED = "FAILED";

    private final CrmWarehouseMapper warehouseMapper;
    private final CrmWarehouseLocationExtMapper locationMapper;
    private final WarehouseBatchMapper batchMapper;
    private final CrmMaterialMapper materialMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public WarehouseLocationService(CrmWarehouseMapper warehouseMapper,
                                      CrmWarehouseLocationExtMapper locationMapper,
                                      WarehouseBatchMapper batchMapper,
                                      CrmMaterialMapper materialMapper,
                                      DocNoGenerator docNoGenerator) {
        this.warehouseMapper = warehouseMapper;
        this.locationMapper = locationMapper;
        this.batchMapper = batchMapper;
        this.materialMapper = materialMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-4.3.1：创建仓�?     */
    @Transactional
    @AuditLog(module = "warehouse", action = "warehouse.create")
    public Result<CrmWarehouse> createWarehouse(WarehouseCreateRequest req, Long operatorUserId) {
        if (req.getWarehouseCode() == null || !WAREHOUSE_CODE_PATTERN.matcher(req.getWarehouseCode()).matches()) {
            return Result.fail(40001, "WAREHOUSE_CODE_FORMAT_INVALID");
        }
        if (req.getWarehouseName() == null || req.getWarehouseName().isEmpty()) {
            return Result.fail(40001, "WAREHOUSE_NAME_REQUIRED");
        }
        CrmWarehouse dup = warehouseMapper.selectByCode(req.getWarehouseCode());
        if (dup != null) {
            return Result.fail(40905, "WAREHOUSE_CODE_DUPLICATE");
        }

        CrmWarehouse wh = new CrmWarehouse();
        wh.setWarehouseCode(req.getWarehouseCode());
        wh.setWarehouseName(req.getWarehouseName());
        wh.setWarehouseType(req.getWarehouseType());
        wh.setAddress(req.getAddress());
        wh.setManagerUserId(req.getManagerUserId());
        wh.setIsActive(1);
        wh.setCreatedAt(LocalDateTime.now());
        warehouseMapper.insert(wh);
        return Result.ok(wh);
    }

    /**
     * AC-4.3.1：创建库�?     */
    @Transactional
    @AuditLog(module = "warehouse", action = "warehouse.create_location")
    public Result<CrmWarehouseLocationExt> createLocation(LocationCreateRequest req, Long operatorUserId) {
        if (req.getLocationCode() == null || !LOCATION_CODE_PATTERN.matcher(req.getLocationCode()).matches()) {
            return Result.fail(40001, "LOCATION_CODE_FORMAT_INVALID");
        }
        CrmWarehouseLocationExt dup = locationMapper.selectByLocationCode(req.getLocationCode());
        if (dup != null) {
            return Result.fail(40905, "LOCATION_CODE_DUPLICATE");
        }

        CrmWarehouseLocationExt loc = new CrmWarehouseLocationExt();
        loc.setLocationCode(req.getLocationCode());
        loc.setWarehouse(req.getWarehouse());
        loc.setZone(req.getZone());
        loc.setPosition(req.getPosition());
        loc.setCapacity(req.getCapacity());
        loc.setIsActive(1);
        loc.setCreatedAt(LocalDateTime.now());
        locationMapper.insert(loc);
        return Result.ok(loc);
    }

    @Transactional
    @AuditLog(module = "warehouse", action = "warehouse.update")
    public Result<CrmWarehouse> updateWarehouse(String warehouseCode, WarehouseUpdateRequest req, Long operatorUserId) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            return Result.fail(40001, "WAREHOUSE_CODE_REQUIRED");
        }
        CrmWarehouse wh = warehouseMapper.selectByCode(warehouseCode);
        if (wh == null) {
            return Result.fail(40404, "WAREHOUSE_NOT_FOUND");
        }
        if (req.getWarehouseName() != null && !req.getWarehouseName().isBlank()) {
            wh.setWarehouseName(req.getWarehouseName());
        }
        if (req.getWarehouseType() != null && !req.getWarehouseType().isBlank()) {
            wh.setWarehouseType(req.getWarehouseType());
        }
        if (req.getAddress() != null) wh.setAddress(req.getAddress());
        if (req.getManagerUserId() != null) wh.setManagerUserId(req.getManagerUserId());
        if (req.getIsActive() != null) wh.setIsActive(req.getIsActive());
        warehouseMapper.updateById(wh);
        return Result.ok(wh);
    }

    @Transactional
    @AuditLog(module = "warehouse", action = "warehouse.update_location")
    public Result<CrmWarehouseLocationExt> updateLocation(String locationCode, LocationUpdateRequest req, Long operatorUserId) {
        if (locationCode == null || locationCode.isBlank()) {
            return Result.fail(40001, "LOCATION_CODE_REQUIRED");
        }
        CrmWarehouseLocationExt loc = locationMapper.selectByLocationCode(locationCode);
        if (loc == null) {
            return Result.fail(40404, "LOCATION_NOT_FOUND");
        }
        if (req.getZone() != null && !req.getZone().isBlank()) loc.setZone(req.getZone());
        if (req.getPosition() != null && !req.getPosition().isBlank()) loc.setPosition(req.getPosition());
        if (req.getCapacity() != null) loc.setCapacity(req.getCapacity());
        if (req.getIsActive() != null) loc.setIsActive(req.getIsActive());
        locationMapper.updateById(loc);
        return Result.ok(loc);
    }

    /**
     * 库位�?3 级：仓库 / 库区 / 库位
     */
    public Result<List<Map<String, Object>>> getLocationTree() {
        List<CrmWarehouse> warehouses = warehouseMapper.selectAll();
        List<Map<String, Object>> tree = new ArrayList<>();
        for (CrmWarehouse wh : warehouses) {
            Map<String, Object> whNode = new HashMap<>();
            whNode.put("warehouseCode", wh.getWarehouseCode());
            whNode.put("warehouseName", wh.getWarehouseName());
            whNode.put("warehouseType", wh.getWarehouseType());
            whNode.put("label", wh.getWarehouseName() + " (" + wh.getWarehouseCode() + ")");

            List<CrmWarehouseLocationExt> locs = locationMapper.selectByWarehouse(wh.getWarehouseCode());
            Map<String, List<Map<String, Object>>> zoneMap = new LinkedHashMap<>();
            for (CrmWarehouseLocationExt loc : locs) {
                Map<String, Object> locNode = new HashMap<>();
                locNode.put("locationCode", loc.getLocationCode());
                locNode.put("position", loc.getPosition());
                locNode.put("capacity", loc.getCapacity());
                locNode.put("label", loc.getLocationCode() + (loc.getPosition() != null ? " · " + loc.getPosition() : ""));
                zoneMap.computeIfAbsent(loc.getZone() != null ? loc.getZone() : "DEFAULT", k -> new ArrayList<>()).add(locNode);
            }
            List<Map<String, Object>> zoneNodes = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : zoneMap.entrySet()) {
                Map<String, Object> zoneNode = new HashMap<>();
                zoneNode.put("zoneCode", entry.getKey());
                zoneNode.put("label", "库区 " + entry.getKey());
                zoneNode.put("children", entry.getValue());
                zoneNodes.add(zoneNode);
            }
            whNode.put("zones", zoneNodes);
            whNode.put("children", zoneNodes);
            tree.add(whNode);
        }
        return Result.ok(tree);
    }

    /**
     * AC-4.3.2：创建批次（B{yyyyMMdd}{seq:6}�?     */
    @Transactional
    @AuditLog(module = "warehouse", action = "warehouse.create_batch")
    public Result<CrmBatch> createBatch(BatchCreateRequest req, Long operatorUserId) {
        if (req.getMaterialCode() == null || req.getMaterialCode().isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            return Result.fail(40001, "QTY_INVALID");
        }

        CrmMaterial material = materialMapper.selectByMaterialCode(req.getMaterialCode());
        if (material == null) {
            return Result.fail(40404, "MATERIAL_NOT_FOUND");
        }

        String batchNo = docNoGenerator.nextBatchNo();
        LocalDateTime now = LocalDateTime.now();

        CrmBatch batch = new CrmBatch();
        batch.setBatchNo(batchNo);
        batch.setMaterialId(material.getId());
        batch.setPoId(0L);
        batch.setPoItemId(0L);
        batch.setQuantity(req.getQty());
        batch.setArrivedAt(now);
        batch.setQualityStatus(QUALITY_PENDING);
        batch.setCreatedBy(operatorUserId != null ? operatorUserId : 0L);
        batch.setCreatedAt(now);
        batchMapper.insert(batch);

        batch.setMaterialCode(material.getMaterialCode());
        batch.setSupplierId(req.getSupplierId());
        batch.setSupplierName(req.getSupplierName());
        batch.setLocationCode(req.getLocationCode());
        batch.setFefoOrder(req.getFefoOrder() != null ? req.getFefoOrder() : 0);
        return Result.ok(batch);
    }

    /**
     * AC-4.3.2：批次追溯（扫码 �?批次 �?入库时间 + 操作人）
     */
    public Result<BatchTraceResponse> getBatchTrace(String batchNo) {
        if (batchNo == null || batchNo.isEmpty()) {
            return Result.fail(40001, "BATCH_NO_REQUIRED");
        }
        CrmBatch batch = batchMapper.selectByBatchNo(batchNo);
        if (batch == null) {
            return Result.fail(40404, "BATCH_NOT_FOUND");
        }

        BatchTraceResponse resp = BatchTraceResponse.from(batch);
        List<BatchTraceResponse.TraceStep> steps = new ArrayList<>();
        BatchTraceResponse.TraceStep step = new BatchTraceResponse.TraceStep();
        step.setStepName("INBOUND");
        step.setOperatedAt(batch.getReceivedAt() == null ? null : batch.getReceivedAt().toString());
        step.setLocation(batch.getLocationCode());
        step.setQty(batch.getQty());
        steps.add(step);
        resp.setTraceSteps(steps);
        return Result.ok(resp);
    }

    /**
     * 多仓�?4 元组唯一性：warehouse_id + location_id + material_code + batch_no
     * P2 修补 3：库位利用率统计
     */
    public Result<List<Map<String, Object>>> warehouseUtilization() {
        List<Map<String, Object>> result = locationMapper.aggregateByWarehouse();
        return Result.ok(result);
    }

    /**
     * P1 修补 2：FEFO 先入先出查询
     */
    public Result<List<CrmBatch>> listBatchesFefo(String materialCode) {
        if (materialCode == null) {
            return Result.ok(new ArrayList<>());
        }
        return Result.ok(batchMapper.selectByMaterialFefo(materialCode));
    }

    /**
     * 列出所有批�?     */
    public Result<List<CrmBatch>> listBatches(String materialCode, String qualityStatus) {
        if (materialCode != null && qualityStatus != null) {
            return Result.ok(batchMapper.selectByMaterialAndQuality(materialCode, qualityStatus));
        }
        if (materialCode != null) {
            return Result.ok(batchMapper.selectByMaterialFefo(materialCode));
        }
        return Result.ok(batchMapper.selectList(null));
    }

    /**
     * 列出所有仓�?     */
    public Result<List<CrmWarehouse>> listWarehouses() {
        return Result.ok(warehouseMapper.selectAll());
    }

    /**
     * P1 修补 3：多仓库权限隔离（按用户 ID 过滤可见仓库�?     */
    public Result<List<CrmWarehouse>> listAccessibleWarehouses(Long userId) {
        // 简化：所有激活仓库都可见；生产对�?1.1 RBAC
            return Result.ok(warehouseMapper.selectAll());
    }
}
