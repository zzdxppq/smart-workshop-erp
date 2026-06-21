package com.btsheng.erp.business.crm.purchaserequest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.purchaseorder.dto.CreatePoRequest;
import com.btsheng.erp.business.crm.purchaseorder.entity.CrmPurchaseOrder;
import com.btsheng.erp.business.crm.purchaseorder.entity.CrmPurchaseOrderItem;
import com.btsheng.erp.business.crm.purchaseorder.mapper.CrmPurchaseOrderItemMapper;
import com.btsheng.erp.business.crm.purchaseorder.mapper.CrmPurchaseOrderMapper;
import com.btsheng.erp.business.crm.purchaserequest.dto.ConvertPrToPoRequest;
import com.btsheng.erp.business.crm.purchaserequest.entity.CrmPurchaseRequest;
import com.btsheng.erp.business.crm.purchaserequest.mapper.CrmPurchaseRequestMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.rfq.service.RfqService;
import com.btsheng.erp.business.crm.vendor.entity.OutsubVendor;
import com.btsheng.erp.business.crm.vendor.mapper.OutsubVendorMapper;
import com.btsheng.erp.core.model.Result;
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
public class PurchaseRequestService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_CONVERTED = "CONVERTED";
    public static final String PR_PREFIX = "PR";

    private final CrmPurchaseRequestMapper prMapper;
    private final CrmPurchaseOrderMapper poMapper;
    private final CrmPurchaseOrderItemMapper poItemMapper;
    private final CrmMaterialMapper materialMapper;
    private final OutsubVendorMapper vendorMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public PurchaseRequestService(CrmPurchaseRequestMapper prMapper,
                                  CrmPurchaseOrderMapper poMapper,
                                  CrmPurchaseOrderItemMapper poItemMapper,
                                  CrmMaterialMapper materialMapper,
                                  OutsubVendorMapper vendorMapper,
                                  DocNoGenerator docNoGenerator) {
        this.prMapper = prMapper;
        this.poMapper = poMapper;
        this.poItemMapper = poItemMapper;
        this.materialMapper = materialMapper;
        this.vendorMapper = vendorMapper;
        this.docNoGenerator = docNoGenerator;
    }

    public Result<Map<String, Object>> list(String status, String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<CrmPurchaseRequest> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(CrmPurchaseRequest::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(CrmPurchaseRequest::getPrNo, keyword)
                    .or().like(CrmPurchaseRequest::getMaterialCode, keyword)
                    .or().like(CrmPurchaseRequest::getWorkorderNo, keyword));
        }
        qw.orderByDesc(CrmPurchaseRequest::getId);
        List<CrmPurchaseRequest> all = prMapper.selectList(qw);

        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, all.size());
        List<Map<String, Object>> items = new ArrayList<>();
        if (from < all.size()) {
            for (CrmPurchaseRequest pr : all.subList(from, to)) {
                items.add(toVo(pr));
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
        CrmPurchaseRequest pr = prMapper.selectById(id);
        if (pr == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "PR_NOT_FOUND");
        }
        return Result.ok(toVo(pr));
    }

    /** MRP 缺料批量生成采购申请（24h 同物料去重） */
    @Transactional
    public Result<Map<String, Object>> createFromShortages(Map<String, Object> body, Long userId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) {
            return Result.fail(40001, "MRP_SHORTAGES_EMPTY");
        }
        Long runId = body.get("runId") instanceof Number n ? n.longValue() : null;
        String note = body.get("note") != null ? String.valueOf(body.get("note")) : "MRP缺料转采购申请";

        LocalDateTime dedupSince = LocalDateTime.now().minusHours(24);
        List<CrmPurchaseRequest> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Map<String, Object> item : items) {
            String code = item.get("materialCode") != null ? String.valueOf(item.get("materialCode")).trim() : null;
            if (code == null || code.isBlank()) continue;

            long dup = prMapper.selectCount(new LambdaQueryWrapper<CrmPurchaseRequest>()
                    .eq(CrmPurchaseRequest::getMaterialCode, code)
                    .ge(CrmPurchaseRequest::getCreatedAt, dedupSince)
                    .in(CrmPurchaseRequest::getStatus, STATUS_PENDING, STATUS_PARTIAL));
            if (dup > 0) {
                skipped.add(code);
                continue;
            }

            CrmMaterial material = materialMapper.selectOne(
                    new LambdaQueryWrapper<CrmMaterial>().eq(CrmMaterial::getMaterialCode, code).last("LIMIT 1"));
            if (material == null) {
                skipped.add(code + "(物料不存在)");
                continue;
            }

            Object qty = item.get("quantity");
            if (qty == null) qty = item.get("shortageQty");
            int requiredQty = qty instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(qty));

            CrmPurchaseRequest pr = new CrmPurchaseRequest();
            pr.setPrNo(docNoGenerator.nextNo(PR_PREFIX));
            pr.setMrpRunId(runId);
            if (item.get("shortageId") instanceof Number sid) {
                pr.setMrpShortageId(sid.longValue());
            }
            if (item.get("workorderNo") != null) {
                pr.setWorkorderNo(String.valueOf(item.get("workorderNo")));
            } else if (item.get("sourceWorkorders") != null) {
                String wo = String.valueOf(item.get("sourceWorkorders"));
                pr.setWorkorderNo(wo.contains(",") ? wo.split(",")[0].trim() : wo.trim());
            }
            pr.setMaterialId(material.getId());
            pr.setMaterialCode(material.getMaterialCode());
            pr.setMaterialName(material.getMaterialName());
            pr.setRequiredQty(requiredQty);
            pr.setConvertedQty(0);
            if (item.get("requiredDate") != null) {
                pr.setRequiredDate(LocalDate.parse(String.valueOf(item.get("requiredDate")).substring(0, 10)));
            }
            pr.setStatus(STATUS_PENDING);
            pr.setSourceType("MRP");
            pr.setRemark(note);
            pr.setCreatedBy(userId != null ? userId : 1L);
            pr.setCreatedAt(LocalDateTime.now());
            pr.setUpdatedAt(LocalDateTime.now());
            prMapper.insert(pr);
            created.add(pr);
        }

        if (created.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("skipped", skipped);
            return Result.fail(40001, "PR_CREATE_NONE");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("prCount", created.size());
        out.put("prs", created.stream().map(this::toVo).toList());
        if (!skipped.isEmpty()) {
            out.put("skipped", skipped);
            out.put("partial", true);
        }
        return Result.ok(out);
    }

    /** 采购员：采购申请 → 采购订单（物料/数量/交期锁定） */
    @Transactional
    public Result<Map<String, Object>> convertToPo(Long prId, ConvertPrToPoRequest req, Long userId) {
        CrmPurchaseRequest pr = prMapper.selectById(prId);
        if (pr == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "PR_NOT_FOUND");
        }
        if (STATUS_CONVERTED.equals(pr.getStatus())) {
            return Result.fail(40903, "PR_ALREADY_CONVERTED");
        }
        if (req == null || req.getVendorName() == null || req.getVendorName().isBlank()) {
            return Result.fail(40001, "VENDOR_REQUIRED");
        }

        int remaining = pr.getRequiredQty() - (pr.getConvertedQty() != null ? pr.getConvertedQty() : 0);
        if (remaining <= 0) {
            return Result.fail(40903, "PR_NO_REMAINING_QTY");
        }

        int convertQty = req.getQty() != null && req.getQty() > 0 ? req.getQty() : remaining;
        if (convertQty > remaining) {
            convertQty = remaining;
        }

        OutsubVendor vendor = vendorMapper.selectOne(
                new LambdaQueryWrapper<OutsubVendor>()
                        .eq(OutsubVendor::getVendorName, req.getVendorName().trim())
                        .last("LIMIT 1"));
        Long supplierId = vendor != null ? vendor.getId() : 0L;

        BigDecimal unitPrice = req.getUnitPrice() != null ? req.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(convertQty));
        LocalDate deliveryDate = parseDate(req.getDeliveryDate());
        if (deliveryDate == null) {
            deliveryDate = pr.getRequiredDate();
        }

        CrmPurchaseOrder po = new CrmPurchaseOrder();
        po.setPoNo(docNoGenerator.nextNo(RfqService.PO_NO_PREFIX));
        po.setPrId(pr.getId());
        po.setPrNo(pr.getPrNo());
        po.setWorkorderNo(pr.getWorkorderNo());
        po.setMrpRunId(pr.getMrpRunId());
        po.setSupplierId(supplierId);
        po.setSupplierName(req.getVendorName().trim());
        po.setTotalAmount(totalAmount);
        po.setStatus("PENDING_SHIP");
        po.setSourceType("FROM_MRP");
        po.setApprovalStatus("PENDING");
        po.setRemark(req.getNote());
        po.setCreatedBy(userId != null ? userId : 1L);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        poMapper.insert(po);

        CrmPurchaseOrderItem line = new CrmPurchaseOrderItem();
        line.setPoId(po.getId());
        line.setPurchaseOrderId(po.getId());
        line.setMaterialId(pr.getMaterialId());
        line.setMaterialCode(pr.getMaterialCode());
        line.setMaterialName(pr.getMaterialName());
        line.setQuantity(convertQty);
        line.setUnitPrice(unitPrice);
        line.setAmount(totalAmount);
        line.setDeliveryDate(deliveryDate);
        line.setSortNo(1);
        line.setCreatedAt(LocalDateTime.now());
        poItemMapper.insert(line);

        int newConverted = (pr.getConvertedQty() != null ? pr.getConvertedQty() : 0) + convertQty;
        pr.setConvertedQty(newConverted);
        if (newConverted >= pr.getRequiredQty()) {
            pr.setStatus(STATUS_CONVERTED);
        } else {
            pr.setStatus(STATUS_PARTIAL);
        }
        pr.setUpdatedAt(LocalDateTime.now());
        prMapper.updateById(pr);

        Map<String, Object> result = new HashMap<>();
        result.put("poId", po.getId());
        result.put("poNo", po.getPoNo());
        result.put("prNo", pr.getPrNo());
        result.put("workorderNo", pr.getWorkorderNo());
        result.put("convertedQty", convertQty);
        result.put("prStatus", pr.getStatus());
        result.put("truncated", req.getQty() != null && req.getQty() > remaining);
        return Result.ok(result);
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private Map<String, Object> toVo(CrmPurchaseRequest pr) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", pr.getId());
        m.put("prNo", pr.getPrNo());
        m.put("mrpRunId", pr.getMrpRunId());
        m.put("workorderNo", pr.getWorkorderNo());
        m.put("salesOrderNo", pr.getSalesOrderNo());
        m.put("materialId", pr.getMaterialId());
        m.put("materialCode", pr.getMaterialCode());
        m.put("materialName", pr.getMaterialName());
        m.put("requiredQty", pr.getRequiredQty());
        m.put("convertedQty", pr.getConvertedQty());
        m.put("remainingQty", pr.getRequiredQty() - (pr.getConvertedQty() != null ? pr.getConvertedQty() : 0));
        m.put("requiredDate", pr.getRequiredDate());
        m.put("status", pr.getStatus());
        m.put("sourceType", pr.getSourceType());
        m.put("remark", pr.getRemark());
        m.put("createdAt", pr.getCreatedAt());
        return m;
    }
}
