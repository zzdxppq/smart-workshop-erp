package com.btsheng.erp.platform.print.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 补打请求 DTO（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.4）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "补打请求")
public class ReprintRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标模式 · ZPL_DIRECT / PDF_BROWSER / SAME（默认 SAME）", example = "SAME")
    private String targetMode = "SAME";

    @Schema(description = "targetMode=ZPL_DIRECT 时必填", example = "5")
    private Long printerId;

    @Size(max = 200)
    @Schema(description = "备注")
    private String remark;
}
