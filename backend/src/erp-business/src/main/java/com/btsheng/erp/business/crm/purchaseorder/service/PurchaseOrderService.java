package com.btsheng.erp.business.crm.purchaseorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.purchaseorder.dto.CreatePoRequest;
import com.btsheng.erp.business.crm.purchaseorder.entity.CrmPurchaseOrder;
import com.btsheng.erp.business.crm.purchaseorder.entity.CrmPurchaseOrderItem;
import com.btsheng.erp.business.crm.purchaseorder.mapper.CrmPurchaseOrderItemMapper;
import com.btsheng.erp.business.crm.purchaseorder.mapper.CrmPurchaseOrderMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.rfq.service.RfqService;
import com.btsheng.erp.business.crm.vendor.entity.OutsubVendor;
import com.btsheng.erp.business.crm.vendor.mapper.OutsubVendorMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.ProcurementDataScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseOrderService {

    private static final String STATUS_PENDING = "PENDING_SHIP";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";

    private final CrmPurchaseOrderMapper poMapper;
    private final CrmPurchaseOrderItemMapper itemMapper;
    private final OutsubVendorMapper vendorMapper;
    private final CrmMaterialMapper materialMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public PurchaseOrderService(CrmPurchaseOrderMapper poMapper,
                                CrmPurchaseOrderItemMapper itemMapper,
                                OutsubVendorMapper vendorMapper,
                                CrmMaterialMapper materialMapper,
                                DocNoGenerator docNoGenerator) {
        this.poMapper = poMapper;
        this.itemMapper = itemMapper;
        this.vendorMapper = vendorMapper;
        this.materialMapper = materialMapper;
        this.docNoGenerator = docNoGenerator;
    }

    public Result<Map<String, Object>> list(String keyword, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<CrmPurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(CrmPurchaseOrder::getPoNo, keyword)
                    .or().like(CrmPurchaseOrder::getSupplierName, keyword));
        }
        if (status != null && !status.isBlank()) {
            applyStatusFilter(qw, status);
        }
        Long creatorId = ProcurementDataScopeHelper.resolveCreatorId(null);
        if (creatorId != null && ProcurementDataScopeHelper.effectiveScope() == ProcurementDataScopeHelper.Scope.SELF) {
            qw.eq(CrmPurchaseOrder::getCreatedBy, creatorId);
        }
        qw.orderByDesc(CrmPurchaseOrder::getId);
        List<CrmPurchaseOrder> all = poMapper.selectList(qw);

        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, all.size());
        List<Map<String, Object>> items = new ArrayList<>();
        if (from < all.size()) {
            for (CrmPurchaseOrder po : all.subList(from, to)) {
                items.add(toListVo(po));
            }
        }
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", items);
        pageData.put("records", items);
        pageData.put("total", all.size());
        pageData.put("pageNum", page);
        pageData.put("pageSize", size);
        return Result.ok(pageData);
    }

    public Result<Map<String, Object>> getDetail(Long id) {
        CrmPurchaseOrder po = poMapper.selectById(id);
        if (po == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "PO_NOT_FOUND");
        }
        Result<Void> scope = ProcurementDataScopeHelper.assertCreator(po.getCreatedBy());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        Map<String, Object> detail = toListVo(po);
        detail.put("items", loadItems(id));
        return Result.ok(detail);
    }

    private List<Map<String, Object>> loadItems(Long poId) {
        List<CrmPurchaseOrderItem> rows = itemMapper.selectList(
                new LambdaQueryWrapper<CrmPurchaseOrderItem>()
                        .eq(CrmPurchaseOrderItem::getPoId, poId)
                        .orderByAsc(CrmPurchaseOrderItem::getSortNo));
        List<Map<String, Object>> items = new ArrayList<>();
        for (CrmPurchaseOrderItem row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.getId());
            m.put("poItemId", row.getId());
            m.put("materialId", row.getMaterialId());
            m.put("materialCode", row.getMaterialCode());
            m.put("materialName", row.getMaterialName());
            m.put("qty", row.getQuantity());
            m.put("unitPrice", row.getUnitPrice());
            m.put("amount", row.getAmount());
            Long drawingId = itemMapper.selectDrawingIdByMaterialCode(row.getMaterialCode());
            if (drawingId != null) {
                m.put("drawingId", drawingId);
            }
            items.add(m);
        }
        return items;
    }

    @Transactional
    public Result<Map<String, Object>> create(CreatePoRequest req, Long userId) {
        if (req == null || req.getVendorName() == null || req.getVendorName().isBlank()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "vendorName 必填");
        }
        if (req.getPrId() == null && req.getRfqId() == null && !Boolean.TRUE.equals(req.getAllowDirectCreate())) {
            return Result.fail(40301, "请通过采购申请转单或询比价定标转单，禁止独立新建采购单");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "items 至少一行物料明细");
        }

        OutsubVendor vendor = vendorMapper.selectOne(
                new LambdaQueryWrapper<OutsubVendor>()
                        .eq(OutsubVendor::getVendorName, req.getVendorName().trim())
                        .last("LIMIT 1"));
        Long supplierId = vendor != null ? vendor.getId() : 0L;

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CrmPurchaseOrderItem> itemRows = new ArrayList<>();
        int sortNo = 1;
        LocalDate deliveryDate = parseDeliveryDate(req.getDeliveryDate());

        for (CreatePoRequest.PoItemLine line : req.getItems()) {
            if (line == null || line.getQuantity() == null || line.getQuantity() <= 0) {
                return Result.fail(Result.CODE_PARAM_MISSING, "明细数量必须大于 0");
            }
            CrmMaterial material = resolveMaterial(line);
            if (material == null) {
                return Result.fail(Result.CODE_PARAM_MISSING, "物料不存在或未选择");
            }
            BigDecimal unitPrice = line.getUnitPrice() != null ? line.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(line.getQuantity()));

            CrmPurchaseOrderItem item = new CrmPurchaseOrderItem();
            item.setMaterialId(material.getId());
            item.setMaterialCode(material.getMaterialCode());
            item.setMaterialName(material.getMaterialName());
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setDeliveryDate(deliveryDate);
            item.setSortNo(sortNo++);
            item.setCreatedAt(LocalDateTime.now());
            itemRows.add(item);
            totalAmount = totalAmount.add(amount);
        }

        CrmPurchaseOrder po = new CrmPurchaseOrder();
        po.setPoNo(docNoGenerator.nextNo(RfqService.PO_NO_PREFIX));
        po.setRfqId(req.getRfqId());
        po.setPrId(req.getPrId());
        po.setPrNo(req.getPrNo());
        po.setWorkorderNo(req.getWorkorderNo());
        po.setMrpRunId(req.getMrpRunId());
        po.setSupplierId(supplierId);
        po.setSupplierName(req.getVendorName().trim());
        po.setTotalAmount(totalAmount);
        po.setStatus(STATUS_PENDING);
        po.setSourceType(req.getSourceType() != null ? req.getSourceType() : "FROM_ORDER");
        po.setApprovalStatus(APPROVAL_PENDING);
        po.setRemark(req.getNote());
        po.setCreatedBy(userId != null ? userId : 1L);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        poMapper.insert(po);

        for (CrmPurchaseOrderItem item : itemRows) {
            item.setPoId(po.getId());
            item.setPurchaseOrderId(po.getId());
            itemMapper.insert(item);
        }

        Map<String, Object> result = toListVo(po);
        result.put("items", loadItems(po.getId()));
        return Result.ok(result);
    }

    private CrmMaterial resolveMaterial(CreatePoRequest.PoItemLine line) {
        if (line.getMaterialId() != null) {
            CrmMaterial byId = materialMapper.selectById(line.getMaterialId());
            if (byId != null) {
                return byId;
            }
        }
        if (line.getMaterialCode() != null && !line.getMaterialCode().isBlank()) {
            return materialMapper.selectByMaterialCode(line.getMaterialCode().trim());
        }
        return null;
    }

    private LocalDate parseDeliveryDate(String deliveryDate) {
        if (deliveryDate == null || deliveryDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(deliveryDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Transactional
    public Result<Map<String, Object>> confirm(Long id) {
        CrmPurchaseOrder po = poMapper.selectById(id);
        if (po == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "PO_NOT_FOUND");
        }
        if (STATUS_CANCELLED.equals(po.getStatus())) {
            return Result.fail(Result.CODE_CONFLICT, "PO_ALREADY_CLOSED");
        }
        po.setApprovalStatus(APPROVAL_APPROVED);
        po.setUpdatedAt(LocalDateTime.now());
        poMapper.updateById(po);
        return Result.ok(toListVo(po));
    }

    @Transactional
    public Result<Map<String, Object>> close(Long id) {
        CrmPurchaseOrder po = poMapper.selectById(id);
        if (po == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "PO_NOT_FOUND");
        }
        po.setStatus(STATUS_CANCELLED);
        po.setUpdatedAt(LocalDateTime.now());
        poMapper.updateById(po);
        return Result.ok(toListVo(po));
    }

    private void applyStatusFilter(LambdaQueryWrapper<CrmPurchaseOrder> qw, String uiStatus) {
        switch (uiStatus) {
            case "DRAFT" -> qw.eq(CrmPurchaseOrder::getApprovalStatus, APPROVAL_PENDING)
                    .ne(CrmPurchaseOrder::getStatus, STATUS_CANCELLED);
            case "CONFIRMED" -> qw.eq(CrmPurchaseOrder::getApprovalStatus, APPROVAL_APPROVED)
                    .ne(CrmPurchaseOrder::getStatus, STATUS_CANCELLED);
            case "CLOSED" -> qw.eq(CrmPurchaseOrder::getStatus, STATUS_CANCELLED);
            default -> qw.eq(CrmPurchaseOrder::getStatus, uiStatus);
        }
    }

    private Map<String, Object> toListVo(CrmPurchaseOrder po) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", po.getId());
        m.put("poNo", po.getPoNo());
        m.put("vendorName", po.getSupplierName());
        m.put("totalAmount", po.getTotalAmount());
        m.put("status", toUiStatus(po));
        m.put("sourceType", po.getSourceType());
        m.put("purchaseReason", po.getPurchaseReason());
        m.put("prNo", po.getPrNo());
        m.put("workorderNo", po.getWorkorderNo());
        m.put("deliveryDate", null);
        m.put("createdAt", po.getCreatedAt());
        return m;
    }

    private String toUiStatus(CrmPurchaseOrder po) {
        if (STATUS_CANCELLED.equals(po.getStatus())) {
            return "CLOSED";
        }
        if (APPROVAL_APPROVED.equals(po.getApprovalStatus())) {
            return "CONFIRMED";
        }
        return "DRAFT";
    }
}
