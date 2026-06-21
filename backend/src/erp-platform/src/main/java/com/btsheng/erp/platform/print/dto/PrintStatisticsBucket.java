package com.btsheng.erp.platform.print.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 打印统计 Bucket（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.4）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "打印统计 bucket · groupBy 聚合结果")
public class PrintStatisticsBucket implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分组键（年-月 / 操作人ID / codeType / mode）", example = "2026-06")
    private String key;

    @Schema(description = "操作人 user_id（groupBy=operator_id 时）", example = "1001")
    private Long operatorId;

    @Schema(description = "总记录数", example = "100")
    private Long totalCount;

    @Schema(description = "成功记录数", example = "80")
    private Long successCount;

    @Schema(description = "失败记录数", example = "20")
    private Long failedCount;

    @Schema(description = "总份数（含 copies 累加）", example = "120")
    private Long totalCopies;
}
