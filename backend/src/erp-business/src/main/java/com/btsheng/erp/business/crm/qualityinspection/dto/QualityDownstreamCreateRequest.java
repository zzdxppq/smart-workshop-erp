package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "品质下游单据创建请求")
public class QualityDownstreamCreateRequest {

    private Long inspectionId;
    private String inspectionNo;
    private Integer qty;
    private String remark;
}
