package com.btsheng.erp.production.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "生管分配工序归属请求（不含 vendorId）")
public class CreateAllocationRequest {
    private Long workorderId;
    private Integer processSeq;
    /** INHOUSE / OUTSOURCE */
    private String decision;
    private Integer qty;
}
