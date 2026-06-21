package com.btsheng.erp.business.crm.pricecontrol.service;

import com.btsheng.erp.business.crm.pricecontrol.dto.CheckPriceRequest;
import com.btsheng.erp.business.crm.pricecontrol.dto.SetPriceLimitRequest;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceControl;
import com.btsheng.erp.business.crm.pricecontrol.entity.CrmPriceHistory;
import com.btsheng.erp.business.crm.pricecontrol.mapper.CrmPriceControlMapper;
import com.btsheng.erp.business.crm.pricecontrol.mapper.CrmPriceHistoryMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.33 · 采购·价格控制 Service (FR-8-2)
 *
 * <p>4 业务方法：setPriceLimit / getPriceLimit / checkPrice / listPriceHistory
 * <p>限价单号：PL{yyyyMMdd}{seq:4}
 * <p>3 P1 修补：价格上限非�?/ 偏差�?�?20% ALERTED / 历史�?3 月内 / 唯一索引 (material_id, vendor_id)
 */
@Service
public class PriceControlService {

    /** P1 修补 2：偏差率阈�?20% */
    public static final BigDecimal DEVIATION_ALERT_THRESHOLD = new BigDecimal("0.20");

    /** P1 修补 3：历史价 3 月内 */
    public static final int HISTORY_MONTHS = 3;

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ALERTED = "ALERTED";
    public static final String STATUS_OVER_LIMIT = "OVER_LIMIT";

    private final CrmPriceControlMapper controlMapper;
    private final CrmPriceHistoryMapper historyMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public PriceControlService(CrmPriceControlMapper controlMapper,
                               CrmPriceHistoryMapper historyMapper,
                               DocNoGenerator docNoGenerator) {
        this.controlMapper = controlMapper;
        this.historyMapper = historyMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-8.2.1：设置物料限�?     * P1 修补 1：价格上限非�?     */
    @Transactional
    @AuditLog(module = "price_control", action = "price_control.set_limit")
    // V1.3.8 Sprint 7 集成 E：价格变更触�?mat:detail + mat:price-history 缓存失效
            @CacheEvict(value = {"mat:detail", "mat:price-history"}, allEntries = true)
    public Result<CrmPriceControl> setPriceLimit(SetPriceLimitRequest req, Long operatorUserId, String operatorName) {
        if (req == null || req.getMaterialId() == null) {
            return Result.fail(40001, "MATERIAL_ID_REQUIRED");
        }
        // P1 修补 1：非�?
            if (req.getPriceLimit() == null || req.getPriceLimit().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "PRICE_LIMIT_NEGATIVE");
        }
        if (req.getEffectiveDate() == null) {
            return Result.fail(40001, "EFFECTIVE_DATE_REQUIRED");
        }
        CrmPriceControl pc = new CrmPriceControl();
        pc.setControlNo(docNoGenerator.nextPriceControlNo());
        pc.setMaterialId(req.getMaterialId());
        pc.setMaterialCode(req.getMaterialCode());
        pc.setMaterialName(req.getMaterialName());
        pc.setVendorId(req.getVendorId());
        pc.setVendorName(req.getVendorName());
        pc.setPriceLimit(req.getPriceLimit());
        pc.setCurrency(req.getCurrency() == null ? "CNY" : req.getCurrency());
        pc.setEffectiveDate(req.getEffectiveDate());
        pc.setExpiryDate(req.getExpiryDate());
        pc.setStatus("ACTIVE");
        pc.setSetBy(operatorUserId);
        pc.setSetByName(operatorName);
        pc.setRemark(req.getRemark());
        pc.setCreatedAt(LocalDateTime.now());
        pc.setUpdatedAt(LocalDateTime.now());
        controlMapper.insert(pc);
        return Result.ok(pc);
    }

