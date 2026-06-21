package com.btsheng.erp.business.crm.inventoryalert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "预警解决请求（AC-4.4.2 · 解决后归档）")
public class AlertResolveRequest {
    @Schema(description = "解决备注", required = true)
    private String resolutionNote;
}
