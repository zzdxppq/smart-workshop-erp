package com.btsheng.erp.business.crm.batch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V1.3.8 · Story 3.1 · 批次创建请求 DTO
 *
 * <p>Story 3.1 AC-3.1.1：按物料粒度批次创建。
 * 原 V1.3.7 1.34/1.35 是 PO 粒度（一条记录 = 一次到货汇总）。
 * V1.3.8 按传入的 items 列表，每个物料生成一个 batch。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "批次创建请求（按物料粒度）")
public class BatchCreateRequest {

    @Schema(description = "PO ID", example = "1001", required = true)
    @NotNull
    private Long poId;

    @Schema(description = "到货时间", example = "2026-06-13T10:30:00", required = true)
    @NotNull
    private LocalDateTime arrivedAt;

    @Schema(description = "到货物料列表（每个物料一行）", required = true)
    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @Schema(description = "物料 ID", example = "5001", required = true)
        @NotNull
        private Long materialId;

        @Schema(description = "本批到货数量", example = "60", required = true)
        @NotNull
        @Min(1)
        private Integer quantity;

        @Schema(description = "PO 行项 ID（可选，未传则按 (poId, materialId) 查）")
        private Long poItemId;
    }
}