    /**
     * AC-8.2.2：获取物料限价（厂商专享 > 通用�?     */
    @AuditLog(module = "price_control", action = "price_control.get_limit")
    public Result<CrmPriceControl> getPriceLimit(Long materialId, Long vendorId) {
        if (materialId == null) {
            return Result.fail(40001, "MATERIAL_ID_REQUIRED");
        }
        CrmPriceControl pc = null;
        if (vendorId != null) {
            pc = controlMapper.selectByMaterialAndVendor(materialId, vendorId);
        }
        if (pc == null) {
            pc = controlMapper.selectGenericByMaterial(materialId);
        }
        if (pc == null) {
            return Result.fail(40404, "PRICE_LIMIT_NOT_FOUND");
        }
        return Result.ok(pc);
    }

    /**
     * AC-8.2.2：价格校验（限价 + 历史�?+ 偏差率）
     * P1 修补 2：偏差率 �?20% ALERTED
     * P1 修补 3：历史价 3 月内
     */
    @AuditLog(module = "price_control", action = "price_control.check")
    public Result<Map<String, Object>> checkPrice(CheckPriceRequest req) {
        if (req == null || req.getMaterialId() == null || req.getVendorId() == null) {
            return Result.fail(40001, "MATERIAL_VENDOR_REQUIRED");
        }
        if (req.getUnitPrice() == null || req.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "UNIT_PRICE_INVALID");
        }
        // 1. 限价校验
            CrmPriceControl pc = null;
        CrmPriceControl vendorSpecific = controlMapper.selectByMaterialAndVendor(req.getMaterialId(), req.getVendorId());
        if (vendorSpecific != null) {
            pc = vendorSpecific;
        } else {
            pc = controlMapper.selectGenericByMaterial(req.getMaterialId());
        }
        boolean overLimit = false;
        BigDecimal priceLimit = null;
        if (pc != null) {
            priceLimit = pc.getPriceLimit();
            if (req.getUnitPrice().compareTo(priceLimit) > 0) {
                overLimit = true;
            }
        }

        // 2. 历史�?+ 偏差�?
            LocalDate since = LocalDate.now().minusMonths(HISTORY_MONTHS);
        BigDecimal avgPrice = historyMapper.avgPrice(req.getMaterialId(), req.getVendorId(), since);
        BigDecimal deviation = null;
        String alertLevel = STATUS_OK;
        if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
            deviation = req.getUnitPrice().subtract(avgPrice)
                    .divide(avgPrice, 4, RoundingMode.HALF_UP);
            // P1 修补 2：偏差率 �?20% ALERTED
            if (deviation.abs().compareTo(DEVIATION_ALERT_THRESHOLD) >= 0) {
                alertLevel = STATUS_ALERTED;
            }
        }

        if (overLimit) {
            alertLevel = STATUS_OVER_LIMIT;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("materialId", req.getMaterialId());
        data.put("vendorId", req.getVendorId());
        data.put("unitPrice", req.getUnitPrice());
        data.put("priceLimit", priceLimit);
        data.put("avgPrice", avgPrice);
        data.put("deviation", deviation);
        data.put("alertLevel", alertLevel);
        data.put("overLimit", overLimit);
        return Result.ok(data);
    }

    /**
     * 历史价列表（3 月内�?     * P1 修补 3：历史价 3 月内
     */
    @AuditLog(module = "price_control", action = "price_control.list_history")
    public Result<List<CrmPriceHistory>> listPriceHistory(Long materialId, Long vendorId) {
        if (materialId == null) {
            return Result.fail(40001, "MATERIAL_ID_REQUIRED");
        }
        LocalDate since = LocalDate.now().minusMonths(HISTORY_MONTHS);
        List<CrmPriceHistory> list;
        if (vendorId != null) {
            list = historyMapper.selectByMaterialVendorSince(materialId, vendorId, since);
        } else {
            list = historyMapper.selectByMaterialSince(materialId, since);
        }
        if (list == null) {
            list = new ArrayList<>();
        }
        return Result.ok(list);
    }
}
