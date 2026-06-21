package com.btsheng.erp.business.crm.bom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "BOM 树保存（Web BomTree 组件）")
public class BomSaveTreeRequest {
    private Long bomId;
    private String productCode;
    private List<Map<String, Object>> lines;
}
