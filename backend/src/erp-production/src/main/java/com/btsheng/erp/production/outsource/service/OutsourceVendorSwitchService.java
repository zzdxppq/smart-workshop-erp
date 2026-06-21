package com.btsheng.erp.production.outsource.service;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceVendorSwitch;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceVendorSwitchMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OutsourceVendorSwitchService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private final CrmOutsourceVendorSwitchMapper switchMapper;
    private final CrmOutsourceOrderMapper orderMapper;
    private final ErpDocNoGenerator docNoGenerator;

    public OutsourceVendorSwitchService(CrmOutsourceVendorSwitchMapper switchMapper,
                                          CrmOutsourceOrderMapper orderMapper,
                                          ErpDocNoGenerator docNoGenerator) {
        this.switchMapper = switchMapper;
        this.orderMapper = orderMapper;
        this.docNoGenerator = docNoGenerator;
    }

    public Result<PageResponse<Map<String, Object>>> list(String keyword, int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.max(pageSize, 1);
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = switchMapper.selectPage(keyword, size, offset).stream()
                .map(this::toView)
                .collect(Collectors.toList());
        long total = switchMapper.countByKeyword(keyword);
        return Result.ok(PageResponse.of(rows, total, page, size));
    }

    public Result<Map<String, Object>> getById(Long id) {
        CrmOutsourceVendorSwitch sw = switchMapper.selectById(id);
        if (sw == null) {
            return Result.fail(40404, "OUTSOURCE_SWITCH_NOT_FOUND");
        }
        return Result.ok(toView(sw));
    }

    @Transactional
    public Result<Map<String, Object>> confirmProd(Long id, Long operatorUserId) {
        return confirm(id, operatorUserId, true);
    }

    @Transactional
    public Result<Map<String, Object>> confirmPurch(Long id, Long operatorUserId) {
        return confirm(id, operatorUserId, false);
    }

    private Result<Map<String, Object>> confirm(Long id, Long operatorUserId, boolean prod) {
        CrmOutsourceVendorSwitch sw = switchMapper.selectById(id);
        if (sw == null) {
            return Result.fail(40404, "OUTSOURCE_SWITCH_NOT_FOUND");
        }
        if (prod) {
            sw.setProdConfirmed(1);
            sw.setProdConfirmedBy(operatorUserId);
            sw.setProdConfirmedAt(LocalDateTime.now());
        } else {
            sw.setPurchConfirmed(1);
            sw.setPurchConfirmedBy(operatorUserId);
            sw.setPurchConfirmedAt(LocalDateTime.now());
        }
        if (sw.getProdConfirmed() != null && sw.getProdConfirmed() == 1
                && sw.getPurchConfirmed() != null && sw.getPurchConfirmed() == 1) {
            sw.setStatus(STATUS_COMPLETED);
            applyVendorSwitch(sw);
        }
        sw.setUpdatedAt(LocalDateTime.now());
        switchMapper.updateById(sw);
        return Result.ok(toView(sw));
    }

    private void applyVendorSwitch(CrmOutsourceVendorSwitch sw) {
        CrmOutsourceOrder order = orderMapper.selectById(sw.getOutsourceId());
        if (order == null
                || (!"DRAFT".equals(order.getStatus()) && !"SENT".equals(order.getStatus()))) {
            return;
        }
        order.setSupplierId(sw.getNewSupplierId());
        order.setSupplierName(sw.getNewSupplierName());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private Map<String, Object> toView(CrmOutsourceVendorSwitch sw) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", sw.getId());
        row.put("switchNo", sw.getSwitchNo());
        row.put("outsourceNo", sw.getOutsourceNo());
        row.put("oldVendor", sw.getOldSupplierName());
        row.put("newVendor", sw.getNewSupplierName());
        row.put("reason", sw.getReason());
        row.put("prodConfirmed", sw.getProdConfirmed() != null && sw.getProdConfirmed() == 1);
        row.put("purchConfirmed", sw.getPurchConfirmed() != null && sw.getPurchConfirmed() == 1);
        row.put("status", sw.getStatus());
        return row;
    }
}
