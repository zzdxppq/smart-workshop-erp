package com.btsheng.erp.production.process.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * V1.3.7 · Story 1.10 · AC-3.4.1 创建工艺请求
 */
@Data
@Schema(description = "工艺创建请求（POST /processes）")
public class ProcessCreateRequest {

    @Schema(description = "工艺名称", example = "航空精密连接器外壳加工工艺", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processName;

    @Schema(description = "工艺类型：STANDARD/FA/PROTOTYPE", example = "FA")
    private String processType = "STANDARD";

    @Schema(description = "关联图纸 ID（可空 · 工艺复用 · P2 修补）", example = "1")
    private Long drawingId;

    @Schema(description = "是否可复用（P2 修补）", example = "true")
    private Boolean isReusable = true;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "备注")
    private String comment;

    @Schema(description = "工序列表（至少 1 道）")
    private List<StepInput> steps;

    @Data
    @Schema(description = "工序输入")
    public static class StepInput {
        @Schema(description = "工序序号（P1 修补 1：严格排序）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer stepNo;
        @Schema(description = "工序名称", example = "车削外圆", requiredMode = Schema.RequiredMode.REQUIRED)
        private String stepName;
        @Schema(description = "5 段：原材料/粗加工/精加工/表面处理/检验", example = "原材料")
        private String segment = "原材料";
        @Schema(description = "机器类型（P1 修补 2：必须匹配）", example = "CNC_LATHE")
        private String machineType;
        @Schema(description = "具体机器 ID", example = "101")
        private Long machineId;
        @Schema(description = "工时（P1 修补 3：非负）", example = "1.5")
        private BigDecimal estimatedHours = BigDecimal.ZERO;
        @Schema(description = "单价", example = "120.50")
        private BigDecimal unitCost = BigDecimal.ZERO;
        @Schema(description = "是否为质检工序")
        private Boolean isQualityCheck = false;
    }
}
