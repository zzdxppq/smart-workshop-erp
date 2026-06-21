package com.btsheng.erp.business.crm.qualityinspection.service;

import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.qualityinspection.dto.PendingInspectionRequest;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionItem;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionItemMapper;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityInspectionMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class QualityInspectionAutoPushService {

    private final CrmQualityInspectionMapper inspectionMapper;
    private final CrmQualityInspectionItemMapper itemMapper;
    private final CrmMaterialMapper materialMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public QualityInspectionAutoPushService(CrmQualityInspectionMapper inspectionMapper,
                                            CrmQualityInspectionItemMapper itemMapper,
                                            CrmMaterialMapper materialMapper,
                                            DocNoGenerator docNoGenerator) {
        this.inspectionMapper = inspectionMapper;
        this.itemMapper = itemMapper;
        this.materialMapper = materialMapper;
        this.docNoGenerator = docNoGenerator;
    }

    @Transactional
    public Result<CrmQualityInspection> createPending(PendingInspectionRequest req, Long operatorUserId) {
        if (req == null || req.getInspectType() == null || req.getInspectType().isBlank()) {
            return Result.fail(40001, "INSPECT_TYPE_REQUIRED");
        }
        if (req.getSourceRef() != null && !req.getSourceRef().isBlank()) {
            CrmQualityInspection existing = inspectionMapper.selectBySourceRef(req.getSourceRef().trim());
            if (existing != null) {
                return Result.ok(existing);
            }
        }

        CrmQualityInspection insp = new CrmQualityInspection();
        insp.setInspectionNo(docNoGenerator.nextQualityInspectionNo());
        insp.setInspectType(req.getInspectType().trim());
        insp.setMaterialCode(req.getMaterialCode());
        insp.setMaterialName(req.getMaterialName());
        insp.setMaterialId(req.getMaterialId());
        if (insp.getMaterialId() == null && req.getMaterialCode() != null) {
            CrmMaterial m = materialMapper.selectByMaterialCode(req.getMaterialCode().trim());
            if (m != null) {
                insp.setMaterialId(m.getId());
                insp.setMaterialName(m.getMaterialName());
            }
        }
        if (insp.getMaterialCode() == null || insp.getMaterialCode().isBlank()) {
            insp.setMaterialCode(req.getWorkOrderNo() != null ? req.getWorkOrderNo() : "PENDING");
            insp.setMaterialName(insp.getMaterialCode());
        }
        insp.setWorkOrderNo(req.getWorkOrderNo());
        insp.setWorkOrderId(req.getWorkOrderId());
        insp.setProcessName(req.getProcessName());
        insp.setBatchNo(req.getBatchNo());
        int qty = req.getQty() == null || req.getQty() <= 0 ? 1 : req.getQty();
        insp.setLotSize(qty);
        insp.setSampleSize(0);
        insp.setInspectQty(0);
        insp.setPassedQty(0);
        insp.setFailedQty(0);
        insp.setDefectRate(BigDecimal.ZERO);
        insp.setResult(QualityInspectionService.STATUS_DRAFT);
        insp.setRemark(req.getRemark());
        insp.setSourceRef(req.getSourceRef());
        insp.setCreatedBy(operatorUserId == null ? 1L : operatorUserId);
        insp.setCreatedAt(LocalDateTime.now());
        insp.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.insert(insp);

        CrmQualityInspectionItem placeholder = new CrmQualityInspectionItem();
        placeholder.setInspectionId(insp.getId());
        placeholder.setItemName("待检验项");
        placeholder.setStandard("请录入实测值");
        placeholder.setSeverity(QualityInspectionService.SEVERITY_INFO);
        placeholder.setPassed(0);
        placeholder.setCreatedAt(LocalDateTime.now());
        itemMapper.insert(placeholder);
        insp.setInspectQty(1);
        inspectionMapper.updateById(insp);

        return Result.ok(insp);
    }
}
