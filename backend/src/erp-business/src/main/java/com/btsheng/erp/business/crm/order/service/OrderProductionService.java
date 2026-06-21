package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.integration.client.WorkorderClient;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售订单 → 生产工单（AC-5.1.1 · Epic 5 Feign）
 */
@Service
public class OrderProductionService {

    private static final Map<String, String> DEMO_DRAWING_MATERIAL = Map.of(
            "DWG-DEMO-001", "CP-DEMO-001",
            "DWG-DEMO-002", "CP-DEMO-002"
    );

    private final WorkorderClient workorderClient;
    private final CrmOrderItemMapper itemMapper;
    private final CrmDrawingMapper drawingMapper;
    private final CrmMaterialMapper materialMapper;

    @Autowired
    public OrderProductionService(WorkorderClient workorderClient,
                                  CrmOrderItemMapper itemMapper,
                                  CrmDrawingMapper drawingMapper,
                                  CrmMaterialMapper materialMapper) {
        this.workorderClient = workorderClient;
        this.itemMapper = itemMapper;
        this.drawingMapper = drawingMapper;
        this.materialMapper = materialMapper;
    }

    public Result<Map<String, Object>> createWorkorderFromOrder(CrmOrder order, Long operatorUserId) {
        if (order.getProductionOrderNo() != null && !order.getProductionOrderNo().isBlank()) {
            Result<Map<String, Object>> existing = workorderClient.getByNo(order.getProductionOrderNo());
            if (existing != null && existing.isSuccess() && existing.getData() != null) {
                return existing;
            }
        }

        Result<Map<String, Object>> linked = workorderClient.getBySalesOrderId(order.getId());
        if (linked != null && linked.isSuccess() && linked.getData() != null) {
            return linked;
        }

        List<CrmOrderItem> items = itemMapper.selectByOrderId(order.getId());
        if (items == null || items.isEmpty()) {
            return Result.fail(40001, "ORDER_ITEMS_EMPTY");
        }

        ResolvedProduct product = resolveProduct(items.get(0));
        if (product.materialCode == null || product.materialCode.isBlank()) {
            return Result.fail(40001, "ORDER_MATERIAL_NOT_RESOLVED");
        }

        Map<String, Object> req = new HashMap<>();
        req.put("salesOrderId", order.getId());
        req.put("salesOrderNo", order.getOrderNo());
        req.put("materialCode", product.materialCode);
        req.put("productName", product.productName);
        req.put("qty", product.qty);
        req.put("drawingId", product.drawingId);
        req.put("bomId", product.bomId);
        req.put("processRouteId", product.processRouteId);
        req.put("deliveryDate", order.getDeliveryDate() != null ? order.getDeliveryDate().toString() : null);
        req.put("priority", order.getIsUrgent() != null && order.getIsUrgent() == 1 ? 1 : 5);
        req.put("isFa", order.getIsFa());

        Result<Map<String, Object>> created = workorderClient.createFromOrder(req, operatorUserId);
        if (created == null || !created.isSuccess()) {
            return created != null ? created : Result.fail(50301, "PRODUCTION_SERVICE_UNAVAILABLE");
        }
        return created;
    }

    private ResolvedProduct resolveProduct(CrmOrderItem item) {
        ResolvedProduct p = new ResolvedProduct();
        p.qty = item.getQuantity() != null ? item.getQuantity() : 1;

        if (item.getDrawingNo() != null && !item.getDrawingNo().isBlank()) {
            CrmDrawing drawing = drawingMapper.selectByDrawingNoAndVersion(item.getDrawingNo(), "v1");
            if (drawing == null) {
                drawing = drawingMapper.selectLatestByDrawingNo(item.getDrawingNo());
            }
            if (drawing != null) {
                p.drawingId = drawing.getId();
                if (drawing.getMaterialCode() != null && !drawing.getMaterialCode().isBlank()) {
                    p.materialCode = drawing.getMaterialCode();
                }
            }
            if (p.materialCode == null) {
                p.materialCode = DEMO_DRAWING_MATERIAL.get(item.getDrawingNo());
            }
        }

        if (p.materialCode != null) {
            CrmMaterial material = materialMapper.selectByMaterialCode(p.materialCode);
            if (material != null) {
                p.productName = material.getMaterialName();
                p.processRouteId = material.getProcessId();
            }
        }

        if (p.productName == null || p.productName.isBlank()) {
            String spec = item.getSpec() != null ? " " + item.getSpec() : "";
            p.productName = (item.getMaterial() != null ? item.getMaterial() : "产品") + spec;
        }
        return p;
    }

    private static class ResolvedProduct {
        String materialCode;
        String productName;
        Integer qty;
        Long drawingId;
        Long bomId;
        Long processRouteId;
    }
}
