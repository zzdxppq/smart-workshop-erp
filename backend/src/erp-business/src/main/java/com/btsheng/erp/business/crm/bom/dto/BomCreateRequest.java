package com.btsheng.erp.business.crm.bom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * V1.3.7 · Story 1.9 · AC-3.3.1 创建 BOM 请求
 */
@Data
@Schema(description = "BOM 创建请求（POST /boms）")
public class BomCreateRequest {

    @Schema(description = "源图纸 ID（Story 1.8 工程转化）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long drawingId;

    @Schema(description = "图号（冗余）", example = "DWG-20260620-0001")
    private String drawingNo;

    @Schema(description = "预生成 BOM 单号（工程转化传入，可选）", example = "BOM-20260620-0001")
    private String bomNo;

    @Schema(description = "BOM 类型：STANDARD/FA/PROTOTYPE", example = "STANDARD")
    private String bomType = "STANDARD";

    @Schema(description = "BOM 版本（多版本 · P2 修补）", example = "v1")
    private String bomVersion = "v1";

    @Schema(description = "目标数量（正整数 · P1 修补 3）", example = "100", minimum = "1")
    private Integer targetQty = 1;

    @Schema(description = "主物料编码", example = "WL-1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String materialCode;

    @Schema(description = "父 BOM ID（多级树 · 5 级递归上限）", example = "null")
    private Long parentBomId;

    @Schema(description = "是否允许物料替代（P2 修补）", example = "false")
    private Boolean isSubstitutable = false;

    @Schema(description = "备注")
    private String comment;

    @Schema(description = "BOM 子项列表")
    private List<BomItemInput> items;

    @Data
    @Schema(description = "BOM 子项输入")
    public static class BomItemInput {
        @Schema(description = "父物料项 ID（多级树 · 5 级递归）")
        private Long parentItemId;
        @Schema(description = "物料层级 0-4（P1 修补 1：5 级上限）", example = "0", minimum = "0", maximum = "4")
        private Integer itemLevel = 0;
        @Schema(description = "同级排序", example = "1")
        private Integer itemNo = 1;
        @Schema(description = "物料编码", example = "WL-1001", requiredMode = Schema.RequiredMode.REQUIRED)
        private String materialCode;
        @Schema(description = "物料名称", example = "航空精密连接器外壳毛坯", requiredMode = Schema.RequiredMode.REQUIRED)
        private String materialName;
        @Schema(description = "规格")
        private String spec;
        @Schema(description = "数量（正数 · P1 修补 3）", example = "1")
        private BigDecimal qty = BigDecimal.ONE;
        @Schema(description = "单位", example = "PCS")
        private String unit = "PCS";
        @Schema(description = "单价", example = "120.50")
        private BigDecimal unitCost = BigDecimal.ZERO;
        @Schema(description = "5 段：原材料/粗加工/精加工/表面处理/检验", example = "原材料")
        private String segment = "原材料";
        @Schema(description = "替代物料编码（多个逗号分隔 · P2 修补）")
        private String substituteMaterials;
    }
}
