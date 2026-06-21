package com.btsheng.erp.business.crm.purchaseinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.35 · 添加检验项请求
 */
@Data
@Schema(description = "添加检验项请求（Story 1.35 FR-8-4）")
public class AddItemRequest {

    @Schema(description = "检验项目（外观/尺寸/材质/性能等）", example = "外观", required = true)
    private String checkItem;
    @Schema(description = "判定标准")
    private String standard;
    @Schema(description = "抽样数", example = "20", required = true)
    private Integer sampleQty;
    @Schema(description = "合格数", example = "20")
    private Integer passQty;
    @Schema(description = "不合格数", example = "0")
    private Integer failQty;
    @Schema(description = "是否关键项 1 票否决", example = "0")
    private Integer isCritical = 0;
    @Schema(description = "备注")
    private String remark;
}
