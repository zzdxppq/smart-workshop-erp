package com.btsheng.erp.business.crm.qualityinspection.service;

import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityDownstream;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import com.btsheng.erp.business.crm.qualityinspection.mapper.CrmQualityDownstreamMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class QualityInspectionDispositionService {

    public static final String TYPE_RETURN = "RETURN";
    public static final String TYPE_REWORK = "REWORK";
    public static final String TYPE_SCRAP = "SCRAP";

    private final CrmQualityDownstreamMapper downstreamMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public QualityInspectionDispositionService(CrmQualityDownstreamMapper downstreamMapper,
                                             DocNoGenerator docNoGenerator) {
        this.downstreamMapper = downstreamMapper;
        this.docNoGenerator = docNoGenerator;
    }

    @Transactional
    public Result<Map<String, Object>> createReturnOrder(CrmQualityInspection insp, int qty, Long operatorUserId) {
        return createDownstream(insp, TYPE_RETURN, docNoGenerator.nextQualityReturnNo(), qty, operatorUserId);
    }

    @Transactional
    public Result<Map<String, Object>> createReworkOrder(CrmQualityInspection insp, int qty, Long operatorUserId) {
        return createDownstream(insp, TYPE_REWORK, docNoGenerator.nextQualityReworkOrderNo(), qty, operatorUserId);
    }

    @Transactional
    public Result<Map<String, Object>> createScrapRecord(CrmQualityInspection insp, int qty, Long operatorUserId) {
        return createDownstream(insp, TYPE_SCRAP, docNoGenerator.nextQualityScrapNo(), qty, operatorUserId);
    }

    private Result<Map<String, Object>> createDownstream(CrmQualityInspection insp, String type,
                                                          String orderNo, int qty, Long operatorUserId) {
        CrmQualityDownstream row = new CrmQualityDownstream();
        row.setInspectionId(insp.getId());
        row.setDownstreamType(type);
        row.setOrderNo(orderNo);
        row.setQty(qty);
        row.setStatus("CREATED");
        row.setRemark("由检验单 " + insp.getInspectionNo() + " 自动触发");
        row.setCreatedBy(operatorUserId);
        row.setCreatedAt(LocalDateTime.now());
        downstreamMapper.insert(row);

        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", orderNo);
        data.put("downstreamType", type);
        data.put("inspectionId", insp.getId());
        data.put("qty", qty);
        return Result.ok(data);
    }
}
